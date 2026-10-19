package com.aaa.bbb.ccc.tarot

import android.app.Activity
import android.content.Intent
import android.os.SystemClock
import android.view.View
import android.view.inspector.WindowInspector
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.aaa.bbb.ccc.R
import com.aaa.bbb.ccc.core.base.BaseActivity
import com.aaa.bbb.ccc.core.extensions.hideNavigation
import com.aaa.bbb.ccc.ui.home.HomeActivity
import com.google.gson.Gson
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.Closeable

/** Actual lifecycle/window checks. No profile edits or new readings are persisted. */
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 29)
@Suppress("DEPRECATION")
class ImmersiveNavigationTest {
    /** ActivityScenario.onActivity/startActivitySync wait for global idle, which
     * animated/stroked titles do not guarantee. Await RESUMED directly instead. */
    internal class LiveActivity<T : Activity>(
        private val type: Class<T>, private val launchType: Class<out T> = type
    ) : Closeable {
        private val instrumentation = InstrumentationRegistry.getInstrumentation()
        var beforeAction: ((T) -> Unit)? = null

        init {
            instrumentation.runOnMainSync {
                val intent = Intent(instrumentation.targetContext, launchType)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                instrumentation.targetContext.startActivity(intent)
            }
            waitForResumed()
        }

        private fun current(): T? = ActivityLifecycleMonitorRegistry.getInstance()
            .getActivitiesInStage(Stage.RESUMED).firstOrNull { type.isInstance(it) }?.let(type::cast)

        private fun waitForResumed(previous: T? = null) {
            val deadline = SystemClock.uptimeMillis() + 10_000
            while (SystemClock.uptimeMillis() < deadline) {
                var ready = false
                instrumentation.runOnMainSync { ready = current()?.let { it !== previous } == true }
                if (ready) return
                SystemClock.sleep(40)
            }
            fail("${type.simpleName} did not reach RESUMED")
        }

        fun onActivity(action: (T) -> Unit) {
            waitForResumed()
            instrumentation.runOnMainSync {
                val activity = requireNotNull(current())
                beforeAction?.invoke(activity)
                action(activity)
            }
        }

        fun recreate() {
            var previous: T? = null
            onActivity { previous = it; it.recreate() }
            waitForResumed(previous)
        }

        override fun close() {
            instrumentation.runOnMainSync {
                val monitor = ActivityLifecycleMonitorRegistry.getInstance()
                Stage.values().flatMap { monitor.getActivitiesInStage(it).toList() }
                    .filter { type.isInstance(it) }.distinct().forEach { it.finish() }
            }
        }
    }

    private fun hidden(view: View): Boolean =
        view.systemUiVisibility and View.SYSTEM_UI_FLAG_HIDE_NAVIGATION != 0 &&
            ViewCompat.getRootWindowInsets(view)?.isVisible(WindowInsetsCompat.Type.navigationBars()) == false

    private fun <T : Activity> await(scenario: LiveActivity<T>, description: String, check: (T) -> Boolean) {
        val deadline = SystemClock.uptimeMillis() + 10_000
        while (SystemClock.uptimeMillis() < deadline) {
            var passed = false
            scenario.onActivity { passed = check(it) }
            if (passed) return
            // Await drawn/lifecycle state, not global Espresso idle (titles may animate).
            SystemClock.sleep(40)
        }
        fail(description)
    }

    private fun homeScenario(): LiveActivity<TarotActivity> {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assumeTrue("Home needs an existing profile; this test never creates or overwrites one",
            TarotPreferences(context).profile != null)
        return LiveActivity(TarotActivity::class.java, HomeActivity::class.java)
    }

    @Test fun profileInheritsBaseAndPreservesFormBackAndRecreationState() {
        assertTrue(BaseActivity::class.java.isAssignableFrom(TarotProfileActivity::class.java))
        LiveActivity(TarotProfileActivity::class.java).use { scenario ->
            await(scenario, "Profile navigation must be hidden") { hidden(it.window.decorView) }
            scenario.onActivity { activity ->
                val root = activity.binding.root
                val grid = activity.binding.zodiacGrid.getChildAt(0) as android.widget.GridLayout
                grid.getChildAt(0).performClick() // Aries, without saving a profile.
                activity.binding.next.performClick()
                assertSame(root, activity.binding.root)
                assertEquals(View.VISIBLE, activity.binding.profileFields.visibility)
                activity.binding.nameField.nameInput.setText("Navigation test")
            }
            scenario.recreate()
            scenario.onActivity { activity ->
                assertEquals("Navigation test", activity.binding.nameField.nameInput.text.toString())
                assertEquals(1, activity.binding.birthdayHost.childCount)
                activity.onBackPressed()
                assertFalse(activity.isFinishing)
                assertEquals(View.VISIBLE, activity.binding.zodiacGrid.visibility)
                assertEquals(1, activity.binding.zodiacGrid.childCount)
                activity.binding.next.performClick()
                assertEquals("Navigation test", activity.binding.nameField.nameInput.text.toString())
                assertEquals(1, activity.binding.birthdayHost.childCount)
            }
            await(scenario, "Profile must remain immersive after Back/recreation") { hidden(it.window.decorView) }
        }
    }

    @Test fun sheetsAndAlertsHideNavigationAndRestoreTheHostAfterDismiss() {
        LiveActivity(TarotProfileActivity::class.java).use { scenario ->
            await(scenario, "Host must be immersive") { hidden(it.window.decorView) }
            lateinit var sheet: com.google.android.material.bottomsheet.BottomSheetDialog
            scenario.onActivity { activity ->
                sheet = TarotUi(activity, TarotPreferences(activity).dark).sheet("Navigation test") { _, _ -> }
            }
            await(scenario, "Bottom sheet must hide navigation") { hidden(requireNotNull(sheet.window).decorView) }
            scenario.onActivity { sheet.dismiss() }
            await(scenario, "Host must hide navigation again after the sheet") { hidden(it.window.decorView) }

            listOf(false, true).forEach { dark ->
                lateinit var dialog: AlertDialog
                scenario.onActivity { activity ->
                    val themed = ContextThemeWrapper(activity, if (dark) R.style.TarotThemeDark else R.style.TarotThemeLight)
                    dialog = AlertDialog.Builder(themed).setMessage("Navigation test").setPositiveButton("Close", null).show()
                    dialog.hideNavigation(!dark)
                }
                await(scenario, "Alert must hide navigation") { hidden(requireNotNull(dialog.window).decorView) }
                scenario.onActivity {
                    val lightStatus = requireNotNull(dialog.window).decorView.systemUiVisibility and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR != 0
                    assertEquals(!dark, lightStatus)
                    dialog.dismiss()
                }
                await(scenario, "Host must recover immersive mode") { hidden(it.window.decorView) }
            }
        }
    }

    @Test fun allTarotDestinationsUseSeparateActivitiesAndKeepOnlyTheAppsOwnTabs() {
        assertTrue(BaseActivity::class.java.isAssignableFrom(HomeActivity::class.java))
        homeScenario().use { scenario ->
            await(scenario, "Deck must load") { ViewModelProvider(it)[TarotViewModel::class.java].cards.size == 78 }
            var root: View? = null
            scenario.onActivity { root = it.binding.root }
            val screens = listOf("home" to R.string.tarot_app_title, "spreads" to R.string.tarot_spreads,
                "cards" to R.string.tarot_cards, "history" to R.string.tarot_history,
                "settings" to R.string.tarot_settings, "horoscope" to R.string.tarot_horoscope)
            screens.forEach { (screen, title) ->
                scenario.onActivity { ViewModelProvider(it)[TarotViewModel::class.java].screen = screen }
                await(scenario, "$screen must render without system navigation") {
                    it.binding.actionBar.tvCenter.text == it.getString(title) && hidden(it.window.decorView)
                }
                scenario.onActivity {
                    if (screen == "home") assertSame(root, it.binding.root)
                    else assertNotSame(root, it.binding.root)
                    assertEquals(1, it.binding.screenHost.childCount)
                    val hasTabs = screen in listOf("home", "spreads", "cards", "history")
                    assertEquals(if (hasTabs) View.VISIBLE else View.GONE, it.binding.bottomTabs.visibility)
                    assertEquals(if (hasTabs) 4 else 0, it.binding.bottomTabs.childCount)
                }
            }
            scenario.onActivity { activity ->
                val vm = ViewModelProvider(activity)[TarotViewModel::class.java]
                vm.detailSlug = vm.cards.first().slug
                vm.screen = "detail"
            }
            await(scenario, "Detail must render") {
                it is TarotCardDetailActivity && ViewModelProvider(it)[TarotViewModel::class.java].cards.isNotEmpty() &&
                    it.binding.actionBar.tvCenter.text == ViewModelProvider(it)[TarotViewModel::class.java].cards.first().title && hidden(it.window.decorView)
            }
            scenario.onActivity { ViewModelProvider(it)[TarotViewModel::class.java].begin(Spread.CELTIC) }
            await(scenario, "Selection must render") { it.binding.screenHost.findViewById<View>(R.id.selectDescription) != null && hidden(it.window.decorView) }
            scenario.onActivity { activity ->
                val vm = ViewModelProvider(activity)[TarotViewModel::class.java]
                val cards = vm.cards.take(10).mapIndexed { i, card -> TarotRules.interpret(card, Spread.CELTIC, i, false) }
                val reading = ReadingSnapshot("navigation-test-in-memory", Spread.CELTIC.name, System.currentTimeMillis(), cards)
                // open() only parses this snapshot; unlike complete(), it never inserts into Room.
                vm.open(ReadingEntity(reading.id, reading.spread, reading.createdAt, Gson().toJson(reading)))
            }
            await(scenario, "Celtic result must render") { it.binding.screenHost.findViewById<View>(R.id.positionTitle) != null && hidden(it.window.decorView) }
            scenario.onActivity { assertNotSame(root, it.binding.root) }
        }
    }

    @Test fun legacySystemBackStillUsesTheSelectionDiscardDialogInsteadOfFinishingHome() {
        homeScenario().use { scenario ->
            await(scenario, "Deck must load") { ViewModelProvider(it)[TarotViewModel::class.java].cards.size == 78 }
            scenario.onActivity { ViewModelProvider(it)[TarotViewModel::class.java].begin(Spread.CELTIC) }
            await(scenario, "Selection must render") { it.binding.screenHost.findViewById<View>(R.id.selectDescription) != null }
            scenario.onActivity {
                val vm = ViewModelProvider(it)[TarotViewModel::class.java]
                vm.select(vm.deck.first().slug)
                it.onBackPressed()
            }
            await(scenario, "Discard dialog must be immersive") { activity ->
                WindowInspector.getGlobalWindowViews().any { decor ->
                    decor.findViewById<TextView>(android.R.id.message)?.text == activity.getString(R.string.tarot_discard_message) && hidden(decor)
                }
            }
            scenario.onActivity { activity ->
                val decor = WindowInspector.getGlobalWindowViews().first {
                    it.findViewById<TextView>(android.R.id.message)?.text == activity.getString(R.string.tarot_discard_message)
                }
                decor.findViewById<View>(android.R.id.button2).performClick() // Keep selecting.
                assertFalse(activity.isFinishing)
                val vm = ViewModelProvider(activity)[TarotViewModel::class.java]
                assertEquals("select", vm.screen)
                assertEquals(1, vm.selected.size)
            }
            await(scenario, "Selection must regain immersive mode") { hidden(it.window.decorView) }
        }
    }
}

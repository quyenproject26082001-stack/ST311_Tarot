package com.tarot.card.meaning.tarot

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.SystemClock
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.tarot.card.meaning.R
import com.tarot.card.meaning.ui.home.HomeActivity
import com.google.gson.Gson
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TarotActivityNavigationTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val preferences = instrumentation.targetContext.getSharedPreferences("tarot_preferences", Context.MODE_PRIVATE)
    private var originalProfile: String? = null
    private var originalDark: Boolean? = null

    @Before fun prepareProfile() {
        originalProfile = preferences.getString("profile", null)
        originalDark = if (preferences.contains("dark")) preferences.getBoolean("dark", false) else null
        assertEquals(null, TarotPreferences(instrumentation.targetContext)
            .saveProfile(Profile("Navigation Test", "1994-11-14", "Scorpio")))
        preferences.edit().putBoolean("dark", false).commit()
    }

    @After fun restoreProfile() {
        instrumentation.runOnMainSync {
            val monitor = ActivityLifecycleMonitorRegistry.getInstance()
            Stage.values().flatMap { monitor.getActivitiesInStage(it).toList() }
                .filterIsInstance<TarotActivity>().distinct().forEach { it.finish() }
        }
        val editor = preferences.edit()
        originalProfile?.let { editor.putString("profile", it) } ?: editor.remove("profile")
        originalDark?.let { editor.putBoolean("dark", it) } ?: editor.remove("dark")
        editor.commit()
    }

    @Test fun themeChangesKeepTheSameActivitiesAndUpdateBothThemes() {
        ImmersiveNavigationTest.LiveActivity(TarotActivity::class.java, HomeActivity::class.java).use { scenario ->
            val home = awaitActivity(HomeActivity::class.java) { model(it).cards.isNotEmpty() }
            scenario.onActivity { it.findViewById<android.view.View>(R.id.btnActionBarRight).performClick() }
            val settings = awaitActivity(TarotSettingsActivity::class.java) {
                it.findViewById<android.view.View>(R.id.darkTheme) != null
            }
            instrumentation.runOnMainSync {
                val originalModel = model(settings)
                val originalWindow = settings.window
                val originalCards = originalModel.cards
                val originalProgress = settings.findViewById<android.widget.SeekBar>(R.id.reverseChance).progress
                val originalGuidance = settings.findViewById<androidx.appcompat.widget.SwitchCompat>(R.id.guidanceToggle).isChecked
                for (dark in listOf(true, false, true, false, true)) {
                    val oldRoot = settings.binding.root
                    val choice = if (dark) R.id.darkTheme else R.id.lightTheme
                    settings.findViewById<android.view.View>(choice).performClick()
                    assertSame(originalModel, model(settings))
                    assertSame(originalWindow, settings.window)
                    assertSame(originalCards, model(settings).cards)
                    assertNotSame(oldRoot, settings.binding.root)
                    assertEquals(dark, model(settings).preferences.dark)
                    assertEquals(Color.parseColor(if (dark) "#172829" else "#FFFFFF"),
                        (settings.binding.root.background as ColorDrawable).color)
                    assertEquals(Color.parseColor(if (dark) "#EFF5F3" else "#172A2A"),
                        settings.binding.actionBar.tvCenter.currentTextColor)
                    assertTrue(settings.findViewById<android.view.View>(choice).isSelected)
                    assertEquals(originalProgress, settings.findViewById<android.widget.SeekBar>(R.id.reverseChance).progress)
                    assertEquals(originalGuidance,
                        settings.findViewById<androidx.appcompat.widget.SwitchCompat>(R.id.guidanceToggle).isChecked)
                }
            }
            val themed = awaitActivity(TarotSettingsActivity::class.java) { model(it).preferences.dark }
            assertSame(settings, themed)
            instrumentation.runOnMainSync { themed.onBackPressedDispatcher.onBackPressed() }
            val returnedHome = awaitActivity(HomeActivity::class.java) {
                (it.binding.root.background as ColorDrawable).color == Color.parseColor("#172829")
            }
            assertSame(home, returnedHome)
            assertTrue(TarotPreferences(instrumentation.targetContext).dark)
        }
    }

    @Test fun selectionDeckLaysOutCardsInScrollAndClassic() {
        ActivityScenario.launch(HomeActivity::class.java).use { scenario ->
            awaitActivity(HomeActivity::class.java) { model(it).cards.isNotEmpty() }
            scenario.onActivity { model(it).begin(Spread.DAILY) }
            val selection = awaitActivity(TarotSelectionActivity::class.java) { activity ->
                val deck = activity.findViewById<RecyclerView>(R.id.deck)
                deck != null && deck.adapter?.itemCount == 78 && deck.childCount > 0 &&
                    (deck.layoutManager as? GridLayoutManager)?.spanCount == 6
            }
            var selectedSlug = ""
            instrumentation.runOnMainSync {
                val deck = selection.findViewById<RecyclerView>(R.id.deck)
                val position = deck.getChildAdapterPosition(deck.getChildAt(0))
                selectedSlug = model(selection).deck[position].slug
                deck.getChildAt(0).performClick()
                assertEquals(selectedSlug, model(selection).selected.single().slug)
            }
            for (classic in listOf(true, false)) {
                instrumentation.runOnMainSync {
                    selection.findViewById<android.view.View>(
                        if (classic) R.id.classicMode else R.id.scrollMode
                    ).performClick()
                }
                awaitActivity(TarotSelectionActivity::class.java) { activity ->
                    val deck = activity.findViewById<RecyclerView>(R.id.deck)
                    deck != null && deck.adapter?.itemCount == 78 && deck.childCount > 0 &&
                        (deck.layoutManager as? GridLayoutManager)?.spanCount == (if (classic) 13 else 6)
                }
                instrumentation.runOnMainSync {
                    assertEquals(selectedSlug, model(selection).selected.single().slug)
                }
            }
        }
    }

    @Test fun selectedDeckSurvivesActivityRecreationAndResultReplacesSelection() {
        ActivityScenario.launch(HomeActivity::class.java).use { scenario ->
            awaitActivity(HomeActivity::class.java) { model(it).cards.isNotEmpty() }
            scenario.onActivity { model(it).begin(Spread.DAILY) }
            val selection = awaitActivity(TarotSelectionActivity::class.java) { model(it).cards.isNotEmpty() }
            var selectedSlug = ""
            instrumentation.runOnMainSync {
                val vm = model(selection)
                selectedSlug = vm.deck.first().slug
                vm.select(selectedSlug)
                vm.classic = true
                selection.recreate()
            }
            val recreated = awaitActivity(TarotSelectionActivity::class.java) {
                it !== selection && model(it).cards.isNotEmpty()
            }
            instrumentation.runOnMainSync {
                val vm = model(recreated)
                assertEquals(selectedSlug, vm.selected.single().slug)
                assertTrue(vm.classic)
                assertEquals(Spread.DAILY, vm.activeSpread)
                val card = TarotRules.interpret(vm.cards.first { it.slug == selectedSlug }, Spread.DAILY, 0, false)
                val snapshot = ReadingSnapshot("navigation-test", Spread.DAILY.name, System.currentTimeMillis(), listOf(card))
                vm.open(ReadingEntity(snapshot.id, snapshot.spread, snapshot.createdAt, Gson().toJson(snapshot)))
            }
            val result = awaitActivity(TarotResultActivity::class.java) { model(it).cards.isNotEmpty() }
            instrumentation.runOnMainSync {
                assertEquals(selectedSlug, model(result).selected.single().slug)
                assertEquals("navigation-test", model(result).reading?.id)
                result.onBackPressedDispatcher.onBackPressed()
            }
            awaitActivity(HomeActivity::class.java) { model(it).screen == "home" }
        }
    }

    @Test fun changingTabsDoesNotStackOldTabActivities() {
        ActivityScenario.launch(HomeActivity::class.java).use { scenario ->
            awaitActivity(HomeActivity::class.java) { model(it).cards.isNotEmpty() }
            scenario.onActivity { model(it).screen = "cards" }
            val cards = awaitActivity(TarotCardsActivity::class.java) { model(it).cards.isNotEmpty() }
            instrumentation.runOnMainSync { model(cards).screen = "history" }
            val history = awaitActivity(TarotHistoryActivity::class.java) { model(it).cards.isNotEmpty() }
            instrumentation.runOnMainSync { history.onBackPressedDispatcher.onBackPressed() }
            awaitActivity(HomeActivity::class.java) { model(it).screen == "home" }
        }
    }

    private fun model(activity: TarotActivity) = ViewModelProvider(activity)[TarotViewModel::class.java]

    private fun <Target : TarotActivity> awaitActivity(type: Class<Target>, ready: (Target) -> Boolean): Target {
        val deadline = SystemClock.uptimeMillis() + 15_000
        while (SystemClock.uptimeMillis() < deadline) {
            var resumed: Activity? = null
            instrumentation.runOnMainSync {
                resumed = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED)
                    .firstOrNull { type.isInstance(it) && !it.isFinishing && ready(type.cast(it)!!) }
            }
            if (resumed != null) return type.cast(resumed)!!
            SystemClock.sleep(50)
        }
        throw AssertionError("Activity did not become ready: ${type.simpleName}")
    }
}

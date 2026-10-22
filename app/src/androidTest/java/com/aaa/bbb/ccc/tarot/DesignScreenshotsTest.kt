package com.tarot.card.meaning.tarot

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Rect
import android.os.Build
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.RecyclerView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.tarot.card.meaning.R
import com.tarot.card.meaning.ui.home.HomeActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.request.Request
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.gson.Gson
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs
import kotlin.random.Random

/**
 * Native, hardware-composited captures; intentionally never waits for global UI idle.
 * Run only this class on the review emulator. Home needs a valid saved profile,
 * or explicit designAllowEmulatorProfile=true to temporarily replace an invalid
 * profile on ranchu/goldfish hardware only. Its original value is restored in finally.
 * Reading fixtures are always held in memory; no reading DB writes are performed.
 *
 * Artifacts: cache/design_review/run_<time>/{manifest.json,<state>.png,<state>.json}.
 * Optional runner arguments: designBaselineDir (directory of approved PNGs),
 * designPixelTolerance (default 8), designMaxMismatch (default 0.005).
 * designAllZodiacs=true adds all twelve selected signs; default captures Scorpio.
 * Pages are capped at six, with one viewport for most states; Glide waits <=1s.
 * Baselines must use the same device, density, font scale, locale and image cache.
 * Comparison excludes system bars; missing baselines fail, never auto-approve.
 * This is Tarot screen coverage, not legacy Splash/Language/Intro/Permission coverage.
 */
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 29)
class DesignScreenshotsTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private val gson = Gson()
    private val arguments = InstrumentationRegistry.getArguments()
    private val directory by lazy {
        File(context.cacheDir, "design_review/run_${System.currentTimeMillis()}").apply {
            check(mkdirs()) { "Cannot create $this" }
        }
    }
    private val records = JSONArray()
    private val failures = mutableListOf<String>()
    private val manifest = JSONObject().put("schemaVersion", 1)
        .put("test", "DesignScreenshotsTest.captureAllTarotDesignScreens")
        .put("capture", "UiAutomation.takeScreenshot; native display pixels")
        .put("fixtureTimestamp", FIXTURE_TIME)
        .put("deckSeed", 311)
        .put("fixturePersistence", "readings in memory; optional emulator-only launch profile preference restored in finally")
        .put("hardware", Build.HARDWARE)
        .put("allowEmulatorProfile", arguments.getString("designAllowEmulatorProfile").equals("true", ignoreCase = true))
        .put("maxPagesPerState", MAX_PAGES)
        .put("glideWaitMs", GLIDE_WAIT_MS)
        .put("constraints", JSONArray(listOf(
            "Home requires a valid profile; an invalid launch profile may be temporarily replaced only with explicit emulator opt-in",
            "The real dark preference and any opted-in launch profile override are restored exactly in finally",
            "Remote art is not validated; pending requests are labeled pending and frozen as placeholders",
            "Locale, current horoscope week and system bars remain device-dependent",
            "No reading completion, favorite, profile Submit, ad request, or DB deletion is invoked",
            "Reflection is confined to app-owned ViewModel/preferences/render/sheet fields"
        )))

    @Test fun captureAllTarotDesignScreens() {
        val realPreferences = context.getSharedPreferences("tarot_preferences", Context.MODE_PRIVATE)
        val before = realPreferences.all.toMap()
        var missingProfile = false
        var profileOverridden = false
        var darkOverridden = false
        try {
            val allowProfile = arguments.getString("designAllowEmulatorProfile").equals("true", ignoreCase = true)
            if (allowProfile) {
                val hardware = Build.HARDWARE.lowercase(Locale.US)
                check(hardware.contains("ranchu") || hardware.contains("goldfish")) {
                    "designAllowEmulatorProfile is prohibited on physical hardware: ${Build.HARDWARE}"
                }
                if (TarotPreferences(context).profile == null) {
                    profileOverridden = true
                    check(realPreferences.edit().putString("profile",
                        gson.toJson(Profile("Alex Morgan", "1994-11-14", "Scorpio"))).commit())
                }
            }
            manifest.put("launchProfileTemporarilyOverridden", profileOverridden)
            darkOverridden = true
            check(realPreferences.edit().putBoolean("dark", false).commit())
            captureProfile()
            if (TarotPreferences(context).profile == null) {
                missingProfile = true
                records.put(JSONObject().put("id", "home_and_remaining_tarot_screens")
                    .put("status", "skipped")
                    .put("reason", "Home needs a valid profile; emulator-only override requires designAllowEmulatorProfile=true"))
            } else {
                captureHomeDestinations()
            }
        } catch (error: Exception) {
            recordFailure("capture_run", error)
        } catch (error: AssertionError) {
            recordFailure("capture_run", error)
        } finally {
            try {
                if (profileOverridden) restorePreference(realPreferences, "profile", before)
            } finally {
                if (darkOverridden) restorePreference(realPreferences, "dark", before)
            }
            val restored = realPreferences.all == before
            manifest.put("realPreferencesRestored", restored)
            if (!restored) failures += "Real Tarot preferences differ after cleanup"
            manifest.put("status", if (failures.isNotEmpty()) "failed" else if (missingProfile) "partial" else "complete")
            writeManifest()
            instrumentation.sendStatus(0, android.os.Bundle().apply {
                putString("design_review", directory.absolutePath)
                putString("design_review_status", manifest.getString("status"))
            })
        }
        assertTrue("${failures.joinToString("\n")}\nArtifacts: $directory", failures.isEmpty())
        assertFalse("Partial captures: Home needs a legitimate existing profile. See $directory/manifest.json", missingProfile)
    }

    private fun captureProfile() {
        ImmersiveNavigationTest.LiveActivity(TarotProfileActivity::class.java).use { live ->
            capturePages(live, "profile_step_1_unselected", maxPages = MAX_PAGES) { it.binding.profileScroll }
            val allZodiacs = arguments.getString("designAllZodiacs").equals("true", ignoreCase = true)
            zodiacSigns.forEachIndexed { index, sign ->
                if (!allZodiacs && sign.name != "Scorpio") return@forEachIndexed
                attempt("profile_step_1_${sign.name.lowercase(Locale.US)}") {
                    live.onActivity { activity ->
                        (activity.binding.zodiacGrid.getChildAt(0) as GridLayout).getChildAt(index).performClick()
                    }
                    await(live, "Selected ${sign.name}") {
                        (it.binding.zodiacGrid.getChildAt(0) as GridLayout).getChildAt(index).isSelected
                    }
                    live.onActivity { activity ->
                        val tile = (activity.binding.zodiacGrid.getChildAt(0) as GridLayout).getChildAt(index)
                        tile.requestRectangleOnScreen(Rect(0, 0, tile.width, tile.height), true)
                    }
                    capture(live, "profile_step_1_${sign.name.lowercase(Locale.US)}",
                        JSONObject().put("step", 1).put("selectedSign", sign.name))
                }
            }
            attempt("profile_step_2") {
                live.onActivity { activity ->
                    (activity.binding.zodiacGrid.getChildAt(0) as GridLayout).getChildAt(7).performClick() // Scorpio
                    activity.binding.next.performClick() // Only advances; never click Next on step 2.
                    activity.binding.nameField.nameInput.setText("Alex Morgan")
                    activity.binding.root.clearFocus()
                }
                await(live, "Profile form") { it.binding.profileFields.visibility == View.VISIBLE }
                capturePages(live, "profile_step_2", maxPages = MAX_PAGES) { it.binding.profileScroll }
                live.onActivity { activity ->
                    assertEquals(View.GONE, activity.binding.actionBar.btnActionBarLeft.visibility)
                    assertEquals(View.VISIBLE, activity.binding.previous.visibility)
                    assertEquals(1, activity.binding.birthdayHost.childCount)
                }
            }
        }
    }

    private fun captureHomeDestinations() {
        ImmersiveNavigationTest.LiveActivity(TarotActivity::class.java, HomeActivity::class.java).use { live ->
            lateinit var model: TarotViewModel
            var fixtureRows = emptyList<ReadingEntity>()
            val prepared = mutableSetOf<TarotViewModel>()
            val memory = MemoryPreferences(mapOf(
                    "profile" to gson.toJson(Profile("Alex Morgan", "1994-11-14", "Scorpio")),
                    "dark" to false, "show_guidance" to true, "reverse_chance" to 0
            ))
            val isolated = object : ContextWrapper(context) {
                override fun getApplicationContext(): Context = this
                override fun getSharedPreferences(name: String, mode: Int): SharedPreferences = memory
            }
            live.beforeAction = { activity ->
                model = vm(activity)
                if (model.cards.isNotEmpty() && prepared.add(model)) {
                    model.viewModelScope.coroutineContext.cancelChildren()
                    setField(model, "preferences", TarotPreferences(isolated))
                    setField(model, "history", fixtureRows)
                    render(activity)
                }
            }
            await(live, "78 cards from local JSON") { vm(it).cards.size == 78 && vm(it).error == null }
            val databaseBefore = runBlocking { model.repository.dao.observe().first() }
            try {
                destination(live, "home_empty", "home")
                live.onActivity { fixtureRows = fixtureHistory(model); setField(model, "history", fixtureRows) }
                destination(live, "home_recent", "home")
                live.onActivity { fixtureRows = emptyList(); setField(model, "history", fixtureRows) }
                destination(live, "spreads", "spreads")
                destination(live, "history_empty", "history")
                live.onActivity { fixtureRows = fixtureHistory(model); setField(model, "history", fixtureRows) }
                destination(live, "history_data", "history")
                destination(live, "cards_list", "cards") { it.cardGrid = false }
                destination(live, "cards_grid", "cards") { it.cardGrid = true }
                destination(live, "cards_detail", "detail") { it.detailSlug = it.cards.first().slug }
                destination(live, "horoscope_unavailable", "horoscope")

                listOf(false, true).forEach { classic ->
                    val id = "selection_${if (classic) "classic" else "scroll"}"
                    attempt(id) {
                        live.onActivity { activity ->
                            val saved = field(model, "saved") as SavedStateHandle
                            model.activeSpread = Spread.QUICK
                            model.classic = classic
                            val deck = TarotRules.deck(model.cards, 0, Random(311))
                            saved["deck"] = gson.toJson(deck)
                            saved["selected"] = gson.toJson(deck.take(1))
                            saved["reading"] = null as String?
                            model.screen = "select"
                            render(activity)
                        }
                        await(live, id) {
                            val deck = it.binding.screenHost.findViewById<RecyclerView>(R.id.deck)
                            deck != null && deck.childCount > 0 && model.selected.size == 1
                        }
                        capture(live, id, JSONObject().put("mode", if (classic) "Classic" else "Scroll")
                            .put("selected", JSONArray(model.selected.map { it.slug })))
                    }
                }

                listOf(Spread.DAILY, Spread.WEEKLY, Spread.QUICK, Spread.YES_NO,
                    Spread.UNIVERSE, Spread.CELTIC).forEach { spread ->
                    val id = "result_${spread.name.lowercase(Locale.US)}"
                    attempt(id) {
                        live.onActivity { activity -> model.open(fixture(model, spread)); render(activity) }
                        await(live, id) { it.binding.actionBar.tvCenter.text == spread.title }
                        capturePages(live, id, JSONObject().put("spread", spread.name)
                            .put("cards", JSONArray(model.reading!!.cards.map { it.slug })),
                            maxPages = if (spread == Spread.CELTIC) 2 else 1) {
                            it.binding.root.findViewWithTag("content_scroll")
                        }
                    }
                }
                destination(live, "settings", "settings")
                listOf(0 to "edit_name", 1 to "edit_birthday", 2 to "edit_sign").forEach { (index, name) ->
                    attempt("settings_$name") {
                        show(live, "settings")
                        live.onActivity { activity ->
                            val group = activity.binding.screenHost.findViewById<ViewGroup>(R.id.personalGroup)
                            val rows = (0 until group.childCount).map(group::getChildAt).filter { it.isClickable }
                            assertEquals(3, rows.size)
                            rows[index].performClick()
                        }
                        captureSheet(live, "settings_$name")
                    }
                }
                attempt("guidance") {
                    show(live, "spreads")
                    live.onActivity { activity ->
                        // Call the actual production sheet, bypassing request()/daily DB gates and ads.
                        TarotActivity::class.java.getDeclaredMethod("guidance", Spread::class.java)
                            .apply { isAccessible = true }.invoke(activity, Spread.QUICK)
                    }
                    captureSheet(live, "guidance")
                }
            } finally {
                live.onActivity { (field(it, "sheet") as? BottomSheetDialog)?.dismiss() }
                val databaseAfter = runBlocking { model.repository.dao.observe().first() }
                manifest.put("readingDatabaseUnchanged", databaseBefore == databaseAfter)
                assertEquals("No reading, favorite or snapshot may be persisted", databaseBefore, databaseAfter)
            }
        }
    }

    private fun fixture(model: TarotViewModel, spread: Spread): ReadingEntity {
        val cards = model.cards.take(spread.cardCount).mapIndexed { index, card ->
            TarotRules.interpret(card, spread, index, false)
        }
        val snapshot = ReadingSnapshot("design-memory-${spread.name}", spread.name, FIXTURE_TIME, cards)
        return ReadingEntity(snapshot.id, snapshot.spread, snapshot.createdAt, gson.toJson(snapshot), spread == Spread.QUICK)
    }

    private fun fixtureHistory(model: TarotViewModel) = listOf(Spread.DAILY, Spread.QUICK, Spread.CELTIC)
        .map { fixture(model, it) }

    private fun destination(
        live: ImmersiveNavigationTest.LiveActivity<TarotActivity>, id: String, screen: String,
        configure: (TarotViewModel) -> Unit = {}
    ) = attempt(id) {
        show(live, screen, configure)
        capturePages(live, id, JSONObject().put("screen", screen), maxPages = if (screen == "spreads") 2 else 1) {
            it.binding.root.findViewWithTag("content_scroll")
        }
    }

    private fun show(
        live: ImmersiveNavigationTest.LiveActivity<TarotActivity>, screen: String,
        configure: (TarotViewModel) -> Unit = {}
    ) {
        live.onActivity { activity ->
            (field(activity, "sheet") as? BottomSheetDialog)?.dismiss()
            configure(vm(activity))
            vm(activity).screen = screen
            render(activity)
        }
        await(live, screen) {
            vm(it).cards.size == 78 && field(it, "renderedScreen") == screen && it.binding.screenHost.childCount == 1
        }
    }

    private fun render(activity: TarotActivity) {
        TarotActivity::class.java.getDeclaredMethod("render").apply { isAccessible = true }.invoke(activity)
    }

    private fun captureSheet(live: ImmersiveNavigationTest.LiveActivity<TarotActivity>, id: String) {
        lateinit var sheet: BottomSheetDialog
        await(live, "$id sheet") {
            sheet = field(it, "sheet") as? BottomSheetDialog ?: return@await false
            sheet.isShowing && sheet.window?.decorView?.isLaidOut == true
        }
        try {
            live.onActivity {
                val surface = requireNotNull(sheet.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet))
                assertEquals("Hidden system navigation must not leave a transparent strip below the sheet", 0, surface.paddingBottom)
            }
            capture(live, id, JSONObject().put("surface", "BottomSheetDialog")) { requireNotNull(sheet.window) }
        } finally {
            live.onActivity { sheet.dismiss() }
        }
    }

    /** Capture overlapping native viewports; no synthetic stitched/software bitmap. */
    private fun <T : Activity> capturePages(
        live: ImmersiveNavigationTest.LiveActivity<T>, id: String, state: JSONObject = JSONObject(),
        maxPages: Int = 1,
        scroll: (T) -> ScrollView?
    ) {
        val pageLimit = maxPages.coerceIn(1, MAX_PAGES)
        var host: ScrollView? = null
        live.onActivity { host = scroll(it); host?.scrollTo(0, 0) }
        settle(live)
        var page = 0
        var previous = -1
        do {
            var y = 0
            var maximum = 0
            live.onActivity {
                host?.let { view ->
                    y = view.scrollY
                    maximum = ((view.getChildAt(0)?.height ?: 0) - view.height + view.paddingTop + view.paddingBottom).coerceAtLeast(0)
                }
            }
            if (y == previous) break
            capture(live, if (page == 0) id else "${id}_page_${page + 1}", JSONObject(state.toString())
                .put("page", page + 1).put("scrollY", y).put("maxScrollY", maximum)
                .put("pageLimit", pageLimit).put("hasMoreContent", y < maximum)
                .put("truncatedAtPageLimit", page + 1 == pageLimit && y < maximum))
            if (y >= maximum || page + 1 >= pageLimit) break
            previous = y
            live.onActivity { host?.let { it.scrollTo(0, minOf(maximum, y + (it.height * .8f).toInt().coerceAtLeast(1))) } }
            settle(live)
            page++
        } while (page < pageLimit)
    }

    private fun <T : Activity> capture(
        live: ImmersiveNavigationTest.LiveActivity<T>, id: String, state: JSONObject,
        window: (T) -> Window = { it.window }
    ) {
        settle(live)
        // Bounded wait for visible Glide work; animations do not need global idle.
        val imageDeadline = SystemClock.uptimeMillis() + GLIDE_WAIT_MS
        while (SystemClock.uptimeMillis() < imageDeadline) {
            var pending = false
            live.onActivity { pending = descendants(window(it).decorView).filterIsInstance<ImageView>()
                .any { image -> visible(image) && request(image)?.isRunning == true } }
            if (!pending) break
            val remaining = imageDeadline - SystemClock.uptimeMillis()
            if (remaining <= 0) break
            SystemClock.sleep(minOf(40L, remaining))
        }
        val metadata = JSONObject().put("id", id).put("state", state)
        val views = JSONArray()
        val images = JSONArray()
        var stableBounds = Rect()
        live.onActivity { activity ->
            val root = window(activity).decorView
            activity.findViewById<ViewGroup>(R.id.actionBar)?.let { bar ->
                val titleBounds = bar.findViewById<View>(R.id.actionBarTitleBounds)
                assertEquals("Every screen uses the shared 70dp ActionBar", activity.resources.getDimensionPixelSize(R.dimen.shared_actionbar_height), bar.height)
                assertEquals("Title band is 10dp above the ActionBar bottom", activity.resources.getDimensionPixelSize(R.dimen.shared_actionbar_title_bottom_margin), bar.height - titleBounds.bottom)
                listOf(R.id.tvCenter, R.id.tvStart, R.id.brand, R.id.btnActionBarLeft,
                    R.id.btnActionBarRight, R.id.libraryToggle).forEach { id ->
                    val child = bar.findViewById<View>(id)
                    if (child.visibility == View.VISIBLE) {
                        assertEquals("Top aligned: ${activity.resources.getResourceEntryName(id)}", titleBounds.top, child.top)
                        assertEquals("Bottom aligned: ${activity.resources.getResourceEntryName(id)}", titleBounds.bottom, child.bottom)
                        assertEquals(0f, child.translationY, .1f)
                    }
                }
            }
            metadata.put("activity", activity.javaClass.name).put("sdk", Build.VERSION.SDK_INT)
                .put("density", activity.resources.displayMetrics.density.toDouble())
                .put("densityDpi", activity.resources.displayMetrics.densityDpi)
                .put("fontScale", activity.resources.configuration.fontScale.toDouble())
                .put("locale", activity.resources.configuration.locales.toLanguageTags())
                .put("timezone", TimeZone.getDefault().id)
                .put("rootLocationOnScreen", JSONArray(location(root).toList()))
            val insets = ViewCompat.getRootWindowInsets(root)?.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            val origin = location(activity.window.decorView)
            stableBounds = Rect(origin[0], origin[1] + (insets?.top ?: 0),
                origin[0] + activity.window.decorView.width, origin[1] + activity.window.decorView.height - (insets?.bottom ?: 0))
            val roots = listOf(activity.window.decorView, root).distinct()
            roots.flatMap(::descendants).distinct().forEach { view ->
                if (view.id != View.NO_ID) {
                    val name = runCatching { view.resources.getResourceEntryName(view.id) }.getOrDefault(view.id.toString())
                    val rect = Rect()
                    view.getGlobalVisibleRect(rect)
                    val entry = JSONObject().put("id", name).put("class", view.javaClass.simpleName)
                        .put("window", if (view.rootView === root) "capture" else "host")
                        .put("bounds", rectangle(bounds(view))).put("visibleBounds", rectangle(rect))
                        .put("visibility", view.visibility).put("selected", view.isSelected)
                    if (view is TextView) entry.put("text", view.text.toString())
                        .put("textSizePx", view.textSize.toDouble()).put("gravity", view.gravity)
                        .put("lineHeightPx", view.lineHeight).put("typefaceStyle", view.typeface?.style ?: 0)
                    views.put(entry)
                }
                if (view is ImageView && visible(view)) {
                    val request = request(view)
                    val status = when {
                        request == null -> "local_drawable"
                        request.isComplete -> "complete"
                        !request.isRunning -> "incomplete_placeholder"
                        else -> "pending"
                    }
                    // Freeze unresolved work before capture, with an explicit metadata flag.
                    if (request?.isRunning == true) {
                        Glide.with(view).clear(view)
                        view.setImageResource(R.drawable.tarot_deck_back)
                    }
                    images.put(JSONObject().put("bounds", rectangle(bounds(view)))
                        .put("description", view.contentDescription?.toString().orEmpty()).put("status", status))
                }
            }
            metadata.put("views", views).put("images", images).put("comparisonBounds", rectangle(stableBounds))
                .put("artValidated", false).put("glideWaitLimitMs", GLIDE_WAIT_MS)
        }
        settle(live)
        val bitmap = requireNotNull(instrumentation.uiAutomation.takeScreenshot()) { "Native screenshot unavailable" }
        try {
            metadata.put("widthPx", bitmap.width).put("heightPx", bitmap.height)
            File(directory, "$id.png").outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
            metadata.put("regression", compareBaseline(id, bitmap, stableBounds))
        } finally {
            bitmap.recycle()
            File(directory, "$id.json").writeText(metadata.toString(2))
        }
        records.put(JSONObject().put("id", id).put("status", "captured").put("png", "$id.png").put("metadata", "$id.json"))
        writeManifest()
    }

    private fun compareBaseline(id: String, actual: Bitmap, region: Rect): JSONObject {
        val base = arguments.getString("designBaselineDir") ?: return JSONObject().put("status", "capture_only")
        val file = File(base, "$id.png")
        val baseline = BitmapFactory.decodeFile(file.absolutePath)
        if (baseline == null) {
            failures += "$id: missing/unreadable baseline $file"
            return JSONObject().put("status", "missing_baseline").put("path", file.absolutePath)
        }
        try {
            if (baseline.width != actual.width || baseline.height != actual.height) {
                failures += "$id: baseline dimensions differ"
                return JSONObject().put("status", "dimension_mismatch")
            }
            val tolerance = (arguments.getString("designPixelTolerance")?.toIntOrNull() ?: 8).coerceIn(0, 255)
            val limit = (arguments.getString("designMaxMismatch")?.toDoubleOrNull() ?: .005).coerceIn(0.0, 1.0)
            val bounds = Rect(region)
            check(bounds.intersect(0, 0, actual.width, actual.height)) { "Empty comparison region" }
            val expectedRow = IntArray(bounds.width())
            val actualRow = IntArray(bounds.width())
            var changed = 0L
            for (y in bounds.top until bounds.bottom) {
                baseline.getPixels(expectedRow, 0, expectedRow.size, bounds.left, y, expectedRow.size, 1)
                actual.getPixels(actualRow, 0, actualRow.size, bounds.left, y, actualRow.size, 1)
                expectedRow.indices.forEach { x ->
                    val a = actualRow[x]; val b = expectedRow[x]
                    if (maxOf(abs(Color.red(a) - Color.red(b)), abs(Color.green(a) - Color.green(b)),
                            abs(Color.blue(a) - Color.blue(b))) > tolerance) changed++
                }
            }
            val ratio = changed.toDouble() / (bounds.width().toLong() * bounds.height())
            if (ratio > limit) failures += "$id: mismatch $ratio exceeds $limit"
            return JSONObject().put("status", if (ratio <= limit) "passed" else "failed")
                .put("mismatchRatio", ratio).put("maxMismatchRatio", limit).put("channelTolerance", tolerance)
        } finally { baseline.recycle() }
    }

    private fun attempt(id: String, action: () -> Unit) {
        try { action() } catch (error: Exception) { recordFailure(id, error) }
        catch (error: AssertionError) { recordFailure(id, error) }
    }

    private fun recordFailure(id: String, error: Throwable) {
        failures += "$id: ${error.javaClass.simpleName}: ${error.message}"
        records.put(JSONObject().put("id", id).put("status", "failed").put("error", error.stackTraceToString()))
        writeManifest()
    }

    private fun writeManifest() {
        manifest.put("directory", directory.absolutePath).put("captures", records)
            .put("failures", JSONArray(failures)).put("updatedAt", System.currentTimeMillis())
        File(directory, "manifest.json").writeText(manifest.toString(2))
    }

    private fun <T : Activity> await(live: ImmersiveNavigationTest.LiveActivity<T>, description: String, ready: (T) -> Boolean) {
        val deadline = SystemClock.uptimeMillis() + 10_000
        while (SystemClock.uptimeMillis() < deadline) {
            var done = false
            live.onActivity { done = ready(it) }
            if (done) return
            SystemClock.sleep(40)
        }
        error("Timed out awaiting $description")
    }

    private fun <T : Activity> settle(live: ImmersiveNavigationTest.LiveActivity<T>) {
        await(live, "laid-out activity window") { it.window.decorView.isLaidOut && !it.window.decorView.isLayoutRequested }
        // Bounded frames, independent of repeating shimmer/marquee/title animations.
        SystemClock.sleep(160)
    }

    private fun vm(activity: TarotActivity) = ViewModelProvider(activity)[TarotViewModel::class.java]
    private fun location(view: View) = IntArray(2).also(view::getLocationOnScreen)
    private fun bounds(view: View): Rect = location(view).let { Rect(it[0], it[1], it[0] + view.width, it[1] + view.height) }
    private fun rectangle(rect: Rect) = JSONArray(listOf(rect.left, rect.top, rect.right, rect.bottom))
    private fun visible(view: View) = view.isShown && view.getGlobalVisibleRect(Rect())
    private fun request(view: ImageView) = view.getTag(com.bumptech.glide.R.id.glide_custom_view_target_tag) as? Request
    private fun descendants(root: View): List<View> = buildList {
        add(root)
        if (root is ViewGroup) for (index in 0 until root.childCount) addAll(descendants(root.getChildAt(index)))
    }

    private fun field(target: Any, name: String): Any? {
        val type = if (target is TarotActivity) TarotActivity::class.java else target.javaClass
        return type.getDeclaredField(name).apply { isAccessible = true }.get(target)
    }

    private fun setField(target: Any, name: String, value: Any) {
        target.javaClass.getDeclaredField(name).apply { isAccessible = true }.set(target, value)
    }

    private fun restorePreference(preferences: SharedPreferences, key: String, snapshot: Map<String, *>) {
        val editor = preferences.edit()
        when (val value = snapshot[key]) {
            null -> editor.remove(key)
            is Boolean -> editor.putBoolean(key, value)
            is String -> editor.putString(key, value)
            is Int -> editor.putInt(key, value)
            is Long -> editor.putLong(key, value)
            is Float -> editor.putFloat(key, value)
            else -> error("Unsupported original $key preference type: ${value.javaClass}")
        }
        check(editor.commit()) { "Cannot restore $key" }
    }

    /** Same ContextWrapper isolation pattern as ProfilePreferencesTest, with no disk file. */
    private class MemoryPreferences(private val values: Map<String, Any>) : SharedPreferences {
        override fun getAll(): MutableMap<String, *> = values.toMutableMap()
        override fun contains(key: String?) = values.containsKey(key)
        override fun getString(key: String?, defValue: String?) = values[key] as? String ?: defValue
        override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? = defValues?.toMutableSet()
        override fun getInt(key: String?, defValue: Int) = values[key] as? Int ?: defValue
        override fun getLong(key: String?, defValue: Long) = values[key] as? Long ?: defValue
        override fun getFloat(key: String?, defValue: Float) = values[key] as? Float ?: defValue
        override fun getBoolean(key: String?, defValue: Boolean) = values[key] as? Boolean ?: defValue
        override fun edit(): SharedPreferences.Editor = error("Capture fixtures are read-only; never Submit or toggle persistence controls")
        override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit
        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit
    }

    private companion object {
        const val MAX_PAGES = 6
        const val GLIDE_WAIT_MS = 1_000L
        const val FIXTURE_TIME = 1704888000000L // 2024-01-10T12:00:00Z, independent of run date.
    }
}

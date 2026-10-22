package com.tarot.card.meaning.tarot

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.view.ContextThemeWrapper
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.tarot.card.meaning.R
import com.tarot.card.meaning.core.extensions.applyActionBarTopInset
import com.tarot.card.meaning.databinding.ActivityLanguageBinding
import com.tarot.card.meaning.databinding.ActivityPermissionBinding
import com.tarot.card.meaning.databinding.ActivityTarotBinding
import com.tarot.card.meaning.databinding.ActivityTarotProfileBinding
import com.tarot.card.meaning.databinding.LayoutActionbarBinding
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Inflation and geometry checks; no profile, preferences or reading data is written. */
@RunWith(AndroidJUnit4::class)
class SharedActionBarTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    private fun checkOnMain(theme: Int, check: (Context, LayoutInflater) -> Unit) {
        instrumentation.runOnMainSync {
            val context = ContextThemeWrapper(instrumentation.targetContext, theme)
            check(context, LayoutInflater.from(context))
        }
    }

    private fun checkTarotStyle(bar: LayoutActionbarBinding, ink: Int, profile: Boolean = false) {
        assertEquals(ink, bar.tvCenter.currentTextColor)
        assertEquals(ink, bar.btnActionBarLeft.imageTintList!!.defaultColor)
        assertEquals((if (profile) 24f else 20f) * bar.root.resources.displayMetrics.scaledDensity, bar.tvCenter.textSize, .1f)
        if (profile) assertEquals(android.view.Gravity.START, bar.tvCenter.gravity and android.view.Gravity.RELATIVE_HORIZONTAL_GRAVITY_MASK)
        val titleParams = bar.tvCenter.layoutParams as ConstraintLayout.LayoutParams
        assertEquals(0, titleParams.width)
        assertEquals(R.id.actionBarTitleBounds, titleParams.topToTop)
        assertEquals(R.id.actionBarTitleBounds, titleParams.bottomToBottom)
        assertEquals(0, titleParams.height)
        assertEquals(0f, bar.tvCenter.translationY, .1f)
        assertEquals(bar.root.resources.getDimensionPixelSize(R.dimen.shared_actionbar_height), bar.root.layoutParams.height)
    }

    @Test fun tarotLightUsesSharedBarWithScopedXmlStyles() = checkOnMain(R.style.TarotThemeLight) { _, inflater ->
        val page = ActivityTarotBinding.inflate(inflater)
        assertSame(page.root.findViewById(R.id.actionBar), page.actionBar.root)
        checkTarotStyle(page.actionBar, Color.parseColor("#172A2A"))
    }

    @Test fun tarotDarkKeepsReadableTitleAndBackIcon() = checkOnMain(R.style.TarotThemeDark) { _, inflater ->
        checkTarotStyle(ActivityTarotBinding.inflate(inflater).actionBar, Color.parseColor("#EFF5F3"))
    }

    @Test fun profileUsesTheSameBarInBothThemes() {
        listOf(R.style.TarotThemeLight to "#172A2A", R.style.TarotThemeDark to "#EFF5F3").forEach { (theme, ink) ->
            checkOnMain(theme) { _, inflater ->
                val page = ActivityTarotProfileBinding.inflate(inflater)
                checkTarotStyle(page.actionBar, Color.parseColor(ink), profile = true)
                assertSame(page.profilePage, page.actionBar.root.parent)
                // The title/back button stay outside the scrolling form.
                assertNotSame(page.profileScroll, page.actionBar.root.parent)
            }
        }
    }

    @Test fun languageAndPermissionShareGeometryButKeepTheirTypography() = checkOnMain(R.style.Theme_BaseProject) { context, inflater ->
        val language = ActivityLanguageBinding.inflate(inflater)
        val permission = ActivityPermissionBinding.inflate(inflater)
        listOf(language.actionBar, permission.actionBar).forEach { bar ->
            assertEquals(context.resources.getDimensionPixelSize(R.dimen.shared_actionbar_height), bar.root.layoutParams.height)
            assertEquals(0, bar.tvCenter.layoutParams.width)
            assertEquals(context.resources.getDimension(R.dimen.sp_28), bar.tvCenter.textSize, .1f)
            assertEquals(Gravity.CENTER, bar.tvCenter.gravity)
            assertEquals(View.GONE, bar.brand.visibility)
            assertEquals(View.GONE, bar.libraryToggle.visibility)
        }
    }

    @Test fun onboardingTitlesMatchTarotTitlePositionAndDoNotAccumulateInsets() =
        checkOnMain(R.style.Theme_BaseProject) { context, inflater ->
            val language = ActivityLanguageBinding.inflate(inflater)
            val permission = ActivityPermissionBinding.inflate(inflater)
            val tarotContext = ContextThemeWrapper(context, R.style.TarotThemeLight)
            val tarot = ActivityTarotBinding.inflate(LayoutInflater.from(tarotContext))
            val density = context.resources.displayMetrics.density
            listOf(language.root to language.layoutActionBar, permission.root to permission.layoutActionBar).forEach { (root, host) ->
                root.applyActionBarTopInset(host)
                val bar = if (root === language.root) language.actionBar else permission.actionBar
                bar.tvStart.visibility = View.GONE
                bar.tvCenter.visibility = View.VISIBLE
                for (insetDp in listOf(24, 48, 24)) {
                    val topInset = (insetDp * density).toInt()
                    val insets = WindowInsetsCompat.Builder()
                        .setInsets(WindowInsetsCompat.Type.statusBars(), Insets.of(0, topInset, 0, 0))
                        .build()
                    ViewCompat.dispatchApplyWindowInsets(root, insets)
                    assertEquals(topInset, (host.layoutParams as ViewGroup.MarginLayoutParams).topMargin)
                    tarot.page.setPadding(0, topInset, 0, 0)
                    tarot.actionBar.tvCenter.visibility = View.VISIBLE
                    for (widthDp in listOf(320, 360, 800)) {
                        val width = (widthDp * density).toInt()
                        val height = (900 * density).toInt()
                        listOf(root, tarot.root).forEach { page ->
                            page.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                                View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY))
                            page.layout(0, 0, width, height)
                        }
                        val onboardingTitle = bar.tvCenter
                        val tarotTitle = tarot.actionBar.tvCenter
                        assertEquals(tarotTitle.top + tarot.actionBar.root.top + tarot.page.top,
                            onboardingTitle.top + bar.root.top + host.top)
                        assertEquals(tarotTitle.bottom + tarot.actionBar.root.top + tarot.page.top,
                            onboardingTitle.bottom + bar.root.top + host.top)
                        assertEquals(width / 2f,
                            host.left + bar.root.left + (onboardingTitle.left + onboardingTitle.right) / 2f, 1f)
                    }
                }
            }
        }

    @Test fun everyButtonAndTitleSharesTheSameTopAndBottomInAllThemes() {
        listOf(R.style.Theme_BaseProject, R.style.TarotThemeLight, R.style.TarotThemeDark).forEach { theme ->
            checkOnMain(theme) { context, inflater ->
                val bar = LayoutActionbarBinding.inflate(inflater)
                val ids = listOf(R.id.tvStart, R.id.tvCenter, R.id.brand,
                    R.id.btnActionBarLeft, R.id.btnActionBarLeftText, R.id.btnReset, R.id.cvLogo,
                    R.id.imgCenter, R.id.btnActionBarCenter, R.id.btnActionBarCenterLeft,
                    R.id.btnActionBarCenterRight, R.id.btnActionBarNextRight1, R.id.btnActionBarNextRight,
                    R.id.btnActionBarRight, R.id.btnActionBarRightLang, R.id.btnShare,
                    R.id.btnActionBarRightText, R.id.libraryToggle)
                ids.forEach { bar.root.findViewById<View>(it).visibility = View.VISIBLE }
                listOf(320, 360, 800).forEach { widthDp ->
                    measureBar(bar, widthDp)
                    val band = bar.actionBarTitleBounds
                    assertEquals(context.resources.getDimensionPixelSize(R.dimen.shared_actionbar_title_bottom_margin),
                        bar.root.height - band.bottom)
                    ids.forEach { id ->
                        val view = bar.root.findViewById<View>(id)
                        val name = context.resources.getResourceEntryName(id)
                        assertEquals("$name top", band.top, view.top)
                        assertEquals("$name bottom", band.bottom, view.bottom)
                        assertEquals("$name translation", 0f, view.translationY, .1f)
                    }
                }
            }
        }
    }

    @Test fun hiddenOrSwitchedTitlesDoNotMoveTheButtons() = checkOnMain(R.style.TarotThemeLight) { _, inflater ->
        val bar = LayoutActionbarBinding.inflate(inflater)
        val titles = listOf(bar.tvCenter, bar.tvStart, bar.brand)
        bar.btnActionBarRight.visibility = View.VISIBLE
        titles.forEach { shown ->
            titles.forEach { it.visibility = if (it === shown) View.VISIBLE else View.GONE }
            // A hidden back button must not collapse the geometry of the other actions.
            bar.btnActionBarLeft.visibility = View.GONE
            measureBar(bar, 360)
            assertEquals(shown.top, bar.btnActionBarRight.top)
            assertEquals(shown.bottom, bar.btnActionBarRight.bottom)
        }
    }

    @Test fun startTitleWrapsItsContent() = checkOnMain(R.style.TarotThemeLight) { _, inflater ->
        val bar = LayoutActionbarBinding.inflate(inflater)
        bar.tvStart.visibility = View.VISIBLE
        bar.tvStart.text = "Language"
        measureBar(bar, 320)
        val shortWidth = bar.tvStart.width
        assertEquals(ViewGroup.LayoutParams.WRAP_CONTENT, bar.tvStart.layoutParams.width)

        bar.tvStart.text = "Select Star Sign"
        measureBar(bar, 320)
        assertEquals(1, bar.tvStart.lineCount)
        assertTrue(bar.tvStart.width > shortWidth)
    }

    private fun measureBar(bar: LayoutActionbarBinding, widthDp: Int) {
        val density = bar.root.resources.displayMetrics.density
        val height = bar.root.resources.getDimensionPixelSize(R.dimen.shared_actionbar_height)
        bar.root.measure(View.MeasureSpec.makeMeasureSpec((widthDp * density).toInt(), View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY))
        bar.root.layout(0, 0, bar.root.measuredWidth, bar.root.measuredHeight)
        assertEquals(height, bar.root.height)
    }

    @Test fun longTitleDoesNotOverlapCardsToggleOnNarrowPhone() = checkOnMain(R.style.TarotThemeLight) { context, inflater ->
        val page = ActivityTarotBinding.inflate(inflater)
        val bar = page.actionBar
        bar.tvCenter.visibility = View.VISIBLE
        bar.tvCenter.text = "A very long card title that must stay inside the shared ActionBar"
        bar.libraryToggle.visibility = View.VISIBLE
        val density = context.resources.displayMetrics.density
        page.root.measure(
            View.MeasureSpec.makeMeasureSpec((320 * density).toInt(), View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec((800 * density).toInt(), View.MeasureSpec.EXACTLY)
        )
        page.root.layout(0, 0, page.root.measuredWidth, page.root.measuredHeight)
        assertTrue(bar.tvCenter.width > 0)
        assertTrue(bar.tvCenter.right <= bar.libraryToggle.left)
        assertTrue(bar.libraryToggle.width > 0)
    }
}

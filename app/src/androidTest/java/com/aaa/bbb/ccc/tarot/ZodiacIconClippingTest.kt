package com.tarot.card.meaning.tarot

import android.graphics.Rect
import android.graphics.Bitmap
import android.graphics.Color
import android.os.SystemClock
import android.view.LayoutInflater
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.view.ContextThemeWrapper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.filters.SdkSuppress
import com.tarot.card.meaning.R
import com.tarot.card.meaning.databinding.ActivityTarotHomeBinding
import com.tarot.card.meaning.databinding.ItemTarotZodiacBinding
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.json.JSONObject
import org.json.JSONArray
import java.io.File

/** Inspects native clipping/outline geometry; does not modify the source PNGs. */
@RunWith(AndroidJUnit4::class)
class ZodiacIconClippingTest {
    private fun checkCircle(icon: ImageView, sizeDp: Int) {
        val size = (sizeDp * icon.resources.displayMetrics.density + .5f).toInt()
        assertEquals(size, icon.layoutParams.width)
        assertEquals(size, icon.layoutParams.height)
        icon.layout(0, 0, size, size)
        assertFalse(icon.clipToOutline)
        assertNull(icon.background)
        assertEquals(0, icon.paddingLeft + icon.paddingTop + icon.paddingRight + icon.paddingBottom)
        assertEquals(ImageView.ScaleType.FIT_CENTER, icon.scaleType)
    }

    @Test fun allTwelveSelectedAndUnselectedIconsKeepFullArtworkInBothThemes() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            listOf(R.style.TarotThemeLight, R.style.TarotThemeDark).forEach { theme ->
                val context = ContextThemeWrapper(instrumentation.targetContext, theme)
                val inflater = LayoutInflater.from(context)
                val parent = android.widget.GridLayout(context)
                zodiacSigns.forEach { sign ->
                    val item = ItemTarotZodiacBinding.inflate(inflater, parent, false)
                    item.zodiacIcon.setImageResource(sign.image)
                    listOf(false, true).forEach { selected ->
                        item.root.isSelected = selected
                        item.root.refreshDrawableState()
                        checkCircle(item.zodiacIcon, 26)
                    }
                }
                // The common grid still dispatches the original sign on tap.
                var chosen = ""
                val grid = TarotUi(context, theme == R.style.TarotThemeDark).zodiacGrid("Aries") { chosen = it }
                assertEquals(12, grid.childCount)
                grid.getChildAt(1).performClick()
                assertEquals("Taurus", chosen)
            }
        }
    }

    @Test fun homeBadgeUsesFullArtworkWithoutChangingItsSize() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            listOf(R.style.TarotThemeLight, R.style.TarotThemeDark).forEach { theme ->
                val context = ContextThemeWrapper(instrumentation.targetContext, theme)
                val parent = LinearLayout(context)
                val home = ActivityTarotHomeBinding.inflate(LayoutInflater.from(context), parent, false)
                checkCircle(home.zodiacIcon, 16)
            }
        }
    }

    /** Capture real hardware-composited pixels, not a software Canvas simulation. */
    @Test
    @SdkSuppress(minSdkVersion = 29)
    fun selectedPiscesHasNoWhiteRimInTheActualProfileWindow() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val directory = File(instrumentation.targetContext.cacheDir, "zodiac_design_check").apply { mkdirs() }
        ImmersiveNavigationTest.LiveActivity(TarotProfileActivity::class.java).use { scenario ->
            listOf(false, true).forEach { selected ->
                scenario.onActivity { activity ->
                    val grid = activity.binding.zodiacGrid.getChildAt(0) as GridLayout
                    if (selected) grid.getChildAt(11).performClick()
                    val current = (activity.binding.zodiacGrid.getChildAt(0) as GridLayout).getChildAt(11)
                    current.requestRectangleOnScreen(Rect(0, 0, current.width, current.height), true)
                }
                SystemClock.sleep(250) // Allow a frame after selection/layout, without waiting for global idle.
                val metadata = JSONObject()
                var iconRect = Rect()
                scenario.onActivity { activity ->
                    val tile = (activity.binding.zodiacGrid.getChildAt(0) as GridLayout).getChildAt(11)
                    val icon = tile.findViewById<ImageView>(R.id.zodiacIcon)
                    assertEquals(selected, tile.isSelected)
                    val location = IntArray(2)
                    icon.getLocationOnScreen(location)
                    iconRect = Rect(location[0], location[1], location[0] + icon.width, location[1] + icon.height)
                    val tileLocation = IntArray(2)
                    tile.getLocationOnScreen(tileLocation)
                    if (selected && icon.width == 52 && tile.width == 208) {
                        assertEquals("Reference icon horizontal position inside the tile", 78, iconRect.left - tileLocation[0])
                        assertEquals("Reference icon vertical position inside the tile", 20, iconRect.top - tileLocation[1])
                    }
                    metadata.put("icon", JSONArray(listOf(iconRect.left, iconRect.top, iconRect.right, iconRect.bottom)))
                    metadata.put("tile", JSONArray(listOf(tileLocation[0], tileLocation[1], tileLocation[0] + tile.width, tileLocation[1] + tile.height)))
                    metadata.put("density", activity.resources.displayMetrics.density.toDouble())
                }
                val screenshot = requireNotNull(instrumentation.uiAutomation.takeScreenshot())
                try {
                    if (selected) {
                        val gold = Color.rgb(246, 233, 182)
                        listOf(iconRect.left to iconRect.top,
                            iconRect.right - 1 to iconRect.top,
                            iconRect.left to iconRect.bottom - 1,
                            iconRect.right - 1 to iconRect.bottom - 1).forEach { (x, y) ->
                            assertEquals("Selected icon edge must reveal the design's gold background", gold, screenshot.getPixel(x, y))
                        }
                    }
                    val name = if (selected) "selected" else "unselected"
                    File(directory, "$name.png").outputStream().use { screenshot.compress(Bitmap.CompressFormat.PNG, 100, it) }
                    File(directory, "$name.json").writeText(metadata.toString())
                } finally { screenshot.recycle() }
            }
        }
    }
}

package com.tarot.card.meaning.tarot

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.view.ContextThemeWrapper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.tarot.card.meaning.R
import com.tarot.card.meaning.databinding.ItemTarotSpreadBinding
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Isolated layout rendering only: no profile, reading, preferences or ad calls. */
@RunWith(AndroidJUnit4::class)
class SpreadLockUiTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    private fun onMain(theme: Int = R.style.TarotThemeLight, check: (Context, ItemTarotSpreadBinding) -> Unit) {
        instrumentation.runOnMainSync {
            val context = ContextThemeWrapper(instrumentation.targetContext, theme)
            val row = ItemTarotSpreadBinding.inflate(LayoutInflater.from(context))
            row.spreadTitle.text = "Yes / No"
            row.spreadDescription.text = "Find clarity on your question"
            row.spreadIcon.setImageResource(R.drawable.tarot_spread_icon_5)
            check(context, row)
        }
    }

    private fun measure(context: Context, row: ItemTarotSpreadBinding, widthDp: Int) {
        val density = context.resources.displayMetrics.density
        row.root.measure(
            View.MeasureSpec.makeMeasureSpec((widthDp * density).toInt(), View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec((64 * density).toInt(), View.MeasureSpec.EXACTLY)
        )
        row.root.layout(0, 0, row.root.measuredWidth, row.root.measuredHeight)
    }

    private fun showLock(row: ItemTarotSpreadBinding) {
        row.spreadLock.text = "20h"
        row.spreadLockBadge.visibility = View.VISIBLE
        row.spreadLockOverlay.visibility = View.VISIBLE
    }

    @Test fun unlockedRowDoesNotReserveSpaceForTheBadge() = onMain { context, row ->
        measure(context, row, 360)
        assertEquals(View.GONE, row.spreadLockBadge.visibility)
        assertEquals(View.GONE, row.spreadLockOverlay.visibility)
        val column = row.spreadTitle.parent as View
        val bounds = Rect(0, 0, column.width, column.height)
        row.root.offsetDescendantRectToMyCoords(column, bounds)
        val padding = (12 * context.resources.displayMetrics.density).toInt()
        assertEquals(row.root.width - padding, bounds.right)
    }

    @Test fun badgeStaysAboveTheOverlayAndClearOfTextInBothThemes() {
        listOf(R.style.TarotThemeLight, R.style.TarotThemeDark).forEach { theme ->
            onMain(theme) { context, row ->
                row.spreadTitle.text = "Message From The Universe"
                showLock(row)
                listOf(320, 360, 800).forEach { width ->
                    measure(context, row, width)
                    val column = row.spreadTitle.parent as View
                    val bounds = Rect(0, 0, column.width, column.height)
                    row.root.offsetDescendantRectToMyCoords(column, bounds)
                    assertTrue(bounds.right <= row.spreadLockBadge.left)
                    assertEquals((24 * context.resources.displayMetrics.density).toInt(), row.spreadLockBadge.height)
                    assertTrue(row.root.indexOfChild(row.spreadLockBadge) > row.root.indexOfChild(row.spreadLockOverlay))
                    assertNotNull(row.spreadLockIcon.drawable)
                    assertTrue(row.spreadLockIcon.width > 0 && row.spreadLockIcon.height > 0)
                    assertTrue(row.spreadLock.left >= row.spreadLockIcon.right)
                    assertEquals(1f, row.spreadLock.alpha, 0f)
                    assertEquals(1f, row.spreadTitle.alpha, 0f)
                    assertEquals(1f, row.spreadIcon.alpha, 0f)
                }
                assertFalse(row.spreadLockOverlay.isClickable)
                assertFalse(row.spreadLockOverlay.isFocusable)
                var clicked = false
                row.root.setOnClickListener { clicked = true }
                assertTrue(row.root.performClick())
                assertTrue(clicked)
            }
        }
    }

    @Test fun renderedBadgeKeepsItsOriginalColorWhileTheRowIsDimmed() = onMain { context, row ->
        val directory = File(context.cacheDir, "spread_lock_review").apply { check(mkdirs() || isDirectory) }
        fun capture(name: String): Bitmap {
            val bitmap = Bitmap.createBitmap(row.root.width, row.root.height, Bitmap.Config.ARGB_8888)
            row.root.draw(Canvas(bitmap))
            File(directory, "$name.png").outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
            return bitmap
        }
        measure(context, row, 360)
        val unlocked = capture("unlocked")
        showLock(row)
        measure(context, row, 360)
        val locked = capture("locked")
        try {
            val density = context.resources.displayMetrics.density
            val sampleY = (4 * density).toInt()
            assertTrue(Color.red(locked.getPixel(locked.width / 2, sampleY)) <
                Color.red(unlocked.getPixel(unlocked.width / 2, sampleY)))
            val badgeX = row.spreadLockBadge.left + row.spreadLockBadge.width / 2
            val badgeY = row.spreadLockBadge.top + sampleY
            assertEquals(Color.parseColor("#FCFBF7"), locked.getPixel(badgeX, badgeY))
            val icon = row.spreadLockIcon
            val iconBounds = Rect(0, 0, icon.width, icon.height)
            row.root.offsetDescendantRectToMyCoords(icon, iconBounds)
            val gold = context.getColor(R.color.tarot_gold)
            var goldPixels = 0
            for (y in iconBounds.top until iconBounds.bottom) {
                for (x in iconBounds.left until iconBounds.right) {
                    if (locked.getPixel(x, y) == gold) goldPixels++
                }
            }
            assertTrue("The padlock must visibly render its gold keyhole", goldPixels > 0)
        } finally {
            unlocked.recycle()
            locked.recycle()
        }
    }
}

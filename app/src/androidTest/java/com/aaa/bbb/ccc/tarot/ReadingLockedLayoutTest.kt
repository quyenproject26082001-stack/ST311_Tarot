package com.tarot.card.meaning.tarot

import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.view.ContextThemeWrapper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.tarot.card.meaning.R
import com.tarot.card.meaning.databinding.OverlayTarotReadingLockedBinding
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.roundToInt

@RunWith(AndroidJUnit4::class)
class ReadingLockedLayoutTest {
    @Test fun popupStaysBottomAlignedAndWithinThePageWidthInBothThemes() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            for (theme in listOf(R.style.TarotThemeLight, R.style.TarotThemeDark)) {
                val context = ContextThemeWrapper(instrumentation.targetContext, theme)
                val density = context.resources.displayMetrics.density
                val maxWidth = context.resources.getDimensionPixelSize(R.dimen.tarot_max_page_width)
                for (widthDp in listOf(360, 840)) {
                    val form = OverlayTarotReadingLockedBinding.inflate(LayoutInflater.from(context))
                    form.lockedForm.lockedDescription.text = context.getString(R.string.tarot_locked_description, 23)
                    form.lockedForm.remainingTime.text = context.getString(R.string.tarot_wait_time, "22:41")
                    val width = (widthDp * density).roundToInt()
                    val height = (800 * density).roundToInt()
                    form.root.measure(
                        View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)
                    )
                    form.root.layout(0, 0, width, height)
                    assertEquals(height, form.lockedPanel.bottom)
                    assertTrue(form.lockedPanel.width <= maxWidth)
                    assertTrue(form.lockedPanel.height > 0)
                    assertTrue(kotlin.math.abs(form.lockedPanel.left + form.lockedPanel.right - width) <= 1)
                    assertEquals(context.getString(R.string.tarot_reading_locked), form.lockedForm.lockedTitle.text.toString())
                    assertEquals(context.getString(R.string.tarot_watch_ad), form.lockedForm.watchAdLabel.text.toString())
                    assertTrue(form.lockedForm.watchAd.isClickable)
                    assertTrue(form.root.isClickable)
                    assertFalse(form.root.hasOnClickListeners())
                }
            }
        }
    }
}

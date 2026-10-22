package com.tarot.card.meaning.tarot

import android.content.res.Configuration
import android.os.Build
import android.text.Layout
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.view.ContextThemeWrapper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.tarot.card.meaning.R
import com.tarot.card.meaning.databinding.ItemTarotResultCardBinding
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.roundToInt

/** Layout-only checks: no navigation, profile, history or preference changes. */
@RunWith(AndroidJUnit4::class)
class CardMeaningLayoutTest {
    @Test fun worldAdviceWrapsWithoutJustifyingOrTruncatingText() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            val target = instrumentation.targetContext
            val advice = "Mark the achievement, finish the handover and give the next beginning some space."
            for (theme in listOf(R.style.TarotThemeLight, R.style.TarotThemeDark)) {
                for (scale in listOf(1f, 1.3f, 2f)) {
                    val config = Configuration(target.resources.configuration).apply { fontScale = scale }
                    val context = ContextThemeWrapper(target.createConfigurationContext(config), theme)
                    val row = ItemTarotResultCardBinding.inflate(LayoutInflater.from(context))
                    row.cardImage.visibility = View.GONE
                    row.cardTitle.visibility = View.GONE
                    row.interpretation.text = "Upright reflection\n" +
                        "Completion invites you to recognise what has come together and carry its lessons into the next chapter.\n\n" +
                        advice + "\n\nReversed reflection\n" +
                        "Something may feel almost complete while an unfinished detail or unacknowledged lesson still asks for attention."
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        assertEquals(Layout.JUSTIFICATION_MODE_NONE, row.interpretation.justificationMode)
                    }
                    for (pageWidth in listOf(320, 360, 393, 520)) {
                        // The screen has 20dp content padding on each side.
                        val width = ((pageWidth - 40) * context.resources.displayMetrics.density).roundToInt()
                        row.root.measure(
                            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
                        )
                        row.root.layout(0, 0, row.root.measuredWidth, row.root.measuredHeight)
                        val text = row.interpretation
                        val layout = text.layout
                        val available = text.width - text.totalPaddingLeft - text.totalPaddingRight
                        for (line in 0 until layout.lineCount) {
                            assertEquals(0, layout.getEllipsisCount(line))
                            assertTrue("Line exceeds text width at $pageWidth dp / font $scale",
                                layout.getLineRight(line) <= available + 1f)
                            assertTrue(layout.getLineLeft(line) >= -1f)
                        }
                        assertEquals(text.text.length, layout.getLineEnd(layout.lineCount - 1))
                        assertTrue(text.height - text.totalPaddingTop - text.totalPaddingBottom >= layout.height)
                        assertTrue(text.text.contains(advice))
                    }
                }
            }
        }
    }
}

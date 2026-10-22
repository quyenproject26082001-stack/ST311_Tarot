package com.tarot.card.meaning.tarot

import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.view.ContextThemeWrapper
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.tarot.card.meaning.R
import com.tarot.card.meaning.databinding.ActivityTarotHomeBinding
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class QuickActionsLayoutTest {
    @Test fun actionCardsKeepRatioAndGapsAtDifferentWidths() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val context = ContextThemeWrapper(
                InstrumentationRegistry.getInstrumentation().targetContext,
                R.style.TarotThemeLight
            )
            val density = context.resources.displayMetrics.density
            val gap = context.resources.getDimensionPixelSize(R.dimen.dp_12)
            listOf(320, 480).forEach { widthDp ->
                val home = ActivityTarotHomeBinding.inflate(LayoutInflater.from(context))
                val width = (widthDp * density).toInt()
                home.root.measure(
                    View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
                )
                home.root.layout(0, 0, width, home.root.measuredHeight)

                val cards = listOf(home.actionDaily.root, home.actionWeekly.root, home.actionCeltic.root)
                assertEquals(0, cards.first().left)
                assertEquals(home.quickActions.width, cards.last().right)
                assertEquals(gap, cards[1].left - cards[0].right)
                assertEquals(gap, cards[2].left - cards[1].right)
                cards.forEach { card ->
                    assertTrue(card.width > 0)
                    assertEquals(card.width * 122.5f / 99f, card.height.toFloat(), 1.5f)
                }
                listOf(home.actionDaily, home.actionWeekly, home.actionCeltic).forEach { action ->
                    val params = action.actionTitle.layoutParams as ConstraintLayout.LayoutParams
                    assertEquals(.92f, params.verticalBias, .001f)
                }
            }
        }
    }
}

package com.aaa.bbb.ccc.tarot

import android.graphics.Color
import android.widget.*
import androidx.core.view.isVisible
import com.aaa.bbb.ccc.R
import com.aaa.bbb.ccc.databinding.*

class TarotSpreadsActivity : TarotActivity() {
    override val activityScreen = "spreads"

    override fun renderScreen() {
        renderSpreads(scrollContent())
    }

    private fun renderSpreads(content: LinearLayout) {
        val form = ActivityTarotSpreadsBinding.inflate(layoutInflater, content, true)
        val icons = listOf(R.drawable.tarot_spread_icon_0, R.drawable.tarot_spread_icon_1, R.drawable.tarot_spread_icon_2,
            R.drawable.tarot_spread_icon_3, R.drawable.tarot_spread_icon_4, R.drawable.tarot_spread_icon_5, R.drawable.tarot_spread_icon_6, R.drawable.tarot_spread_icon_7)
        val colors = listOf("#FCFBF7", "#FFFBE9", "#F1E7FF", "#E1FBFB", "#FCFBF7", "#FFF6F8", "#FDF6EE", "#FCFBF7")
        val decorations = listOf(R.drawable.tarot_spread_decor_0, R.drawable.tarot_spread_decor_1, R.drawable.tarot_spread_decor_2,
            R.drawable.tarot_spread_decor_3, R.drawable.tarot_spread_decor_4, R.drawable.tarot_spread_decor_5, R.drawable.tarot_spread_decor_6, R.drawable.tarot_spread_decor_7)
        Spread.entries.forEachIndexed { index, spread ->
            val item = ItemTarotSpreadBinding.inflate(layoutInflater, form.spreadItems, false)
            item.root.background = ui.shape(if (ui.dark || index >= colors.size) ui.surface else Color.parseColor(colors[index]), 16, ui.green)
            // Only the first eight spread illustrations appear in the supplied
            // screenshot. A neutral card icon avoids assigning another spread's art.
            item.spreadIcon.setImageResource(icons.getOrNull(index) ?: R.drawable.tarot_ic_cards)
            item.spreadBackground.isVisible = !ui.dark && index < decorations.size
            decorations.getOrNull(index)?.let(item.spreadBackground::setImageResource)
            item.spreadTitle.text = spread.title
            if (ui.dark) item.spreadTitle.setTextColor(ui.ink)
            item.spreadDescription.text = spread.description
            val latest = vm.history.firstOrNull { it.spread == spread.name }
            val remaining = TarotTestConfig.remaining(spread, latest?.createdAt, System.currentTimeMillis())
            item.spreadLockBadge.isVisible = remaining > 0
            item.spreadLockOverlay.isVisible = remaining > 0
            item.spreadLock.text = lockDuration(remaining)
            item.root.setOnClickListener { request(spread) }
            form.spreadItems.addView(item.root)
        }
    }
}

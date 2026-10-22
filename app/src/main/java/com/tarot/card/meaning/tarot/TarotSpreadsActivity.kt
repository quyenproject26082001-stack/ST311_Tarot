package com.tarot.card.meaning.tarot

import android.graphics.Color
import android.widget.*
import androidx.core.view.isVisible
import com.tarot.card.meaning.R
import com.tarot.card.meaning.databinding.*

class TarotSpreadsActivity : TarotActivity() {
    override val activityScreen = "spreads"

    override fun renderScreen() {
        renderSpreads(scrollContent())
    }

    private fun renderSpreads(content: LinearLayout) {
        val form = ActivityTarotSpreadsBinding.inflate(layoutInflater, content, true)
        val icons = listOf(R.drawable.tarot_spread_icon_0, R.drawable.tarot_spread_icon_1, R.drawable.tarot_spread_icon_2,
            R.drawable.tarot_spread_icon_3, R.drawable.tarot_spread_icon_4, R.drawable.tarot_spread_icon_5, R.drawable.tarot_spread_icon_6, R.drawable.tarot_spread_icon_7,
            R.drawable.tarot_spread_icon_8, R.drawable.tarot_spread_icon_9, R.drawable.tarot_spread_icon_10, R.drawable.tarot_spread_icon_11,
            R.drawable.tarot_spread_icon_12, R.drawable.tarot_spread_icon_13, R.drawable.tarot_spread_icon_14, R.drawable.tarot_spread_icon_15)
        val colors = listOf("#FCFBF7", "#FFFBE9", "#F1E7FF", "#E1FBFB", "#FCFBF7", "#FFF6F8", "#FDF6EE", "#FCFBF7",
            "#FFFBE9", "#F1E7FF", "#FDF6EE", "#FCFBF7", "#FFF6F8", "#FDF6EE", "#E1FBFB", "#FFFBE9")
        val decorations = listOf(R.drawable.tarot_spread_decor_0, R.drawable.tarot_spread_decor_1, R.drawable.tarot_spread_decor_2,
            R.drawable.tarot_spread_decor_3, R.drawable.tarot_spread_decor_4, R.drawable.tarot_spread_decor_5, R.drawable.tarot_spread_decor_6, R.drawable.tarot_spread_decor_7,
            R.drawable.tarot_spread_decor_8, R.drawable.tarot_spread_decor_9, R.drawable.tarot_spread_decor_10, R.drawable.tarot_spread_decor_11,
            R.drawable.tarot_spread_decor_12, R.drawable.tarot_spread_decor_13, R.drawable.tarot_spread_decor_14, R.drawable.tarot_spread_decor_15)
        Spread.entries.forEachIndexed { index, spread ->
            val item = ItemTarotSpreadBinding.inflate(layoutInflater, form.spreadItems, false)
            val fill = if (index >= colors.size) {
                if (ui.dark) Color.parseColor("#FCFBF7") else ui.surface
            } else Color.parseColor(colors[index])
            item.root.background = ui.shape(fill, 16, ui.green)
            item.spreadIcon.setImageResource(icons.getOrNull(index) ?: R.drawable.tarot_ic_cards)
            item.spreadBackground.isVisible = index < decorations.size
            decorations.getOrNull(index)?.let(item.spreadBackground::setImageResource)
            item.spreadTitle.text = ui.translations.text(spread.title)
            if (ui.dark) item.spreadDescription.setTextColor(Color.parseColor("#60706D"))
            item.spreadDescription.text = ui.translations.text(spread.description)
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

package com.tarot.card.meaning.tarot

import android.widget.*
import com.tarot.card.meaning.databinding.*

class TarotHistoryActivity : TarotActivity() {
    override val activityScreen = "history"

    override fun renderScreen() {
        renderHistory(scrollContent())
    }

    private fun renderHistory(content: LinearLayout) {
        if (vm.history.isEmpty()) {
            val empty = TarotEmptyHistoryBinding.inflate(layoutInflater, content, true)
            empty.beginReading.setOnClickListener { vm.screen = "spreads" }
        } else vm.history.forEach { readingRow(content, it, showFavorite = true) }
    }
}

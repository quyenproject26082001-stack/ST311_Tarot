package com.aaa.bbb.ccc.tarot

import android.widget.*
import com.aaa.bbb.ccc.databinding.*

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

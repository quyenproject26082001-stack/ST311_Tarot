package com.aaa.bbb.ccc.tarot

import android.widget.*
import androidx.core.view.isVisible
import com.aaa.bbb.ccc.R
import com.aaa.bbb.ccc.databinding.*

class TarotCardDetailActivity : TarotActivity() {
    override val activityScreen = "detail"

    override fun renderScreen() {
        renderDetail(scrollContent())
    }

    private fun renderDetail(content: LinearLayout) {
        val card = vm.cards.firstOrNull { it.slug == vm.detailSlug } ?: return
        val detail = ItemTarotResultCardBinding.inflate(layoutInflater, content, true)
        loadCard(detail.cardImage, card.imageUrl)
        detail.cardTitle.isVisible = false
        detail.interpretation.text = card.meaning
        if (card.original != null) {
            detail.interpretation.text = getString(R.string.tarot_upright_meaning) + "\n" + card.meaning +
                "\n\n" + getString(R.string.tarot_reversed_meaning) + "\n" +
                card.original.reversed.general + "\n\n" + card.original.reversed.advice
            detail.meaningNotice.isVisible = true
            detail.meaningNotice.setText(R.string.tarot_original_interpretation_notice)
        }
    }
}

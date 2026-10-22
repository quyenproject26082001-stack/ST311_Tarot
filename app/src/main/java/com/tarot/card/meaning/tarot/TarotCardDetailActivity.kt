package com.tarot.card.meaning.tarot

import android.widget.*
import androidx.core.view.isVisible
import com.tarot.card.meaning.R
import com.tarot.card.meaning.databinding.*

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
        detail.interpretation.text = ui.translations.text(card.meaning)
        if (card.original != null) {
            detail.interpretation.text = getString(R.string.tarot_upright_meaning) + "\n" + ui.translations.text(card.meaning) +
                "\n\n" + getString(R.string.tarot_reversed_meaning) + "\n" +
                ui.translations.text(card.original.reversed.general) + "\n\n" + ui.translations.text(card.original.reversed.advice)
            detail.meaningNotice.isVisible = true
            detail.meaningNotice.setText(R.string.tarot_original_interpretation_notice)
        }
    }
}

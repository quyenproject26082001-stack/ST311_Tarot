package com.aaa.bbb.ccc.tarot

import android.widget.*
import androidx.core.view.isVisible
import com.aaa.bbb.ccc.R
import com.aaa.bbb.ccc.databinding.*

class TarotResultActivity : TarotActivity() {
    override val activityScreen = "result"

    override fun renderScreen() {
        renderResult(scrollContent())
    }

    private fun renderResult(content: LinearLayout) {
        val reading = vm.reading ?: return
        val spread = Spread.valueOf(reading.spread)
        if (spread == Spread.CELTIC) celticLayout(content, reading.cards)
        if (spread == Spread.UNIVERSE) {
            val pair = TarotPairBinding.inflate(layoutInflater, content, true)
            listOf(pair.firstCard, pair.secondCard).zip(reading.cards).forEach { (image, card) ->
                loadCard(image, card.imageUrl); image.rotation = if (card.reversed) 180f else 0f
            }
            pair.pairTitle.text = reading.cards.joinToString(" & ") { it.title }
            // Keep old snapshots unchanged; only new readings contain a combined original message.
            pair.pairMeaning.text = reading.combinedInterpretation?.let {
                getString(R.string.tarot_original_interpretation_notice) + "\n\n" + it
            } ?: (getString(R.string.tarot_pair_missing) + "\n\n" +
                reading.cards.joinToString("\n\n") { "${it.title}\n${it.interpretation}" })
        }
        reading.cards.takeUnless { spread == Spread.UNIVERSE }.orEmpty().forEach { card ->
            val item = ItemTarotResultCardBinding.inflate(layoutInflater, content, true)
            item.positionTitle.isVisible = reading.cards.size > 1 && spread != Spread.UNIVERSE
            item.positionTitle.text = card.position
            item.cardImage.isVisible = spread != Spread.UNIVERSE
            if (item.cardImage.isVisible) loadCard(item.cardImage, card.imageUrl)
            item.cardImage.rotation = if (card.reversed) 180f else 0f
            item.cardTitle.text = card.title
            val notice = when {
                card.contentId == TarotInterpretations.CONTENT_ID -> R.string.tarot_original_interpretation_notice
                card.reversed -> R.string.tarot_reverse_missing
                card.generalMeaning && spread != Spread.UNIVERSE -> R.string.tarot_general_meaning
                else -> null
            }
            item.meaningNotice.isVisible = notice != null
            if (notice != null) item.meaningNotice.setText(notice)
            item.answer.isVisible = card.answer != null
            item.answerLabel.isVisible = card.answer != null
            item.answer.text = card.answer
            item.interpretation.text = card.interpretation
        }
        ui.add(content, ui.button(getString(R.string.tarot_finish)) { finishScreen() })
    }

    /** Placement and card aspect ratios are declared in tarot_celtic_diagram.xml. */
    private fun celticLayout(content: LinearLayout, cards: List<ReadingCard>) {
        val diagram = TarotCelticDiagramBinding.inflate(layoutInflater, content, true)
        bindCelticCards(diagram, cards)
    }
}

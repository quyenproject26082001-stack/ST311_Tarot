package com.tarot.card.meaning.tarot

import android.content.Context
import org.json.JSONObject

class TarotTranslations(context: Context) {
    private val locale = context.resources.configuration.locales[0]
    private val translations = catalog(context.applicationContext, locale.language)
    val isLocalized: Boolean get() = translations.isNotEmpty()

    fun text(value: String): String = translations[value] ?: value.split('\n').joinToString("\n") {
        translations[it] ?: it
    }

    fun reading(reading: ReadingSnapshot, cards: List<TarotCard>): ReadingSnapshot {
        val spread = Spread.valueOf(reading.spread)
        val deck = cards.associateBy { it.slug }
        val localizedCards = reading.cards.mapIndexed { index, saved ->
            val card = deck[saved.slug]
            if (saved.contentId == TarotInterpretations.CONTENT_ID && card?.original != null) {
                TarotInterpretations.interpret(card, spread, index, saved.reversed, ::text)
            } else saved.copy(title = text(saved.title), position = text(saved.position),
                answer = saved.answer?.let(::text))
        }
        val combined = if (spread == Spread.UNIVERSE && reading.cards.size == 2 &&
            reading.cards.all { it.contentId == TarotInterpretations.CONTENT_ID && deck[it.slug]?.original != null }) {
            TarotInterpretations.universeMessage(
                deck.getValue(reading.cards[0].slug), reading.cards[0].reversed,
                deck.getValue(reading.cards[1].slug), reading.cards[1].reversed, ::text
            )
        } else reading.combinedInterpretation
        return reading.copy(cards = localizedCards, combinedInterpretation = combined)
    }

    companion object {
        private val catalogs = mutableMapOf<String, Map<String, String>>()

        @Synchronized
        private fun catalog(context: Context, language: String): Map<String, String> {
            val code = if (language == "in") "id" else language
            if (code !in setOf("de", "es", "fr", "hi", "pt", "id")) return emptyMap()
            return catalogs.getOrPut(code) {
                val json = context.assets.open("tarot/translations_$code.json").bufferedReader().use {
                    JSONObject(it.readText())
                }
                json.keys().asSequence().associateWith { json.getString(it) }
            }
        }
    }
}

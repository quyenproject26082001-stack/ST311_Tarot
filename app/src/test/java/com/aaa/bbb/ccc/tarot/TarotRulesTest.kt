package com.aaa.bbb.ccc.tarot

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.Calendar
import java.util.TimeZone
import kotlin.random.Random

class TarotRulesTest {
    private val cards: List<TarotCard> = File("src/main/assets/tarot/tarot_data.json").reader().use {
        Gson().fromJson(it, object : TypeToken<List<TarotCard>>() {}.type)
    }
    private fun time(day: Int, hour: Int, minute: Int = 0): Long = Calendar.getInstance(TimeZone.getTimeZone("Asia/Bangkok")).apply {
        clear(); set(2026, Calendar.OCTOBER, day, hour, minute)
    }.timeInMillis

    @Test fun suppliedDeckHasEveryUniqueCardAndConsistentCategories() {
        assertEquals(78, cards.size)
        assertEquals(78, cards.map { it.slug }.distinct().size)
        assertEquals(listOf(14, 14, 14, 14, 22), cards.groupBy { it.category }.values.map { it.size }.sorted())
    }
    @Test fun dailyResetsAtMidnightRatherThanAfter24Hours() {
        val zone = TimeZone.getTimeZone("Asia/Bangkok")
        assertTrue(TarotRules.sameDay(time(9, 0), time(9, 23, 59), zone))
        assertFalse(TarotRules.sameDay(time(9, 23, 59), time(10, 0), zone))
        assertEquals(ReadingRule.DAILY, Spread.WEEKLY.rule)
    }
    @Test fun universeUnlocksExactlyAfter23Hours() {
        val start = time(9, 22)
        assertEquals(TarotRules.COOLDOWN_MS, TarotRules.remaining(start, start))
        assertEquals(1L, TarotRules.remaining(start, start + TarotRules.COOLDOWN_MS - 1))
        assertEquals(0L, TarotRules.remaining(start, start + TarotRules.COOLDOWN_MS))
        assertEquals(0L, TarotRules.remaining(null, start))
    }
    @Test fun shufflingNeverDuplicatesACardAndHonorsReversalLimits() {
        assertEquals(78, TarotRules.deck(cards, 50, Random(4)).map { it.slug }.distinct().size)
        assertTrue(TarotRules.deck(cards, 0).none { it.reversed })
        assertTrue(TarotRules.deck(cards, 100).all { it.reversed })
        assertNotEquals(cards.map { it.slug }, TarotRules.deck(cards, 0, Random(8)).map { it.slug })
    }
    @Test fun all16SpreadsMatchTheProvidedCardCounts() {
        assertEquals(listOf(1, 2, 10, 2, 3, 1, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3), Spread.entries.map { it.cardCount })
        assertTrue(Spread.entries.filter { it !in listOf(Spread.DAILY, Spread.WEEKLY, Spread.UNIVERSE) }.all { it.rule == ReadingRule.UNLIMITED })
    }
    @Test fun incorrectYesNoLabelsAreResolvedFromExplicitConclusions() {
        assertEquals("Yes", TarotRules.answer(cards.first { it.slug == "the-emperor" }.yesNo))
        assertEquals("Yes", TarotRules.answer(cards.first { it.slug == "strength" }.yesNo))
        assertEquals("No", TarotRules.answer(cards.first { it.slug == "death" }.yesNo))
        assertEquals("Maybe", TarotRules.answer(cards.first { it.slug == "two-of-swords" }.yesNo))
    }
    @Test fun positionalAndMissingInterpretationsAreNotConfused() {
        val card = cards.first()
        assertEquals(card.positions["past"], TarotRules.interpret(card, Spread.QUICK, 0, false).interpretation)
        assertFalse(TarotRules.interpret(card, Spread.QUICK, 0, false).generalMeaning)
        assertTrue(TarotRules.interpret(card, Spread.CELTIC, 0, false).generalMeaning)
        assertTrue(TarotRules.interpret(card, Spread.DAILY, 0, true).generalMeaning)
        assertNull(TarotRules.interpret(card, Spread.YES_NO, 0, true).answer)
    }
}

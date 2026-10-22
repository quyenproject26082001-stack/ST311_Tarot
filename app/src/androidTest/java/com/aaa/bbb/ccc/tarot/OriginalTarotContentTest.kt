package com.tarot.card.meaning.tarot

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.gson.Gson
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Reads production assets and snapshot parsing. Does not insert readings or change preferences. */
@RunWith(AndroidJUnit4::class)
class OriginalTarotContentTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun repositoryLoadsTheCompleteOriginalCatalogWithoutChangingHistory() = runBlocking {
        val repository = TarotRepository(context)
        val before = repository.dao.observe().first()
        val cards = repository.loadCards()
        assertEquals(78, cards.size)
        assertTrue(cards.all { it.original != null })
        var checked = 0
        cards.forEach { card -> Spread.entries.forEach { spread ->
            spread.positions.indices.forEach { position -> listOf(false, true).forEach { reversed ->
                val result = TarotRules.interpret(card, spread, position, reversed)
                assertEquals(TarotInterpretations.CONTENT_ID, result.contentId)
                assertFalse(result.generalMeaning)
                checked++
            } }
        } }
        assertEquals(7644, checked)
        assertEquals(before, repository.dao.observe().first())
    }

    @Test fun repositoryReadsLegacyAndOriginalSnapshotsWithoutReinterpretingEither() = runBlocking {
        val repository = TarotRepository(context)
        val before = repository.dao.observe().first()
        val legacy = """{"id":"legacy-content-test","spread":"DAILY","createdAt":123,"cards":[{"slug":"the-fool","title":"The Fool","imageUrl":"https://example.invalid/card.png","position":"Today's Message","reversed":true,"interpretation":"Saved legacy words must not change.","generalMeaning":true}]}"""
        val old = repository.snapshot(ReadingEntity("legacy-content-test", "DAILY", 123, legacy))
        assertEquals("Saved legacy words must not change.", old.cards.single().interpretation)
        assertNull(old.cards.single().contentId)
        assertNull(old.combinedInterpretation)

        val cards = repository.loadCards()
        val first = cards.first { it.slug == "the-hanged-man" }
        val second = cards.first { it.slug == "the-star" }
        val reading = ReadingSnapshot("original-content-test", Spread.UNIVERSE.name, 456,
            listOf(TarotRules.interpret(first, Spread.UNIVERSE, 0, false), TarotRules.interpret(second, Spread.UNIVERSE, 1, true)),
            TarotInterpretations.universeMessage(first, false, second, true))
        val row = ReadingEntity(reading.id, reading.spread, reading.createdAt, Gson().toJson(reading))
        assertEquals(reading, repository.snapshot(row))
        assertEquals(before, repository.dao.observe().first())
    }
}

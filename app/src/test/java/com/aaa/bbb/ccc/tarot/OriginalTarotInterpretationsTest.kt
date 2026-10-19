package com.aaa.bbb.ccc.tarot

import com.google.gson.Gson
import com.google.gson.JsonNull
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class OriginalTarotInterpretationsTest {
    private val gson = Gson()
    private val originalJson = File("src/main/assets/${TarotInterpretations.ASSET_PATH}").readText()
    private val raw = gson.fromJson(File("src/main/assets/tarot/tarot_data.json").readText(), Array<TarotCard>::class.java).toList()
    private val cards = TarotInterpretations.attach(raw, originalJson)

    @Test fun all78CardsHaveTwoDistinctOriginalProfiles() {
        assertEquals(78, cards.size)
        assertEquals(raw.map { it.slug }, cards.map { it.slug })
        val versions = cards.flatMap { listOf(it.original!!.upright, it.original.reversed) }
        assertEquals(156, versions.map { it.general }.distinct().size)
        assertEquals(156, versions.map { it.connection }.distinct().size)
        assertEquals(156, versions.map { it.work }.distinct().size)
        assertEquals(156, versions.map { it.advice }.distinct().size)
        cards.zip(raw).forEach { (card, old) ->
            assertEquals(old.imageUrl, card.imageUrl)
            assertEquals(old.title, card.title)
            assertEquals(old.category, card.category)
            assertNotEquals(old.meaning, card.meaning)
            assertEquals(card.original!!.upright.answer, card.yesNo.answer)
        }
    }

    @Test fun everyCardOrientationAndSpreadPositionResolvesWithoutAFallback() {
        var checked = 0
        cards.forEach { card ->
            Spread.entries.forEach { spread ->
                spread.positions.indices.forEach { index ->
                    listOf(false, true).forEach { reversed ->
                        val result = TarotRules.interpret(card, spread, index, reversed)
                        assertEquals(TarotInterpretations.CONTENT_ID, result.contentId)
                        assertFalse(result.generalMeaning)
                        assertEquals(spread.positions[index], result.position)
                        assertEquals(reversed, result.reversed)
                        assertTrue(result.interpretation.length > 150)
                        assertFalse(result.interpretation.contains("Trusted Tarot"))
                        assertFalse(result.interpretation.contains("null"))
                        val profile = if (reversed) card.original!!.reversed else card.original!!.upright
                        assertTrue(result.interpretation.contains(profile.general))
                        assertTrue(result.interpretation.contains(profile.advice))
                        if (spread == Spread.YES_NO) assertEquals(profile.answer, result.answer)
                        else assertNull(result.answer)
                        checked++
                    }
                }
            }
        }
        assertEquals(7644, checked)
    }

    @Test fun hangedManChangesWithTopicAndPositionNotJustTheHeading() {
        val card = cards.first { it.slug == "the-hanged-man" }
        val money = TarotRules.interpret(card, Spread.MONEY, 1, false).interpretation
        val love = TarotRules.interpret(card, Spread.LOVE, 1, false).interpretation
        assertTrue(money.contains(card.original!!.upright.work))
        assertFalse(money.contains(card.original.upright.connection))
        assertTrue(love.contains(card.original.upright.connection))
        assertFalse(love.contains(card.original.upright.work))
        assertNotEquals(money, love)
        assertNotEquals(money, TarotRules.interpret(card, Spread.MONEY, 0, false).interpretation)
        assertNotEquals(love, TarotRules.interpret(card, Spread.LOVE, 1, true).interpretation)
    }

    @Test fun positionsDifferExceptForTheIntentionallyUnlabelledCallingAndUniverseCards() {
        val card = cards.first { it.slug == "the-star" }
        Spread.entries.filter { it !in listOf(Spread.CALLING, Spread.UNIVERSE) }.forEach { spread ->
            val texts = spread.positions.indices.map { TarotRules.interpret(card, spread, it, false).interpretation }
            assertEquals(spread.name, spread.cardCount, texts.distinct().size)
        }
        val calling = (0..2).map { TarotRules.interpret(card, Spread.CALLING, it, false).interpretation }
        assertEquals(1, calling.distinct().size)
        assertFalse(calling.first().contains("For your past"))
        assertFalse(calling.first().contains("For your future"))
    }

    @Test fun reversalsAreAuthoredSeparatelyAndYesNoIsNotBlindlyInverted() {
        val hanged = cards.first { it.slug == "the-hanged-man" }
        assertEquals("Maybe", TarotRules.interpret(hanged, Spread.YES_NO, 0, false).answer)
        assertEquals("Maybe", TarotRules.interpret(hanged, Spread.YES_NO, 0, true).answer)
        val emperor = cards.first { it.slug == "the-emperor" }
        assertEquals("Yes", TarotRules.interpret(emperor, Spread.YES_NO, 0, false).answer)
        assertEquals("No", TarotRules.interpret(emperor, Spread.YES_NO, 0, true).answer)
    }

    @Test fun allDistinctUniversePairsResolveInAllFourOrientations() {
        var checked = 0
        cards.forEach { first -> cards.filter { it.slug != first.slug }.forEach { second ->
            listOf(false, true).forEach { firstReversed -> listOf(false, true).forEach { secondReversed ->
                val result = TarotInterpretations.universeMessage(first, firstReversed, second, secondReversed)
                assertTrue(result.contains(first.title))
                assertTrue(result.contains(second.title))
                val a = if (firstReversed) first.original!!.reversed else first.original!!.upright
                val b = if (secondReversed) second.original!!.reversed else second.original!!.upright
                assertTrue(result.contains(a.general) && result.contains(b.general))
                assertTrue(result.contains(a.advice) && result.contains(b.advice))
                assertFalse(result.contains("not included in the supplied dataset"))
                checked++
            } }
        } }
        assertEquals(24024, checked)
    }

    @Test fun universeIncludesAConnectionBetweenThemesNotOnlyTwoCardDescriptions() {
        val hanged = cards.first { it.slug == "the-hanged-man" }
        val fool = cards.first { it.slug == "the-fool" }
        val result = TarotInterpretations.universeMessage(hanged, false, fool, false)
        assertTrue(result.contains("balance between making space and taking a step"))
        assertTrue(TarotInterpretations.universeMessage(hanged, true, fool, false).contains("(reversed)"))
        assertInvalid { TarotInterpretations.universeMessage(hanged, false, hanged, true) }
    }

    @Test fun oldSnapshotRemainsUnchangedAndHasNoInventedProvenanceOrCombinedMessage() {
        val legacyCard = TarotRules.interpret(raw.first(), Spread.UNIVERSE, 0, true)
        val old = gson.toJson(ReadingSnapshot("legacy", Spread.UNIVERSE.name, 123L, listOf(legacyCard)))
        assertFalse(old.contains("contentId"))
        assertFalse(old.contains("combinedInterpretation"))
        val restored = gson.fromJson(old, ReadingSnapshot::class.java)
        assertEquals(legacyCard.interpretation, restored.cards.first().interpretation)
        assertNull(restored.cards.first().contentId)
        assertNull(restored.combinedInterpretation)
        assertTrue(restored.cards.first().generalMeaning)
    }

    @Test fun newSnapshotPersistsTheExactDisplayedCombinedMessageAndOrigin() {
        val a = cards.first { it.slug == "the-hanged-man" }
        val b = cards.first { it.slug == "the-star" }
        val snapshot = ReadingSnapshot("new", Spread.UNIVERSE.name, 456L,
            listOf(TarotRules.interpret(a, Spread.UNIVERSE, 0, false), TarotRules.interpret(b, Spread.UNIVERSE, 1, true)),
            TarotInterpretations.universeMessage(a, false, b, true))
        assertEquals(snapshot, gson.fromJson(gson.toJson(snapshot), ReadingSnapshot::class.java))
        assertTrue(snapshot.cards.all { it.contentId == TarotInterpretations.CONTENT_ID })
    }

    @Test fun catalogRejectsMissingDuplicateUnknownOrIncompleteCards() {
        fun mutate(change: (com.google.gson.JsonObject) -> Unit): String {
            val value = gson.fromJson(originalJson, com.google.gson.JsonObject::class.java)
            change(value)
            return gson.toJson(value)
        }
        assertInvalid { TarotInterpretations.attach(raw, mutate { it.getAsJsonArray("cards").remove(0) }) }
        assertInvalid { TarotInterpretations.attach(raw, mutate {
            it.getAsJsonArray("cards")[1].asJsonObject.addProperty("slug", "the-fool")
        }) }
        assertInvalid { TarotInterpretations.attach(raw, mutate {
            it.getAsJsonArray("cards")[0].asJsonObject.addProperty("slug", "not-a-card")
        }) }
        assertInvalid { TarotInterpretations.attach(raw, mutate {
            it.getAsJsonArray("cards")[0].asJsonObject.getAsJsonObject("reversed").add("work", JsonNull.INSTANCE)
        }) }
        assertInvalid { TarotInterpretations.attach(raw, mutate { it.addProperty("schemaVersion", 99) }) }
        assertInvalid { TarotInterpretations.attach(raw, mutate { it.addProperty("origin", "some-other-source") }) }
        assertInvalid { TarotInterpretations.attach(raw, mutate {
            it.getAsJsonArray("cards")[0].asJsonObject.getAsJsonObject("upright").addProperty("answer", "Guaranteed")
        }) }
    }

    @Test fun invalidPositionIsRejectedInsteadOfShowingAnotherPositionsMeaning() {
        assertInvalid { TarotRules.interpret(cards.first(), Spread.DAILY, -1, false) }
        assertInvalid { TarotRules.interpret(cards.first(), Spread.CELTIC, 10, true) }
    }

    private fun assertInvalid(block: () -> Unit) {
        assertTrue("Expected invalid content to be rejected", runCatching(block).exceptionOrNull() is IllegalArgumentException)
    }
}

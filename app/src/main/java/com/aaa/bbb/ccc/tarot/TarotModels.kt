package com.aaa.bbb.ccc.tarot

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import java.util.Calendar
import java.util.TimeZone
import kotlin.random.Random

@Keep
data class TarotCard(
    val slug: String,
    val title: String,
    val category: String,
    val keywords: String,
    @SerializedName("keywords_list") val keywordsList: List<String>,
    val meaning: String,
    val positions: Map<String, String>,
    @SerializedName("yes_no") val yesNo: YesNo,
    val astrology: String,
    @SerializedName("image_url") val imageUrl: String,
    @SerializedName("source_url") val sourceUrl: String,
    @Transient val original: OriginalCardMeaning? = null
)

@Keep
data class YesNo(val interpretation: String, val answer: String)

enum class ReadingRule { DAILY, COOLDOWN, UNLIMITED }

enum class Spread(
    val title: String,
    val description: String,
    val positions: List<String>,
    val rule: ReadingRule = ReadingRule.COOLDOWN,
    val symbol: String = "✦"
) {
    DAILY("Daily Reading", "Your guidance for today", listOf("Today's Message"), ReadingRule.DAILY, "☀"),
    WEEKLY("Weekly Reading", "Your challenges and opportunities", listOf("This Week's Challenges", "This Week's Opportunities"), ReadingRule.DAILY, "☾"),
    CELTIC("Celtic Cross", "Explore your situation in depth", listOf("Current Situation", "Immediate Challenge", "Aspiration & Goal", "Root Cause & Foundation", "Recent Past", "Immediate Future", "Internal Influences", "External Influences", "Hopes & Fears", "Final Outcome"), rule = ReadingRule.UNLIMITED, symbol = "✚"),
    UNIVERSE("Message from the Universe", "A message from two cards", listOf("First Card", "Second Card"), ReadingRule.COOLDOWN, "✉"),
    QUICK("Quick Reading", "A quick message for you", listOf("Past", "Present", "Future"), symbol = "⚡"),
    YES_NO("Yes / No", "Find clarity on your question", listOf("Your Answer"), symbol = "?"),
    PAST_PRESENT_FUTURE("Past, Present & Future", "See your journey unfold", listOf("Past", "Present", "Future"), symbol = "⌛"),
    SITUATION("Situation, Obstacle & Advice", "Understand your situation", listOf("Situation", "Obstacle", "Advice")),
    RELATIONSHIPS("Relationships", "Explore your connection", listOf("What Brings You Together?", "What Pulls You Apart?", "What Needs Work?"), symbol = "♡"),
    LOVE("Love", "Explore your love life", listOf("What Has Shaped Your Love Life?", "What Is Happening in Your Love Life?", "Where Is Your Love Life Heading?"), symbol = "♥"),
    MONEY("Money & Career", "Reflect on work and finances", listOf("Past", "Present", "Future"), symbol = "$"),
    PROSPERITY("Prosperity", "Find your path to prosperity", listOf("Current Situation", "Blocks to Your Prosperity", "What Should You Do?")),
    DECISION("Important Decision", "Consider your two options", listOf("What does the first option offer?", "What does the other option offer?", "What should you focus on?")),
    MIND_BODY_SPIRIT("Mind, Body & Spirit", "Reflect on your balance", listOf("Mind", "Body", "Spirit")),
    POTENTIAL("Hidden Potential", "Discover your possibilities", listOf("What helps you", "What hinders you", "Unrealized Potential")),
    CALLING("What Is Your Calling?", "Reflect on each card's meaning", listOf("Card 1", "Card 2", "Card 3"));

    val cardCount get() = positions.size
}

@Keep
data class Profile(val name: String, val birthday: String, val zodiac: String)

@Keep
data class DrawnCard(val slug: String, val reversed: Boolean)

@Keep
data class ReadingCard(
    val slug: String,
    val title: String,
    val imageUrl: String,
    val position: String,
    val reversed: Boolean,
    val interpretation: String,
    val generalMeaning: Boolean,
    val answer: String? = null,
    val contentId: String? = null
)

@Keep
data class ReadingSnapshot(
    val id: String, val spread: String, val createdAt: Long, val cards: List<ReadingCard>,
    val combinedInterpretation: String? = null
)

object TarotRules {
    const val COOLDOWN_MS = 23 * 60 * 60 * 1000L

    fun sameDay(first: Long, second: Long, zone: TimeZone = TimeZone.getDefault()): Boolean {
        val a = Calendar.getInstance(zone).apply { timeInMillis = first }
        val b = Calendar.getInstance(zone).apply { timeInMillis = second }
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR) && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)
    }

    fun remaining(lastDraw: Long?, now: Long): Long =
        if (lastDraw == null) 0 else (lastDraw + COOLDOWN_MS - now).coerceIn(0, COOLDOWN_MS)

    /** The merged rule in the user's sheet covers spreads 4–16, independently. */
    fun remaining(spread: Spread, lastDraw: Long?, now: Long): Long =
        if (spread.rule == ReadingRule.COOLDOWN) remaining(lastDraw, now) else 0

    fun deck(cards: List<TarotCard>, reverseChance: Int, random: Random = Random.Default): List<DrawnCard> {
        require(cards.map { it.slug }.distinct().size == cards.size)
        return cards.map { DrawnCard(it.slug, random.nextInt(100) < reverseChance.coerceIn(0, 100)) }
    }

    // The supplied file contains incorrect parsed answers. Resolve only explicit conclusions;
    // leave ambiguous statements as Maybe rather than inventing a binary result.
    fun answer(value: YesNo): String {
        val text = value.interpretation.lowercase().trim()
        if (text.contains("neither toward yes or no") || text.contains("no clear") || text.contains("unclear") ||
            Regex("\\bmaybe\\b").containsMatchIn(text)) return "Maybe"
        if (Regex("\\byes\\b").containsMatchIn(text)) return "Yes"
        if (Regex("(?:answer[^.!?]*\\bno\\b|\\bno is (?:your|the) answer)").containsMatchIn(text)) return "No"
        return value.answer.takeIf { it in listOf("Yes", "No", "Maybe") } ?: "Maybe"
    }

    fun interpret(card: TarotCard, spread: Spread, index: Int, reversed: Boolean): ReadingCard {
        if (card.original != null) return TarotInterpretations.interpret(card, spread, index, reversed)
        // Compatibility for legacy/imported data. Production loads the complete original catalog.
        val position = spread.positions[index]
        val positional = if (spread in listOf(Spread.QUICK, Spread.PAST_PRESENT_FUTURE)) card.positions[position.lowercase()] else null
        val isGeneral = reversed || (spread != Spread.DAILY && spread != Spread.YES_NO && spread != Spread.CALLING && positional == null)
        val text = when {
            reversed -> card.meaning
            spread == Spread.YES_NO -> card.yesNo.interpretation
            positional != null -> positional
            else -> card.meaning
        }
        return ReadingCard(card.slug, card.title, card.imageUrl, position, reversed, text, isGeneral,
            if (spread == Spread.YES_NO && !reversed) answer(card.yesNo) else null)
    }
}

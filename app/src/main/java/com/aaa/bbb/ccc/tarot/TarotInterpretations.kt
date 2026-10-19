package com.aaa.bbb.ccc.tarot

import androidx.annotation.Keep
import com.google.gson.Gson

@Keep
data class OriginalTarotCatalog(val schemaVersion: Int, val origin: String, val cards: List<OriginalCardMeaning>)

@Keep
data class OriginalCardMeaning(val slug: String, val upright: OriginalCardText, val reversed: OriginalCardText)

@Keep
data class OriginalCardText(
    val theme: String,
    val general: String,
    val connection: String,
    val work: String,
    val advice: String,
    val answer: String
)

/** Original, offline editorial content. Composes card text with the exact spread position. */
object TarotInterpretations {
    const val CONTENT_ID = "st311-original-en-v1"
    const val ASSET_PATH = "tarot/original_interpretations_en.json"
    private val answers = setOf("Yes", "No", "Maybe")
    private val themes = setOf("BEGINNING", "ACTION", "BALANCE", "BOUNDARY", "CHANGE", "CLARITY",
        "CONNECTION", "PATIENCE", "RECOVERY", "STABILITY", "UNCERTAINTY", "RELEASE", "LEARNING",
        "FULFILLMENT", "CAUTION")

    private enum class Context { GENERAL, CONNECTION, WORK }
    private data class Position(val opening: String, val prompt: String, val context: Context = Context.GENERAL)

    private val positions = mapOf(
        Spread.DAILY to listOf(Position(
            "Today's reflection is about where to place your attention, not a forecast of an unavoidable event.",
            "Choose one small way to put this message into practice today.")),
        Spread.WEEKLY to listOf(
            Position("For this week's challenges, look at the pattern that could make progress harder.",
                "What could you notice early rather than letting it grow into a larger difficulty?"),
            Position("For this week's opportunities, look for a constructive use of the card's energy.",
                "What realistic opening could you make room for during the week?")),
        Spread.CELTIC to listOf(
            Position("At the centre of this reading, consider what describes your situation most honestly.",
                "Name what is happening now before deciding what should happen next."),
            Position("As the immediate challenge, this card highlights something to work with rather than a verdict against you.",
                "Which part of this pattern needs attention before the next step?"),
            Position("In the place of aspiration and goals, consider what you are trying to build or become.",
                "Does the goal reflect your own values, and what would meaningful progress look like?"),
            Position("At the root of the situation, examine a belief, need or habit that may be supporting the visible pattern.",
                "What assumption is worth checking instead of treating it as a fact?"),
            Position("For the recent past, use this symbol to reflect on an influence you may be carrying forward.",
                "What lesson can you keep, and what part of the old response no longer helps?"),
            Position("For the immediate future, this is a possible direction to prepare for, not a promised event.",
                "What choice now would make the next step more thoughtful?"),
            Position("For internal influences, look at your own feelings, motives and available strengths.",
                "What is within your control, even if the whole situation is not?"),
            Position("For external influences, consider the people, expectations and circumstances around you.",
                "Separate what you can verify from what you are assuming about other people."),
            Position("For hopes and fears, notice what you want to happen and what you are trying to avoid.",
                "Can you acknowledge both without letting either make the decision for you?"),
            Position("For the final outcome, consider a possible direction if the current pattern continues.",
                "This is not fixed destiny. What would you keep or change to move toward an outcome you value?")),
        Spread.UNIVERSE to List(2) { Position(
            "This card contributes one theme to the combined message of the two cards.",
            "Read it alongside the other card rather than treating it as a separate prediction.") },
        Spread.QUICK to listOf(
            Position("In this quick look at the past, notice a pattern that still affects your choices.",
                "What can that experience teach you without deciding your future for you?"),
            Position("In this quick look at the present, bring attention back to what you can respond to now.",
                "What is one useful action within your reach?"),
            Position("In this quick look ahead, explore a possible direction rather than an inevitable result.",
                "What would help you prepare while remaining open to a different outcome?")),
        Spread.YES_NO to listOf(Position(
            "This Yes / No answer is a symbolic prompt, not evidence that an event will or will not happen.",
            "Check the facts and your own priorities before making a decision.")),
        Spread.PAST_PRESENT_FUTURE to listOf(
            Position("For your past, reflect on a lesson or habit you may be bringing into the situation.",
                "Which part of that experience deserves to stay, and which part can be left behind?"),
            Position("For your present, consider where your attention and energy are going.",
                "What can you respond to today instead of waiting for certainty?"),
            Position("For your future, consider the direction your current choices could support.",
                "What adjustment would make that direction more consistent with your values?")),
        Spread.SITUATION to listOf(
            Position("As the situation, this card offers a lens for understanding the issue as it stands.",
                "Describe the issue without confusing an interpretation with a proven fact."),
            Position("As the obstacle, look for the unhelpful expression of this pattern, even when the card is upright.",
                "Could too much, too little or a misdirected use of this quality be getting in the way?"),
            Position("As advice, look for a practical and proportionate response to the issue.",
                "Choose an action you can review and adjust rather than a dramatic gesture you cannot undo.")),
        Spread.RELATIONSHIPS to listOf(
            Position("For what brings you together, reflect on a shared value, need or way of connecting.",
                "What can both people recognise and nurture without one person doing all the work?", Context.CONNECTION),
            Position("For what pulls you apart, examine where needs, expectations or responses may differ.",
                "What could be discussed directly instead of guessed or blamed?", Context.CONNECTION),
            Position("For what needs work, focus on a shared practice rather than on fixing the other person.",
                "What respectful conversation or boundary would make the connection healthier?", Context.CONNECTION)),
        Spread.LOVE to listOf(
            Position("For what has shaped your love life, reflect on what earlier connections taught you.",
                "Which expectation about affection or commitment is still influencing you?", Context.CONNECTION),
            Position("For what is happening in your love life, consider the quality of connection you experience now.",
                "Compare words, actions and your own needs without assuming you know someone else's feelings.", Context.CONNECTION),
            Position("For where your love life could be heading, explore a direction rather than a guaranteed romantic event.",
                "What kind of connection would your present choices help to create?", Context.CONNECTION)),
        Spread.MONEY to listOf(
            Position("For your work and money past, reflect on an experience that shaped your sense of security or success.",
                "What useful lesson can you keep without repeating the same limitation?", Context.WORK),
            Position("For your work and money present, consider your actual commitments, resources and motivation.",
                "What practical detail deserves attention before you make your next choice?", Context.WORK),
            Position("For your work and money future, explore a possible direction, not a promise of income or employment.",
                "What measured preparation could improve your options? Do not base financial decisions on a card.", Context.WORK)),
        Spread.PROSPERITY to listOf(
            Position("For your current prosperity, consider resources broadly: time, skills, support and money.",
                "What resource is already available but not being used thoughtfully?", Context.WORK),
            Position("For blocks to prosperity, look at a pattern that could restrict sustainable progress.",
                "Are expectations, habits or commitments making your resources harder to manage?", Context.WORK),
            Position("For what you can do, focus on a sustainable response rather than a quick promise of abundance.",
                "Identify one concrete step and review its real cost and usefulness.", Context.WORK)),
        Spread.DECISION to listOf(
            Position("For the first option, use this card to explore its appeal, demands and possible trade-offs.",
                "What would this option ask you to practise or give up? It is not automatically the better choice."),
            Position("For the other option, examine its appeal and demands independently of the first.",
                "What different opportunity or trade-off becomes visible through this lens?"),
            Position("For what to focus on, return to the criteria that matter most to the decision.",
                "Compare reliable information, values and consequences rather than letting the cards choose for you.")),
        Spread.MIND_BODY_SPIRIT to listOf(
            Position("For the mind, notice the thoughts and assumptions shaping your attention.",
                "What belief could you question or describe more clearly?"),
            Position("For the body, reflect on everyday energy, rest and routines, not on a diagnosis.",
                "What gentle, realistic care is appropriate today? A card cannot assess symptoms; seek qualified care for health concerns."),
            Position("For the spirit, consider what gives your life meaning, belonging or direction.",
                "What small practice would help you feel more connected to your own values?")),
        Spread.POTENTIAL to listOf(
            Position("For what helps you, look for a strength or form of support you can use deliberately.",
                "Where could you apply this quality without expecting it to solve everything?"),
            Position("For what hinders you, examine how this pattern might become limiting or overused.",
                "Which automatic response could you practise changing?"),
            Position("For unrealised potential, consider a capacity you could develop rather than a hidden destiny.",
                "What small experiment would let you learn whether this direction suits you?")),
        // Calling deliberately has no Past/Present/Future or other invented roles.
        Spread.CALLING to List(3) { Position(
            "Consider this card on its own as one perspective on meaning and direction.",
            "Which activity would let you explore this quality in a way that feels worthwhile?") }
    )

    fun attach(cards: List<TarotCard>, json: String): List<TarotCard> {
        val catalog = requireNotNull(Gson().fromJson(json, OriginalTarotCatalog::class.java))
        require(catalog.schemaVersion == 1 && catalog.origin == CONTENT_ID) { "Unsupported original content catalog" }
        val records = requireNotNull(catalog.cards) { "Original card records are missing" }
        require(cards.size == 78 && records.size == 78) { "Expected 78 original card records" }
        val indexed = records.associateBy { it.slug }
        require(indexed.size == records.size && indexed.keys == cards.map { it.slug }.toSet()) { "Original card slugs must match the deck exactly" }
        records.forEach { row ->
            listOf(requireNotNull(row.upright), requireNotNull(row.reversed)).forEach { version ->
                require(version.theme in themes && version.answer in answers) { "Invalid original card metadata: ${row.slug}" }
                require(listOf(version.general, version.connection, version.work, version.advice).none { it.isNullOrBlank() }) {
                    "Missing original interpretation: ${row.slug}"
                }
            }
            require(row.upright.general != row.reversed.general) { "Reversed text must be distinct: ${row.slug}" }
        }
        return cards.map { card ->
            val original = indexed.getValue(card.slug)
            val enriched = card.copy(original = original,
                meaning = "${original.upright.general}\n\n${original.upright.advice}")
            enriched.copy(
                positions = listOf("past", "present", "future").mapIndexed { index, key ->
                    key to interpret(enriched, Spread.PAST_PRESENT_FUTURE, index, false).interpretation
                }.toMap(),
                yesNo = YesNo(interpret(enriched, Spread.YES_NO, 0, false).interpretation, original.upright.answer)
            )
        }
    }

    fun interpret(card: TarotCard, spread: Spread, index: Int, reversed: Boolean): ReadingCard {
        require(index in spread.positions.indices) { "Invalid position for ${spread.name}: $index" }
        val original = requireNotNull(card.original) { "Original content not loaded for ${card.slug}" }
        val version = if (reversed) original.reversed else original.upright
        val position = positions.getValue(spread)[index]
        val context = when (position.context) {
            Context.GENERAL -> version.general
            Context.CONNECTION -> "${version.general}\n\n${version.connection}"
            Context.WORK -> "${version.general}\n\n${version.work}"
        }
        val orientation = if (reversed) "Reversed reflection: " else ""
        val result = "${position.opening}\n\n$orientation$context\n\n${position.prompt}\n${version.advice}"
        return ReadingCard(card.slug, card.title, card.imageUrl, spread.positions[index], reversed, result,
            generalMeaning = false, answer = if (spread == Spread.YES_NO) version.answer else null,
            contentId = CONTENT_ID)
    }

    /** A combined reflection, not two unrelated meanings or a prewritten database of every pair. */
    fun universeMessage(first: TarotCard, firstReversed: Boolean, second: TarotCard, secondReversed: Boolean): String {
        require(first.slug != second.slug) { "A Universe reading requires two different cards" }
        val firstText = requireNotNull(first.original).let { if (firstReversed) it.reversed else it.upright }
        val secondText = requireNotNull(second.original).let { if (secondReversed) it.reversed else it.upright }
        val pair = setOf(firstText.theme, secondText.theme)
        val bridge = when {
            firstText.theme == secondText.theme -> "Both cards return to a similar theme. Give it sustained attention without assuming that repetition guarantees an outcome."
            pair.any { it in setOf("PATIENCE", "RELEASE") } && pair.any { it in setOf("ACTION", "BEGINNING") } ->
                "Together they suggest a balance between making space and taking a step. A pause can prepare a beginning; movement does not have to mean rushing."
            pair.containsAll(setOf("CONNECTION", "BOUNDARY")) ->
                "Their shared message is that closeness and clear limits can support each other. Caring for someone does not require abandoning your own needs."
            pair.containsAll(setOf("STABILITY", "CHANGE")) ->
                "Their contrast asks what is worth keeping steady while something else changes. Keep the support that helps you adapt, not the structure that only feels familiar."
            pair.any { it in setOf("CAUTION", "UNCERTAINTY") } && "CLARITY" in pair ->
                "Read together, they favour checking what you know before deciding what it means. Clear information can make room for uncertainty without pretending it has disappeared."
            "RECOVERY" in pair && "ACTION" in pair ->
                "Their combined emphasis is a sustainable pace. A useful next step should respect your capacity rather than demand that you prove you have recovered."
            else -> "Bring these themes into conversation: where do they support each other, and where does one offer a useful limit to the other? You do not need to force them into a single prediction."
        }
        fun label(card: TarotCard, reversed: Boolean) = card.title + if (reversed) " (reversed)" else ""
        return "${label(first, firstReversed)} introduces this theme: ${firstText.general}\n\n" +
            "Alongside it, ${label(second, secondReversed)} adds another perspective: ${secondText.general}\n\n" +
            "$bridge\n\nA practical way to explore the pair: ${firstText.advice} ${secondText.advice}\n\n" +
            "What small choice could honour both messages today? This is a reflection, not a message verified to come from an external force."
    }
}

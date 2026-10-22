package com.tarot.card.meaning.tarot

import java.util.Calendar

data class WeeklyHoroscope(
    val firstCard: TarotCard,
    val secondCard: TarotCard,
    val summary: String,
    val message: String
)

object WeeklyHoroscopeGenerator {
    private data class ThemeCopy(
        val focus: String,
        val quality: String,
        val action: String
    )

    private data class PairCopy(
        val summary: String,
        val connection: String,
        val guidance: String
    )

    private val themes = mapOf(
        "BEGINNING" to ThemeCopy(
            "a new beginning that needs curiosity as well as practical preparation",
            "openness to a fresh direction",
            "choose one small, reversible first step"
        ),
        "ACTION" to ThemeCopy(
            "the point where intention needs to become focused action",
            "decisive and purposeful movement",
            "use one resource you already have toward a clear result"
        ),
        "BALANCE" to ThemeCopy(
            "the balance between competing needs, responsibilities, or emotions",
            "proportion and steady adjustment",
            "make one adjustment that gives both priorities a fair place"
        ),
        "BOUNDARY" to ThemeCopy(
            "the limit that protects your time, energy, or self-respect",
            "clear and respectful limits",
            "state one boundary calmly and make your own follow-through clear"
        ),
        "CHANGE" to ThemeCopy(
            "a changing pattern that can no longer be handled in exactly the old way",
            "flexibility during transition",
            "keep what still supports you and change one habit that does not"
        ),
        "CLARITY" to ThemeCopy(
            "the facts, motives, or priorities that need to be seen more clearly",
            "honest observation and precise communication",
            "separate what you know from what you assume before deciding"
        ),
        "CONNECTION" to ThemeCopy(
            "a relationship or source of support that grows through mutual care",
            "warmth, cooperation, and genuine presence",
            "offer one honest gesture of care without deciding the other person's response"
        ),
        "PATIENCE" to ThemeCopy(
            "what needs time, attention, or a slower pace before it can develop well",
            "calm timing and thoughtful observation",
            "pause long enough to notice what is ready and what still needs time"
        ),
        "RECOVERY" to ThemeCopy(
            "the part of your energy or confidence that needs restoration",
            "self-compassion and a sustainable pace",
            "protect time for one restorative choice before adding another demand"
        ),
        "STABILITY" to ThemeCopy(
            "the structure or commitment that can keep the week grounded",
            "reliability and practical support",
            "strengthen one routine or agreement you can realistically maintain"
        ),
        "UNCERTAINTY" to ThemeCopy(
            "an unclear situation that should be explored rather than filled with assumptions",
            "curiosity without false certainty",
            "ask for one missing fact and leave room for the answer to change your view"
        ),
        "RELEASE" to ThemeCopy(
            "what has completed its purpose and is taking energy to hold onto",
            "acceptance and room for renewal",
            "let go of one expectation that no longer matches the present"
        ),
        "LEARNING" to ThemeCopy(
            "the lesson hidden inside recent experience, feedback, or repetition",
            "humility and willingness to improve",
            "turn one lesson into a small experiment you can review"
        ),
        "FULFILLMENT" to ThemeCopy(
            "what already feels meaningful, complete, or worthy of appreciation",
            "gratitude and recognition of enough",
            "notice one genuine success before deciding what must come next"
        ),
        "CAUTION" to ThemeCopy(
            "a risk, inconsistency, or consequence that deserves a closer look",
            "discernment and measured decisions",
            "check one important risk and name a safeguard before committing"
        )
    )

    fun weekStart(now: Calendar = Calendar.getInstance()): Calendar =
        (now.clone() as Calendar).apply {
            add(Calendar.DAY_OF_MONTH, -(get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY))
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

    fun create(cards: List<TarotCard>, zodiac: String, start: Calendar,
        translate: (String) -> String = { it }): WeeklyHoroscope? {
        val availableCards = cards.filter { it.original != null }.sortedBy { it.slug }
        if (availableCards.size < 2) return null

        val seed = "$zodiac:${start.get(Calendar.YEAR)}:${start.get(Calendar.DAY_OF_YEAR)}".hashCode()
        val firstIndex = positiveMod(seed, availableCards.size)
        var secondIndex = positiveMod(seed * 31 + 17, availableCards.size - 1)
        if (secondIndex >= firstIndex) secondIndex += 1

        val firstCard = availableCards[firstIndex]
        val secondCard = availableCards[secondIndex]
        val firstText = requireNotNull(firstCard.original).upright
        val secondText = requireNotNull(secondCard.original).upright
        val firstCopy = themes.getValue(firstText.theme)
        val secondCopy = themes.getValue(secondText.theme)
        val pairCopy = pairCopy(firstText.theme, secondText.theme, firstCopy, secondCopy, translate)

        val summary = String.format(translate("This week, %1\$s."), translate(pairCopy.summary))
        val message = String.format(translate("%1\$s sets the main focus for your week: %2\$s. %3\$s"),
            translate(firstCard.title), translate(firstCopy.focus), translate(firstText.general)) + "\n\n" +
            String.format(translate("%1\$s brings the supporting quality of %2\$s. %3\$s"),
                translate(secondCard.title), translate(secondCopy.quality), translate(secondText.general)) + "\n\n" +
            String.format(translate("Read together, %1\$s"), translate(pairCopy.connection)) + "\n\n" +
            String.format(translate("Your practical focus for the week: %1\$s"), translate(pairCopy.guidance)) + "\n\n" +
            translate("Use this message as a reflection on your choices, not as a fixed prediction.")

        return WeeklyHoroscope(firstCard, secondCard, summary, message)
    }

    private fun positiveMod(value: Int, divisor: Int): Int = (value % divisor + divisor) % divisor

    private fun pairCopy(
        firstTheme: String,
        secondTheme: String,
        firstCopy: ThemeCopy,
        secondCopy: ThemeCopy,
        translate: (String) -> String
    ): PairCopy {
        val pair = setOf(firstTheme, secondTheme)
        return when {
            firstTheme == secondTheme -> PairCopy(
                "give this repeated lesson your full attention",
                "both cards return to the same theme from different angles. The repetition makes it the central lesson of the week, but it does not require an extreme response.",
                String.format(translate("First, %1\$s. Later in the week, review the result and apply the same theme again only where it remains useful."), translate(firstCopy.action))
            )
            pair.any { it in setOf("PATIENCE", "CAUTION") } && pair.any { it in setOf("ACTION", "BEGINNING") } -> PairCopy(
                "move forward without rushing",
                "progress and timing must work together. The opportunity is real enough to explore, while the slower card keeps enthusiasm from turning into avoidable pressure or risk.",
                "Decide what is ready now, take that limited step, and wait on the part that still lacks information or preparation."
            )
            pair.containsAll(setOf("CONNECTION", "BOUNDARY")) -> PairCopy(
                "protect connection with healthy limits",
                "closeness becomes stronger when care and clear limits support each other. A meaningful bond should make room for honesty, consent, and each person's separate needs.",
                "Offer warmth directly, then name the limit that lets you stay present without resentment or overextension."
            )
            pair.containsAll(setOf("STABILITY", "CHANGE")) -> PairCopy(
                "change what is outdated while keeping your footing",
                "change does not require discarding every source of stability. One card asks you to adapt; the other helps you identify the routines, values, or support worth carrying forward.",
                "Keep one dependable anchor in place while changing one pattern that no longer fits the situation."
            )
            pair.any { it in setOf("CAUTION", "UNCERTAINTY") } && "CLARITY" in pair -> PairCopy(
                "replace assumptions with useful facts",
                "uncertainty is asking for investigation rather than fear. Clarity may not remove every unknown, but it can show which concern is real and which story has grown around missing information.",
                "Verify the most important fact first, then make only the decision that the available evidence can support."
            )
            "RECOVERY" in pair && "ACTION" in pair -> PairCopy(
                "build momentum at a sustainable pace",
                "restoration and movement are not opposites. The useful action this week is the one that respects your present capacity instead of asking you to prove that you have already recovered.",
                "Choose one manageable task, protect the rest needed to complete it well, and treat consistency as progress."
            )
            "LEARNING" in pair && "ACTION" in pair -> PairCopy(
                "act on what experience has taught you",
                "a lesson becomes valuable when it changes the way you respond. Action gives your insight a real test, while reflection keeps movement from repeating the same old pattern under a new name.",
                "Choose the clearest lesson from recent experience and use it to shape one observable action this week."
            )
            pair.containsAll(setOf("LEARNING", "CLARITY")) -> PairCopy(
                "turn experience into a clearer next step",
                "experience becomes useful when you name what it actually taught you. Honest review can separate a repeatable lesson from a one-time result or an assumption made in hindsight.",
                "Write down one lesson, identify the evidence behind it, and test it through one small adjustment this week."
            )
            pair.containsAll(setOf("BALANCE", "LEARNING")) -> PairCopy(
                "learn without overcorrecting",
                "reflection can reveal what needs adjustment without turning one difficult result into a rule for everything. The lesson is to respond proportionately, keeping what worked while changing what did not.",
                "Name one useful lesson, then make the smallest adjustment that gives it a fair and realistic place in your week."
            )
            pair.containsAll(setOf("BALANCE", "CLARITY")) -> PairCopy(
                "make a fair decision from clear information",
                "balance depends on seeing the situation accurately. Once facts, needs, and responsibilities are named, compromise becomes a deliberate choice rather than an attempt to keep everyone comfortable.",
                "List the two priorities that need room, verify the facts affecting them, and choose an adjustment you can explain honestly."
            )
            "RECOVERY" in pair && pair.any { it in setOf("CAUTION", "UNCERTAINTY") } -> PairCopy(
                "protect your recovery while uncertainty settles",
                "not every unanswered question needs immediate action. Recovery creates enough steadiness to examine concern without letting fear consume the energy needed to heal or regroup.",
                "Reduce one avoidable demand, verify the concern that matters most, and delay any irreversible choice until you have enough capacity and information."
            )
            pair.containsAll(setOf("LEARNING", "BEGINNING")) -> PairCopy(
                "begin again with greater wisdom",
                "a fresh start is strongest when it carries forward an honest lesson from the past. You do not need to repeat the old approach, but you also do not need to discard everything it taught you.",
                "Choose one lesson to keep, then design a small first step that applies it in a new way."
            )
            pair.containsAll(setOf("BALANCE", "BEGINNING")) -> PairCopy(
                "give a new direction a realistic place in your life",
                "new energy is easier to sustain when it is introduced in proportion to existing needs. The beginning deserves attention, but not at the cost of every responsibility or source of support around it.",
                "Set aside a defined amount of time or energy for the new direction, then review what needs adjusting before expanding it."
            )
            pair.containsAll(setOf("PATIENCE", "CLARITY")) -> PairCopy(
                "wait long enough to see clearly",
                "clarity is more likely to emerge from attentive observation than from forcing an early answer. Patience here is active: it gathers context, notices patterns, and prepares a better question.",
                "Give the situation a clear review point, and use the time before it to gather the one fact that would most improve your decision."
            )
            pair.containsAll(setOf("ACTION", "FULFILLMENT")) -> PairCopy(
                "build on what is already working",
                "satisfaction can become a stable foundation rather than a reason to stop growing. Action is most useful when it extends something meaningful instead of chasing movement only for its own sake.",
                "Recognize one result worth keeping, then take the next action that deepens it without diminishing what you have already achieved."
            )
            pair.containsAll(setOf("BEGINNING", "ACTION")) -> PairCopy(
                "turn a promising start into focused movement",
                "possibility becomes progress when curiosity is paired with direction. The cards favour beginning before every detail is perfect, while still choosing an action clear enough to learn from.",
                "Define the smallest useful result for this week and take the first reversible action that moves you toward it."
            )
            pair.containsAll(setOf("FULFILLMENT", "RELEASE")) -> PairCopy(
                "make room for what matters now",
                "completion can be both satisfying and freeing. Appreciating what was gained makes it easier to release an outdated goal without treating the change as failure.",
                "Acknowledge what has been completed, keep its lesson, and stop spending energy on the expectation that no longer serves you."
            )
            pair.containsAll(setOf("BEGINNING", "RELEASE")) -> PairCopy(
                "create space before beginning again",
                "a genuine beginning needs room to grow. Releasing an old expectation, role, or method prevents the new direction from becoming only a renamed version of the past.",
                "Choose what you are ready to stop carrying, then give the freed time or energy to one modest first step."
            )
            pair.containsAll(setOf("CONNECTION", "RECOVERY")) -> PairCopy(
                "let support strengthen recovery",
                "connection can make recovery less isolating, while recovery clarifies which relationships feel mutual and safe. Support works best when it respects capacity rather than creating another obligation.",
                "Ask for or accept one specific form of support, and be honest about the pace you can sustain."
            )
            pair.containsAll(setOf("BALANCE", "ACTION")) -> PairCopy(
                "act without neglecting the rest of your life",
                "focused effort will help only if it remains proportionate. The goal is not perfect balance every day, but deliberate movement that does not quietly consume every other priority.",
                "Choose one meaningful action and set a clear limit around the time, energy, or attention it receives."
            )
            pair.containsAll(setOf("STABILITY", "BOUNDARY")) -> PairCopy(
                "build security through clear expectations",
                "dependability grows when responsibilities and limits are understood. Structure should protect what matters without becoming rigid or controlling.",
                "Clarify one expectation, agree on what follow-through looks like, and leave room to revise the arrangement if circumstances change."
            )
            else -> PairCopy(
                "turn both messages into one balanced next step",
                "the first card identifies the part of life asking for attention, while the second shows the quality that can keep your response balanced. Neither message needs to cancel the other.",
                String.format(translate("First, %1\$s. Then, %2\$s. Let the second step support rather than compete with the first."), translate(firstCopy.action), translate(secondCopy.action))
            )
        }
    }
}

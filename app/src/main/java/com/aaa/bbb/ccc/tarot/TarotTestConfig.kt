package com.aaa.bbb.ccc.tarot

/** Tester-only switches. Set both flags to false before publishing. */
object TarotTestConfig {
    // true: reveal all 78 card faces in both Scroll and Classic selection modes.
    // false: keep cards face down until selected, as in the normal reading flow.
    // Does not select cards, change reversal chance, bypass locks or alter history.
    const val REVEAL_ALL_CARDS = true

    // true: allow repeated readings, including Daily/Weekly, without waiting or ads.
    // false: restore the normal calendar-day and 23-hour rules using saved history.
    const val BYPASS_READING_LIMITS = true

    internal fun showFace(isSelected: Boolean, revealAll: Boolean = REVEAL_ALL_CARDS): Boolean =
        isSelected || revealAll

    internal fun remaining(
        spread: Spread, lastDraw: Long?, now: Long,
        bypassLimits: Boolean = BYPASS_READING_LIMITS
    ): Long = if (bypassLimits) 0L else TarotRules.remaining(spread, lastDraw, now)

    // Shared by opening a spread and completing a selection, so Daily/Weekly
    // cannot accidentally reopen an old result after the tester picks new cards.
    internal fun reuseDailyReading(
        spread: Spread, lastDraw: Long?, now: Long,
        bypassLimits: Boolean = BYPASS_READING_LIMITS
    ): Boolean = !bypassLimits && spread.rule == ReadingRule.DAILY &&
        lastDraw != null && TarotRules.sameDay(lastDraw, now)
}

package com.tarot.card.meaning.tarot

import java.util.Locale
import kotlin.math.abs

/** Fixed calendar ranges shown in this app, not astronomical birth-chart calculations. */
data class ZodiacDateRange(
    val name: String,
    val startMonth: Int,
    val startDay: Int,
    val endMonth: Int,
    val endDay: Int
) {
    val label: String get() = String.format(Locale.US, "%02d/%02d - %02d/%02d", startDay, startMonth, endDay, endMonth)

    fun contains(date: BirthdayDate): Boolean {
        val value = date.month * 100 + date.day
        val start = startMonth * 100 + startDay
        val end = endMonth * 100 + endDay
        return if (start <= end) value in start..end else value >= start || value <= end
    }
}

object ZodiacDates {
    val ranges = listOf(
        ZodiacDateRange("Aries", 3, 21, 4, 19),
        ZodiacDateRange("Taurus", 4, 20, 5, 20),
        ZodiacDateRange("Gemini", 5, 21, 6, 20),
        ZodiacDateRange("Cancer", 6, 21, 7, 22),
        ZodiacDateRange("Leo", 7, 23, 8, 22),
        ZodiacDateRange("Virgo", 8, 23, 9, 22),
        ZodiacDateRange("Libra", 9, 23, 10, 22),
        ZodiacDateRange("Scorpio", 10, 23, 11, 21),
        ZodiacDateRange("Sagittarius", 11, 22, 12, 21),
        ZodiacDateRange("Capricorn", 12, 22, 1, 19),
        ZodiacDateRange("Aquarius", 1, 20, 2, 18),
        ZodiacDateRange("Pisces", 2, 19, 3, 20)
    )

    fun range(sign: String): ZodiacDateRange = ranges.first { it.name == sign }

    fun signFor(birthday: String?): String? {
        val date = ProfileValidator.parseBirthday(birthday) ?: return null
        return ranges.firstOrNull { it.contains(date) }?.name
    }

    fun allowedDays(sign: String, year: Int, month: Int, today: BirthdayDate): IntRange? {
        if (year !in ProfileValidator.MIN_BIRTH_YEAR..today.year || month !in 1..12) return null
        val rule = range(sign)
        val days = (1..ProfileValidator.daysInMonth(year, month)).filter { day ->
            rule.contains(BirthdayDate(year, month, day)) &&
                (year < today.year || month < today.month || (month == today.month && day <= today.day))
        }
        return if (days.isEmpty()) null else days.first()..days.last()
    }

    fun allowedMonths(sign: String, year: Int, today: BirthdayDate): List<Int> =
        (1..12).filter { allowedDays(sign, year, it, today) != null }

    fun lastBirthYear(sign: String, today: BirthdayDate): Int =
        if (allowedMonths(sign, today.year, today).isEmpty()) today.year - 1 else today.year

    /** Initial/picker adjustments only; never silently edits a saved birthday. */
    fun constrain(date: BirthdayDate, sign: String, today: BirthdayDate): BirthdayDate {
        val year = date.year.coerceIn(ProfileValidator.MIN_BIRTH_YEAR, lastBirthYear(sign, today))
        val month = allowedMonths(sign, year, today).minBy { abs(it - date.month) }
        val days = requireNotNull(allowedDays(sign, year, month, today))
        return BirthdayDate(year, month, date.day.coerceIn(days.first, days.last))
    }
}

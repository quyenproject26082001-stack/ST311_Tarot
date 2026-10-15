package com.aaa.bbb.ccc.tarot

import org.junit.Assert.*
import org.junit.Test
import java.util.Locale

class ZodiacDatesTest {
    private val today = BirthdayDate(2026, 10, 15)
    private fun stored(date: BirthdayDate) = String.format(Locale.US, "%04d-%02d-%02d", date.year, date.month, date.day)

    @Test fun uiLabelsAndValidationUseTheSameTwelveRanges() {
        assertEquals(12, ZodiacDates.ranges.map { it.name }.distinct().size)
        assertEquals(ProfileValidator.zodiacNames, ZodiacDates.ranges.map { it.name }.toSet())
        zodiacSigns.forEach { assertEquals(ZodiacDates.range(it.name).label, it.dates) }
        assertEquals("22/12 - 19/01", ZodiacDates.range("Capricorn").label)
    }

    @Test fun everyDayBelongsToExactlyOneSignIncludingLeapDay() {
        listOf(1900, 2000, 2025, 2026).forEach { year ->
            (1..12).forEach { month ->
                (1..ProfileValidator.daysInMonth(year, month)).forEach { day ->
                    val date = BirthdayDate(year, month, day)
                    val matches = ZodiacDates.ranges.filter { it.contains(date) }
                    assertEquals(stored(date), 1, matches.size)
                    assertEquals(matches.single().name, ZodiacDates.signFor(stored(date)))
                }
            }
        }
    }

    @Test fun everyDisplayedStartAndEndDateIsIncluded() {
        ZodiacDates.ranges.forEach {
            assertEquals(it.name, ZodiacDates.signFor(stored(BirthdayDate(2024, it.startMonth, it.startDay))))
            assertEquals(it.name, ZodiacDates.signFor(stored(BirthdayDate(2024, it.endMonth, it.endDay))))
        }
        assertEquals("Pisces", ZodiacDates.signFor("2024-03-20"))
        assertEquals("Aries", ZodiacDates.signFor("2024-03-21"))
        assertEquals("Libra", ZodiacDates.signFor("2024-10-22"))
        assertEquals("Scorpio", ZodiacDates.signFor("2024-10-23"))
    }

    @Test fun capricornWrapsTheYearWithoutIncludingOtherMonths() {
        assertEquals(listOf(1, 12), ZodiacDates.allowedMonths("Capricorn", 2000, today))
        assertEquals(1..19, ZodiacDates.allowedDays("Capricorn", 2000, 1, today))
        assertEquals(22..31, ZodiacDates.allowedDays("Capricorn", 2000, 12, today))
        assertNull(ZodiacDates.allowedDays("Capricorn", 2000, 6, today))
        assertEquals("Aquarius", ZodiacDates.signFor("2000-01-20"))
        assertEquals("Sagittarius", ZodiacDates.signFor("2000-12-21"))
    }

    @Test fun piscesRespectsCalendarLeapRules() {
        assertEquals(19..28, ZodiacDates.allowedDays("Pisces", 1900, 2, today))
        assertEquals(19..29, ZodiacDates.allowedDays("Pisces", 2000, 2, today))
        assertEquals(1..20, ZodiacDates.allowedDays("Pisces", 2000, 3, today))
        assertEquals("Pisces", ZodiacDates.signFor("2000-02-29"))
        assertNull(ZodiacDates.signFor("1900-02-29"))
    }

    @Test fun currentYearNeverOffersAFutureBirthday() {
        assertEquals(listOf(9, 10), ZodiacDates.allowedMonths("Libra", 2026, today))
        assertEquals(1..15, ZodiacDates.allowedDays("Libra", 2026, 10, today))
        assertEquals(2025, ZodiacDates.lastBirthYear("Scorpio", today))
        assertEquals(emptyList<Int>(), ZodiacDates.allowedMonths("Scorpio", 2026, today))
        assertEquals(listOf(1), ZodiacDates.allowedMonths("Capricorn", 2026, today))
        assertNull(ZodiacDates.allowedDays("Capricorn", 2026, 12, today))
    }

    @Test fun todayAtTheStartOfASignIsSelectable() {
        val firstAriesDay = BirthdayDate(2026, 3, 21)
        assertEquals(2026, ZodiacDates.lastBirthYear("Aries", firstAriesDay))
        assertEquals(listOf(3), ZodiacDates.allowedMonths("Aries", 2026, firstAriesDay))
        assertEquals(21..21, ZodiacDates.allowedDays("Aries", 2026, 3, firstAriesDay))
        assertEquals(2025, ZodiacDates.lastBirthYear("Aries", firstAriesDay.copy(day = 20)))
    }

    @Test fun januaryCapricornOnlyOffersTheElapsedPartOfJanuary() {
        val january = BirthdayDate(2026, 1, 5)
        assertEquals(listOf(1), ZodiacDates.allowedMonths("Capricorn", 2026, january))
        assertEquals(1..5, ZodiacDates.allowedDays("Capricorn", 2026, 1, january))
        assertEquals(2025, ZodiacDates.lastBirthYear("Aquarius", january))
    }

    @Test fun constrainingAnInitialDatePreservesYearWherePossibleAndAlwaysMatchesTheSign() {
        ZodiacDates.ranges.forEach { rule ->
            listOf(BirthdayDate(1994, 11, 14), BirthdayDate(1900, 2, 29), BirthdayDate(2099, 12, 31)).forEach { initial ->
                val result = ZodiacDates.constrain(initial, rule.name, today)
                assertEquals(rule.name, ZodiacDates.signFor(stored(result)))
                assertTrue(result.year in ProfileValidator.MIN_BIRTH_YEAR..ZodiacDates.lastBirthYear(rule.name, today))
                assertTrue(result.day in requireNotNull(ZodiacDates.allowedDays(rule.name, result.year, result.month, today)))
            }
        }
        assertEquals(1994, ZodiacDates.constrain(BirthdayDate(1994, 11, 14), "Pisces", today).year)
        assertEquals(BirthdayDate(1900, 2, 28), ZodiacDates.constrain(BirthdayDate(1900, 2, 29), "Pisces", today))
    }

    @Test fun invalidOrOutOfBoundsCalendarInputsAreNotOffered() {
        listOf(null, "", "2000-02-30", "2000-13-01").forEach { assertNull(ZodiacDates.signFor(it)) }
        assertNull(ZodiacDates.allowedDays("Pisces", 1899, 2, today))
        assertNull(ZodiacDates.allowedDays("Pisces", 2027, 2, today))
        assertNull(ZodiacDates.allowedDays("Pisces", 2000, 0, today))
        assertNull(ZodiacDates.allowedDays("Pisces", 2000, 13, today))
    }
}

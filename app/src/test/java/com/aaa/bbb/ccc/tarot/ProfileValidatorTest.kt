package com.tarot.card.meaning.tarot

import com.google.gson.Gson
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.Locale
import java.util.TimeZone

class ProfileValidatorTest {
    private val utc = TimeZone.getTimeZone("UTC")
    private val now = GregorianCalendar(utc, Locale.US).apply {
        clear()
        set(2026, Calendar.OCTOBER, 14, 18, 0, 0)
    }.timeInMillis

    @Test fun emptyAndUnicodeWhitespaceNamesAreRejected() {
        listOf(null, "", "   ", "\u00A0\u2003\u3000").forEach {
            assertEquals(ProfileValidationError.NAME_REQUIRED, ProfileValidator.nameError(it))
        }
    }

    @Test fun nameIsTrimmedAndVietnameseCombiningMarksAreNormalized() {
        assertEquals("Lê Thế Quyền", ProfileValidator.normalizeName("\u00A0 Lê Thế Quyền \u3000"))
        assertEquals("é", ProfileValidator.normalizeName("e\u0301"))
        assertNull(ProfileValidator.nameError("Lê Thế Quyền"))
    }

    @Test fun namesKeepInternationalLettersAndPunctuation() {
        listOf("O’Connor", "Anne-Marie", "王小明", "محمد", "ST311_Demo", "🌙").forEach {
            assertNull(it, ProfileValidator.nameError(it))
        }
    }

    @Test fun lengthLimitMatchesAndroidUtf16LengthFilter() {
        assertNull(ProfileValidator.nameError("a".repeat(24)))
        assertEquals(ProfileValidationError.NAME_TOO_LONG, ProfileValidator.nameError("a".repeat(25)))
        assertNull(ProfileValidator.nameError("😀".repeat(12)))
        assertEquals(ProfileValidationError.NAME_TOO_LONG, ProfileValidator.nameError("😀".repeat(13)))
    }

    @Test fun zeroWidthSpacesBomAndBidiFormattingAreRejectedEvenInsideNames() {
        listOf("\u200B", "\uFEFF", "A\u200BB", "A\u202EB", "A\u2066B", "A\u00ADB").forEach {
            assertEquals(it, ProfileValidationError.NAME_UNSUPPORTED_CHARACTER, ProfileValidator.nameError(it))
        }
    }

    @Test fun invisibleFillersCannotMasqueradeAsNames() {
        listOf("\u034F", "\u115F", "\u1160", "\u2800", "\u3164", "\uFFA0").forEach {
            assertEquals(ProfileValidationError.NAME_UNSUPPORTED_CHARACTER, ProfileValidator.nameError(it))
        }
    }

    @Test fun isolatedMarksAndJoinersAreNotVisibleNames() {
        listOf("\u0301", "\uFE0F", "\u200D", "\u200C", " \u200D\uFE0F ").forEach {
            assertEquals(ProfileValidationError.NAME_REQUIRED, ProfileValidator.nameError(it))
        }
    }

    @Test fun legitimateJoinersAndJoinedEmojiArePreserved() {
        assertNull(ProfileValidator.nameError("علی\u200Cرضا"))
        assertNull(ProfileValidator.nameError("👩\u200D💻"))
        assertEquals("👩\u200D💻", ProfileValidator.normalizeName(" 👩\u200D💻 "))
    }

    @Test fun controlCharactersAndBrokenSurrogatePairsAreRejected() {
        listOf("A\nB", "A\tB", "A\u0000B", "A\uD83DB", "A\uDE00B").forEach {
            assertEquals(ProfileValidationError.NAME_UNSUPPORTED_CHARACTER, ProfileValidator.nameError(it))
        }
    }

    @Test fun birthdayMustUseExactStoredFormat() {
        listOf(null, "", "2026-1-01", "14/10/2026", "2026-10-14abc", " 2026-10-14", "0000-01-01").forEach {
            assertFalse(it, ProfileValidator.isValidBirthday(it, now, utc))
        }
    }

    @Test fun impossibleCalendarDatesAreRejectedRatherThanRolledIntoNextMonth() {
        listOf("2026-02-30", "2026-04-31", "2026-00-01", "2026-13-01", "2026-01-00", "2026-01-32").forEach {
            assertFalse(it, ProfileValidator.isValidBirthday(it, now, utc))
        }
    }

    @Test fun leapYearsIncludeCenturyExceptions() {
        assertFalse(ProfileValidator.isValidBirthday("1900-02-29", now, utc))
        assertTrue(ProfileValidator.isValidBirthday("2000-02-29", now, utc))
        assertTrue(ProfileValidator.isValidBirthday("2024-02-29", now, utc))
        assertFalse(ProfileValidator.isValidBirthday("2025-02-29", now, utc))
    }

    @Test fun birthdayYearLowerBoundIsInclusive() {
        assertFalse(ProfileValidator.isValidBirthday("1899-12-31", now, utc))
        assertTrue(ProfileValidator.isValidBirthday("1900-01-01", now, utc))
    }

    @Test fun todayIsAllowedAndTomorrowIsRejected() {
        assertTrue(ProfileValidator.isValidBirthday("2026-10-14", now, utc))
        assertFalse(ProfileValidator.isValidBirthday("2026-10-15", now, utc))
        assertFalse(ProfileValidator.isValidBirthday("2027-01-01", now, utc))
    }

    @Test fun futureCheckUsesLocalCalendarDay() {
        val bangkok = TimeZone.getTimeZone("Asia/Bangkok")
        assertEquals(BirthdayDate(2026, 10, 15), ProfileValidator.today(now, bangkok))
        assertTrue(ProfileValidator.isValidBirthday("2026-10-15", now, bangkok))
        assertFalse(ProfileValidator.isValidBirthday("2026-10-15", now, utc))
    }

    @Test fun wheelMonthLengthsUseTheSameLeapYearRules() {
        assertEquals(28, ProfileValidator.daysInMonth(1900, 2))
        assertEquals(29, ProfileValidator.daysInMonth(2000, 2))
        assertEquals(30, ProfileValidator.daysInMonth(2026, 4))
        assertEquals(31, ProfileValidator.daysInMonth(2026, 1))
    }

    @Test fun whitelistMatchesAllTwelvePickerOptions() {
        assertEquals(12, ProfileValidator.zodiacNames.size)
        assertEquals(zodiacSigns.map { it.name }.toSet(), ProfileValidator.zodiacNames)
        listOf(null, "", "pisces", "Unknown", "Aries ").forEach { assertFalse(ProfileValidator.isValidZodiac(it)) }
        ProfileValidator.zodiacNames.forEach { assertTrue(ProfileValidator.isValidZodiac(it)) }
    }

    @Test fun birthdayMustMatchUsersChosenZodiac() {
        assertEquals(ProfileValidationError.BIRTHDAY_ZODIAC_MISMATCH,
            ProfileValidator.validate(Profile("Quyền", "1994-11-14", "Pisces"), now, utc))
        assertNull(ProfileValidator.validate(Profile("Quyền", "1994-11-14", "Scorpio"), now, utc))
    }

    @Test fun wholeProfileRejectsEachInvalidField() {
        assertEquals(ProfileValidationError.NAME_REQUIRED, ProfileValidator.validate(Profile(" ", "2000-01-01", "Aries"), now, utc))
        assertEquals(ProfileValidationError.INVALID_BIRTHDAY, ProfileValidator.validate(Profile("Quyền", "2026-02-30", "Aries"), now, utc))
        assertEquals(ProfileValidationError.INVALID_ZODIAC, ProfileValidator.validate(Profile("Quyền", "2000-01-01", "Unknown"), now, utc))
    }

    @Test fun missingJsonFieldsAreRejectedWithoutNullPointerExceptions() {
        listOf("{}", "{\"name\":\"Quyền\"}", "{\"name\":\"Quyền\",\"birthday\":\"2000-01-01\"}",
            "{\"name\":null,\"birthday\":null,\"zodiac\":null}").forEach {
            val profile = Gson().fromJson(it, Profile::class.java)
            assertNotNull(ProfileValidator.validate(profile, now, utc))
        }
    }
}

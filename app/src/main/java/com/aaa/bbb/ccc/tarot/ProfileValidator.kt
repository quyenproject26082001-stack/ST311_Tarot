package com.aaa.bbb.ccc.tarot

import java.text.Normalizer
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.Locale
import java.util.TimeZone

enum class ProfileValidationError {
    NAME_REQUIRED, NAME_TOO_LONG, NAME_UNSUPPORTED_CHARACTER, INVALID_BIRTHDAY, INVALID_ZODIAC,
    BIRTHDAY_ZODIAC_MISMATCH
}

data class BirthdayDate(val year: Int, val month: Int, val day: Int)

/** Shared, Android-independent rules for onboarding, Settings and persistence. */
object ProfileValidator {
    // The XML field uses the same UTF-16 length limit as Android's LengthFilter.
    const val MAX_NAME_LENGTH = 24
    const val MIN_BIRTH_YEAR = 1900

    val zodiacNames = ZodiacDates.ranges.map { it.name }.toSet()

    fun normalizeName(raw: String?): String = Normalizer.normalize(
        raw.orEmpty().trim { it.isWhitespace() || Character.isSpaceChar(it) },
        Normalizer.Form.NFC
    )

    fun nameError(raw: String?): ProfileValidationError? {
        val name = normalizeName(raw)
        if (name.isEmpty()) return ProfileValidationError.NAME_REQUIRED
        if (name.length > MAX_NAME_LENGTH) return ProfileValidationError.NAME_TOO_LONG

        var hasVisibleCharacter = false
        var index = 0
        while (index < name.length) {
            val point = Character.codePointAt(name, index)
            val type = Character.getType(point)
            // Joiners are needed for legitimate names and joined emoji. They do
            // not count as visible characters on their own. Other formatting
            // controls (zero-width space, BOM, bidi overrides...) are rejected.
            val joiner = point == 0x200C || point == 0x200D
            val invisibleFiller = point in setOf(0x034F, 0x115F, 0x1160, 0x2800, 0x3164, 0xFFA0)
            if (point in 0xD800..0xDFFF || Character.isISOControl(point) || invisibleFiller ||
                (type == Character.FORMAT.toInt() && !joiner)) {
                return ProfileValidationError.NAME_UNSUPPORTED_CHARACTER
            }
            val mark = type == Character.NON_SPACING_MARK.toInt() ||
                type == Character.COMBINING_SPACING_MARK.toInt() || type == Character.ENCLOSING_MARK.toInt()
            if (!Character.isWhitespace(point) && !Character.isSpaceChar(point) && !mark && !joiner) {
                hasVisibleCharacter = true
            }
            index += Character.charCount(point)
        }
        return if (hasVisibleCharacter) null else ProfileValidationError.NAME_REQUIRED
    }

    fun today(nowMillis: Long = System.currentTimeMillis(), zone: TimeZone = TimeZone.getDefault()): BirthdayDate {
        val date = GregorianCalendar(zone, Locale.US).apply { timeInMillis = nowMillis }
        return BirthdayDate(date.get(Calendar.YEAR), date.get(Calendar.MONTH) + 1, date.get(Calendar.DAY_OF_MONTH))
    }

    fun daysInMonth(year: Int, month: Int): Int = GregorianCalendar(TimeZone.getTimeZone("UTC"), Locale.US).apply {
        clear()
        isLenient = false
        set(year, month - 1, 1)
    }.getActualMaximum(Calendar.DAY_OF_MONTH)

    fun parseBirthday(raw: String?): BirthdayDate? {
        if (raw == null || !Regex("[0-9]{4}-[0-9]{2}-[0-9]{2}").matches(raw)) return null
        val year = raw.substring(0, 4).toInt()
        val month = raw.substring(5, 7).toInt()
        val day = raw.substring(8, 10).toInt()
        if (year < 1 || month !in 1..12 || day !in 1..daysInMonth(year, month)) return null
        return BirthdayDate(year, month, day)
    }

    fun isValidBirthday(raw: String?, nowMillis: Long = System.currentTimeMillis(), zone: TimeZone = TimeZone.getDefault()): Boolean {
        val birthday = parseBirthday(raw) ?: return false
        if (birthday.year < MIN_BIRTH_YEAR) return false
        val today = today(nowMillis, zone)
        fun BirthdayDate.sortKey() = year * 10_000 + month * 100 + day
        return birthday.sortKey() <= today.sortKey()
    }

    fun isValidZodiac(value: String?): Boolean = value in zodiacNames

    fun validate(profile: Profile, nowMillis: Long = System.currentTimeMillis(), zone: TimeZone = TimeZone.getDefault()): ProfileValidationError? {
        nameError(profile.name)?.let { return it }
        if (!isValidBirthday(profile.birthday, nowMillis, zone)) return ProfileValidationError.INVALID_BIRTHDAY
        if (!isValidZodiac(profile.zodiac)) return ProfileValidationError.INVALID_ZODIAC
        if (ZodiacDates.signFor(profile.birthday) != profile.zodiac) return ProfileValidationError.BIRTHDAY_ZODIAC_MISMATCH
        return null
    }
}

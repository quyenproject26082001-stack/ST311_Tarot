package com.tarot.card.meaning.tarot

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

/** Uses its own UUID-named preferences file; never clears the app's real profile. */
@RunWith(AndroidJUnit4::class)
class ProfilePreferencesTest {
    private lateinit var base: Context
    private lateinit var raw: SharedPreferences
    private lateinit var preferences: TarotPreferences
    private lateinit var fileName: String

    @Before fun setUp() {
        base = InstrumentationRegistry.getInstrumentation().targetContext
        fileName = "tarot_validation_test_${UUID.randomUUID()}"
        raw = base.getSharedPreferences(fileName, Context.MODE_PRIVATE)
        val isolatedContext = object : ContextWrapper(base) {
            override fun getApplicationContext(): Context = this
            override fun getSharedPreferences(name: String, mode: Int): SharedPreferences = raw
        }
        preferences = TarotPreferences(isolatedContext)
    }

    @After fun cleanUp() {
        base.deleteSharedPreferences(fileName)
    }

    @Test fun validNameIsNormalizedBeforePersistence() {
        assertNull(preferences.saveProfile(Profile("  Lê Thế Quyền  ", "2000-01-01", "Capricorn")))
        assertEquals("Lê Thế Quyền", preferences.profile?.name)
        assertTrue(raw.getString("profile", "")!!.contains("Lê Thế Quyền"))
    }

    @Test fun invalidSaveNeverOverwritesTheExistingProfile() {
        val valid = Profile("Quyền", "2000-01-01", "Capricorn")
        assertNull(preferences.saveProfile(valid))
        val before = raw.getString("profile", null)
        listOf(valid.copy(name = "\u200B"), valid.copy(name = "a".repeat(25)),
            valid.copy(birthday = "2026-02-30"), valid.copy(zodiac = "Unknown"),
            valid.copy(zodiac = "Pisces")).forEach {
            assertNotNull(preferences.saveProfile(it))
            assertEquals(before, raw.getString("profile", null))
            assertEquals(valid, preferences.profile)
        }
    }

    @Test fun malformedOrIncompleteSavedJsonReturnsNullWithoutErasingIt() {
        listOf("not json", "[]", "null", "{}", "{\"name\":null}",
            "{\"name\":\"Quyền\",\"birthday\":\"2000-01-01\",\"zodiac\":\"Unknown\"}").forEach {
            raw.edit().putString("profile", it).commit()
            assertNull(preferences.profile)
            assertEquals(it, raw.getString("profile", null))
        }
    }

    @Test fun wrongPreferenceTypeDoesNotCrashOrEraseTheRecord() {
        raw.edit().putInt("profile", 42).commit()
        assertNull(preferences.profile)
        assertEquals(42, raw.getInt("profile", 0))
    }

    @Test fun existingValidProfileIsReadableWithoutAMigration() {
        val legacy = "{\"name\":\"  Quyền  \",\"birthday\":\"2000-01-01\",\"zodiac\":\"Capricorn\"}"
        raw.edit().putString("profile", legacy).commit()
        assertEquals(Profile("Quyền", "2000-01-01", "Capricorn"), preferences.profile)
        assertEquals(legacy, raw.getString("profile", null))
    }

    @Test fun oldMismatchedProfileKeepsItsBirthdayAndDerivesTheCorrectSignWithoutErasingRawData() {
        val legacy = "{\"name\":\"Quyền\",\"birthday\":\"1994-11-14\",\"zodiac\":\"Pisces\"}"
        raw.edit().putString("profile", legacy).commit()
        val corrected = preferences.profile
        assertEquals(Profile("Quyền", "1994-11-14", "Scorpio"), corrected)
        assertEquals(legacy, raw.getString("profile", null))
        assertNull(preferences.saveProfile(requireNotNull(corrected)))
        assertTrue(raw.getString("profile", "")!!.contains("Scorpio"))
    }

    @Test fun reverseProbabilityStaysWithinZeroAndOneHundred() {
        preferences.reverseChance = -1
        assertEquals(0, preferences.reverseChance)
        preferences.reverseChance = 101
        assertEquals(100, preferences.reverseChance)
    }
}

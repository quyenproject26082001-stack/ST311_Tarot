package com.tarot.card.meaning.tarot

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class TarotTestConfigTest {
    @Test fun testerModeShowsBothUnselectedAndSelectedFaces() {
        assertTrue(TarotTestConfig.showFace(isSelected = false, revealAll = true))
        assertTrue(TarotTestConfig.showFace(isSelected = true, revealAll = true))
    }

    @Test fun normalModeOnlyShowsSelectedFaces() {
        assertFalse(TarotTestConfig.showFace(isSelected = false, revealAll = false))
        assertTrue(TarotTestConfig.showFace(isSelected = true, revealAll = false))
    }

    @Test fun defaultUsesTheSingleConfigSwitch() {
        assertTrue(TarotTestConfig.showFace(isSelected = true))
        assertEquals(TarotTestConfig.REVEAL_ALL_CARDS,
            TarotTestConfig.showFace(isSelected = false))
    }

    @Test fun bypassRemovesEveryCooldownAndNeverReusesDailyResults() {
        val now = System.currentTimeMillis()
        Spread.entries.forEach { spread ->
            assertEquals(0L, TarotTestConfig.remaining(spread, now, now, bypassLimits = true))
            assertEquals(0L, TarotTestConfig.remaining(spread, null, now, bypassLimits = true))
            assertFalse(TarotTestConfig.reuseDailyReading(spread, now, now, bypassLimits = true))
        }
    }

    @Test fun disablingBypassRestoresAllNormalCooldownRulesAndExpiry() {
        val now = System.currentTimeMillis()
        Spread.entries.forEach { spread ->
            val expected = if (spread.rule == ReadingRule.COOLDOWN) TarotRules.COOLDOWN_MS else 0L
            assertEquals(expected, TarotTestConfig.remaining(spread, now, now, bypassLimits = false))
            assertEquals(0L, TarotTestConfig.remaining(spread, now, now + TarotRules.COOLDOWN_MS, bypassLimits = false))
            assertEquals(0L, TarotTestConfig.remaining(spread, null, now, bypassLimits = false))
        }
    }

    @Test fun disablingBypassRestoresDailyAndWeeklyReuseOnlyOnTheSameDay() {
        val now = System.currentTimeMillis()
        val nextDay = Calendar.getInstance().apply { timeInMillis = now; add(Calendar.DAY_OF_YEAR, 1) }.timeInMillis
        Spread.entries.forEach { spread ->
            assertEquals(spread.rule == ReadingRule.DAILY,
                TarotTestConfig.reuseDailyReading(spread, now, now, bypassLimits = false))
            assertFalse(TarotTestConfig.reuseDailyReading(spread, now, nextDay, bypassLimits = false))
            assertFalse(TarotTestConfig.reuseDailyReading(spread, null, now, bypassLimits = false))
        }
    }

    @Test fun defaultLimitPolicyUsesTheSeparateBypassSwitch() {
        val now = System.currentTimeMillis()
        assertEquals(if (TarotTestConfig.BYPASS_READING_LIMITS) 0L else TarotRules.COOLDOWN_MS,
            TarotTestConfig.remaining(Spread.UNIVERSE, now, now))
        assertEquals(!TarotTestConfig.BYPASS_READING_LIMITS,
            TarotTestConfig.reuseDailyReading(Spread.DAILY, now, now))
    }
}

package com.aaa.bbb.ccc.tarot

import android.widget.NumberPicker
import androidx.appcompat.view.ContextThemeWrapper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.aaa.bbb.ccc.R
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.text.DateFormatSymbols
import java.util.Locale

/** Checks actual NumberPicker bounds without writing profile or reading data. */
@RunWith(AndroidJUnit4::class)
class BirthdayWheelTest {
    private fun checkOnMain(block: (TarotUi) -> Unit) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            val context = ContextThemeWrapper(instrumentation.targetContext, R.style.TarotThemeLight)
            block(TarotUi(context))
        }
    }

    @Test fun piscesOnlyOffersFebruaryAndMarchWithTheirOwnDayLimits() = checkOnMain { ui ->
        val february = BirthdayWheel(ui, "2000-02-29", "Pisces")
        val march = BirthdayWheel(ui, "2000-03-20", "Pisces")
        val names = DateFormatSymbols(Locale.getDefault()).months
        assertArrayEquals(arrayOf(names[1], names[2]), february.findViewById<NumberPicker>(R.id.month).displayedValues)
        assertEquals(19, february.findViewById<NumberPicker>(R.id.day).minValue)
        assertEquals(29, february.findViewById<NumberPicker>(R.id.day).maxValue)
        assertEquals(1, march.findViewById<NumberPicker>(R.id.day).minValue)
        assertEquals(20, march.findViewById<NumberPicker>(R.id.day).maxValue)
        assertEquals("2000-02-29", february.value())
        assertTrue(february.valid())
    }

    @Test fun capricornPickerMapsJanuaryAndDecemberToRealCalendarMonths() = checkOnMain { ui ->
        val january = BirthdayWheel(ui, "2000-01-19", "Capricorn")
        val december = BirthdayWheel(ui, "2000-12-22", "Capricorn")
        val names = DateFormatSymbols(Locale.getDefault()).months
        assertArrayEquals(arrayOf(names[0], names[11]), january.findViewById<NumberPicker>(R.id.month).displayedValues)
        assertEquals(19, january.findViewById<NumberPicker>(R.id.day).maxValue)
        assertEquals(22, december.findViewById<NumberPicker>(R.id.day).minValue)
        assertEquals("2000-01-19", january.value())
        assertEquals("2000-12-22", december.value())
    }

    @Test fun commonYearFebruaryDoesNotOfferTheTwentyNinth() = checkOnMain { ui ->
        val wheel = BirthdayWheel(ui, "1900-02-28", "Pisces")
        assertEquals(28, wheel.findViewById<NumberPicker>(R.id.day).maxValue)
        assertTrue(wheel.valid())
    }

    @Test fun mismatchedInitialDateIsConstrainedForAllTwelveSigns() = checkOnMain { ui ->
        ZodiacDates.ranges.forEach { rule ->
            val wheel = BirthdayWheel(ui, "1994-11-14", rule.name)
            assertTrue(rule.name, wheel.valid())
            assertEquals(rule.name, ZodiacDates.signFor(wheel.value()))
            assertTrue(wheel.value().startsWith("1994-"))
        }
    }

    @Test fun futureInitialDateCannotMakeTheWheelOfferAFutureYear() = checkOnMain { ui ->
        val today = ProfileValidator.today()
        ZodiacDates.ranges.forEach { rule ->
            val wheel = BirthdayWheel(ui, "2099-12-31", rule.name)
            assertTrue(rule.name, wheel.valid())
            assertEquals(ZodiacDates.lastBirthYear(rule.name, today), wheel.findViewById<NumberPicker>(R.id.year).maxValue)
        }
    }
}

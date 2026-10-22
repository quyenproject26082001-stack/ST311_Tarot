package com.tarot.card.meaning.tarot

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.LinearLayout
import androidx.core.content.res.ResourcesCompat
import com.tarot.card.meaning.R
import com.tarot.card.meaning.databinding.TarotBirthdayWheelBinding
import java.text.DateFormatSymbols
import java.util.Locale

/** XML owns appearance; all selectable dates must match the chosen sign. */
class BirthdayWheel @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {
    private lateinit var ui: TarotUi
    private lateinit var zodiac: String
    private val binding = TarotBirthdayWheelBinding.inflate(LayoutInflater.from(context), this, true)
    private val month: BirthdayNumberPicker get() = binding.month
    private val day: BirthdayNumberPicker get() = binding.day
    private val year: BirthdayNumberPicker get() = binding.year
    private val today = ProfileValidator.today()
    private var months = emptyList<Int>()
    private val monthNames = DateFormatSymbols(Locale.getDefault()).months

    init {
        orientation = VERTICAL
        if (isInEditMode) bind(TarotUi(context), "2000-04-08", "Aries")
    }

    constructor(ui: TarotUi, initial: String, zodiac: String) : this(ui.context) {
        bind(ui, initial, zodiac)
    }

    fun bind(ui: TarotUi, initial: String, zodiac: String) {
        this.ui = ui
        this.zodiac = zodiac
        month.setOnValueChangedListener(null)
        year.setOnValueChangedListener(null)
        val date = ZodiacDates.constrain(ProfileValidator.parseBirthday(initial) ?: today, zodiac, today)
        year.minValue = ProfileValidator.MIN_BIRTH_YEAR
        year.maxValue = ZodiacDates.lastBirthYear(zodiac, today)
        year.value = date.year
        updateMonths(date.month)
        updateDays(date.day)
        listOf(month, day, year).forEach {
            it.wrapSelectorWheel = false
            it.selectedTextColor = ui.ink
            if (android.os.Build.VERSION.SDK_INT >= 29) {
                it.textColor = ui.muted
                it.textSize = resources.getDimension(R.dimen.sp_12)
                it.selectionDividerHeight = 0
            }
            for (index in 0 until it.childCount) {
                (it.getChildAt(index) as? EditText)?.apply {
                    typeface = ResourcesCompat.getFont(context, R.font.tarot_nunito_semibold)
                    textSize = ui.sp(14)
                    setTextColor(ui.ink)
                    includeFontPadding = false
                    setPadding(0, 0, 0, 0)
                }
            }
        }
        month.setOnValueChangedListener { _, _, _ -> updateDays(day.value) }
        year.setOnValueChangedListener { _, _, _ ->
            val oldMonth = selectedMonth()
            val oldDay = day.value
            updateMonths(oldMonth)
            updateDays(oldDay)
        }
    }

    private fun selectedMonth(): Int = months[month.value]

    private fun updateMonths(preferred: Int) {
        months = ZodiacDates.allowedMonths(zodiac, year.value, today)
        // Capricorn uses January and December: picker positions are not calendar months.
        month.displayedValues = null
        month.minValue = 0
        month.maxValue = months.lastIndex
        month.displayedValues = months.map { monthNames[it - 1] }.toTypedArray()
        month.value = months.indexOf(preferred).takeIf { it >= 0 } ?: 0
    }

    private fun updateDays(preferred: Int) {
        val allowed = requireNotNull(ZodiacDates.allowedDays(zodiac, year.value, selectedMonth(), today))
        // Reset min before max so disjoint ranges (e.g. Capricorn Jan -> Dec) stay safe.
        day.minValue = 1
        day.maxValue = allowed.last
        day.minValue = allowed.first
        day.value = preferred.coerceIn(allowed.first, allowed.last)
    }

    fun value(): String = String.format(Locale.US, "%04d-%02d-%02d", year.value, selectedMonth(), day.value)
    fun valid(): Boolean = ProfileValidator.isValidBirthday(value()) && ZodiacDates.signFor(value()) == zodiac
}

package com.tarot.card.meaning.tarot

import android.widget.*
import com.tarot.card.meaning.R
import com.tarot.card.meaning.databinding.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class TarotHoroscopeActivity : TarotActivity() {
    override val activityScreen = "horoscope"

    override fun renderScreen() {
        renderHoroscope(scrollContent())
    }

    private fun renderHoroscope(content: LinearLayout) {
        val profile = vm.preferences.profile ?: return
        val form = ActivityTarotHoroscopeBinding.inflate(layoutInflater, content, true)
        form.zodiacIcon.setImageResource(ui.zodiacResource(profile.zodiac))
        form.zodiacName.text = ui.translations.text(profile.zodiac)
        if (ui.dark) {
            form.zodiacName.setTextColor(ui.gold)
            form.zodiacName.background = ui.shape(ui.paper, 24, ui.gold)
        }
        val start = WeeklyHoroscopeGenerator.weekStart()
        val end = (start.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, 6) }
        val format = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
        form.weekPeriod.text = getString(R.string.tarot_horoscope_period, format.format(start.time), format.format(end.time))
        val horoscope = WeeklyHoroscopeGenerator.create(vm.cards, profile.zodiac, start, ui.translations::text) ?: return
        loadCard(form.pair.firstCard, horoscope.firstCard.imageUrl)
        loadCard(form.pair.secondCard, horoscope.secondCard.imageUrl)
        form.pair.firstCard.contentDescription = ui.translations.text(horoscope.firstCard.title)
        form.pair.secondCard.contentDescription = ui.translations.text(horoscope.secondCard.title)
        form.pair.pairTitle.text = getString(
            R.string.tarot_horoscope_pair_title,
            ui.translations.text(horoscope.firstCard.title),
            ui.translations.text(horoscope.secondCard.title)
        )
        form.pair.pairMeaning.text = horoscope.message
    }
}

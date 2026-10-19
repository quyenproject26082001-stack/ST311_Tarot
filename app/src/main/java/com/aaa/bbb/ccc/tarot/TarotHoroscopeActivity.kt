package com.aaa.bbb.ccc.tarot

import android.widget.*
import com.aaa.bbb.ccc.R
import com.aaa.bbb.ccc.databinding.*
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
        form.zodiacName.text = profile.zodiac
        val start = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, -(get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY)) }
        val end = (start.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, 6) }
        val format = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
        form.weekPeriod.text = getString(R.string.tarot_horoscope_period, format.format(start.time), format.format(end.time))
        // Keep the designed layout, but never fabricate a weekly prediction from general card data.
        form.pair.firstCard.setImageResource(R.drawable.tarot_deck_back)
        form.pair.secondCard.setImageResource(R.drawable.tarot_deck_back)
        form.pair.pairTitle.setText(R.string.tarot_pair_unavailable_title)
        form.pair.pairMeaning.setText(R.string.tarot_horoscope_unavailable)
    }
}

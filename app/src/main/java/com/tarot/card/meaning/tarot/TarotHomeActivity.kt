package com.tarot.card.meaning.tarot

import android.graphics.Color
import android.widget.*
import androidx.core.view.isVisible
import com.tarot.card.meaning.R
import com.tarot.card.meaning.core.extensions.checkInternet
import com.tarot.card.meaning.databinding.*

open class TarotHomeActivity : TarotActivity() {
    override val activityScreen = "home"

    override fun renderScreen() {
        renderHome(scrollContent())
    }

    private fun renderHome(content: LinearLayout) {
        val profile = vm.preferences.profile ?: return
        val home = ActivityTarotHomeBinding.inflate(layoutInflater, content, true)
        home.greeting.text = getString(R.string.tarot_greeting, profile.name)
        home.zodiacName.text = ui.translations.text(profile.zodiac)
        home.zodiacIcon.setImageResource(ui.zodiacResource(profile.zodiac))
        if (ui.dark) home.horoscope.setBackgroundResource(R.drawable.tarot_bg_horoscope_dark)
        WeeklyHoroscopeGenerator.create(vm.cards, profile.zodiac, WeeklyHoroscopeGenerator.weekStart(), ui.translations::text)?.let {
            home.horoscopeSummary.text = getString(R.string.tarot_home_horoscope_summary, it.summary)
        }
        home.horoscope.setOnClickListener { checkInternet { vm.screen = "horoscope" } }
        listOf(Spread.DAILY, Spread.WEEKLY, Spread.CELTIC).forEachIndexed { index, spread ->
            val action = listOf(home.actionDaily, home.actionWeekly, home.actionCeltic)[index]
            action.actionArt.setImageResource(actionArt(spread))
            action.actionTitle.text = ui.translations.text(spread.title)
            action.actionTitle.setBackgroundColor(Color.TRANSPARENT)
            if (ui.dark) action.actionTitle.setTextColor(ui.green)
            action.root.setOnClickListener { request(spread) }
        }
        home.emptyRecent.isVisible = vm.history.isEmpty()
        home.recentReadings.isVisible = vm.history.isNotEmpty()
        vm.history.take(3).forEach { readingRow(home.recentReadings, it) }
    }

    private fun actionArt(spread: Spread): Int = when (spread) {
        Spread.DAILY -> R.drawable.tarot_action_daily
        Spread.WEEKLY -> R.drawable.tarot_action_weekly
        else -> R.drawable.tarot_action_celtic
    }
}

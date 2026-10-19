package com.aaa.bbb.ccc.tarot

import android.graphics.Color
import android.widget.*
import androidx.core.view.isVisible
import com.aaa.bbb.ccc.R
import com.aaa.bbb.ccc.databinding.*

open class TarotHomeActivity : TarotActivity() {
    override val activityScreen = "home"

    override fun renderScreen() {
        renderHome(scrollContent())
    }

    private fun renderHome(content: LinearLayout) {
        val profile = vm.preferences.profile ?: return
        val home = ActivityTarotHomeBinding.inflate(layoutInflater, content, true)
        home.greeting.text = getString(R.string.tarot_greeting, profile.name)
        home.zodiacName.text = profile.zodiac
        home.zodiacIcon.setImageResource(ui.zodiacResource(profile.zodiac))
        if (ui.dark) home.horoscope.setBackgroundResource(R.drawable.tarot_bg_panel)
        home.horoscope.setOnClickListener { vm.screen = "horoscope" }
        listOf(Spread.DAILY, Spread.WEEKLY, Spread.CELTIC).forEachIndexed { index, spread ->
            val action = ItemTarotActionBinding.inflate(layoutInflater, home.quickActions, false)
            action.actionArt.setImageResource(actionArt(spread))
            action.actionTitle.text = spread.title
            if (!ui.dark) action.actionTitle.setBackgroundColor(Color.parseColor(listOf("#F7FBFB", "#FDF9E5", "#EEE0FB")[index]))
            if (index < 2) (action.root.layoutParams as LinearLayout.LayoutParams).marginEnd = ui.dp(12)
            action.root.setOnClickListener { request(spread) }
            home.quickActions.addView(action.root)
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

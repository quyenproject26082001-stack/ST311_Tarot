package com.tarot.card.meaning.tarot

import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.view.ContextThemeWrapper
import com.tarot.card.meaning.R

class TarotSettingsActivity : TarotActivity() {
    override val activityScreen = "settings"
    private var overlay: TarotSettingsOverlay? = null
    private lateinit var settingsInflater: LayoutInflater

    override fun initView() {
        super.initView()
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.settingsOverlayHost.childCount > 0) closeOverlay()
                else finishScreen()
            }
        })
    }

    private fun closeOverlay() {
        overlay?.dismiss()
        overlay = null
    }

    private fun showOverlay(title: String, content: (LinearLayout, TarotSettingsOverlay) -> Unit) {
        closeOverlay()
        overlay = TarotSettingsOverlay(this, settingsInflater, binding.settingsOverlayHost, binding.page, title, content)
    }

    override fun onDestroy() {
        closeOverlay()
        super.onDestroy()
    }

    override fun renderScreen() {
        val theme = if (vm.preferences.dark) R.style.TarotThemeDark else R.style.TarotThemeLight
        settingsInflater = layoutInflater.cloneInContext(ContextThemeWrapper(this, theme))
        TarotSettingsScreen(
            this, settingsInflater, vm, ui,
            onThemeChanged = { closeOverlay(); refreshTheme() },
            onChanged = { vm.refresh() },
            showOverlay = ::showOverlay
        ).render(scrollContent())
    }
}

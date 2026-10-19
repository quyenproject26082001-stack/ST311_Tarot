package com.aaa.bbb.ccc.tarot

class TarotSettingsActivity : TarotActivity() {
    override val activityScreen = "settings"

    override fun renderScreen() {
        TarotSettingsScreen(
            this, vm, ui,
            onThemeChanged = { refreshTheme() },
            onChanged = { vm.refresh() },
            onSheetOpened = { sheet = it }
        ).render(scrollContent())
    }
}

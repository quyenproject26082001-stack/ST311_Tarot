package com.tarot.card.meaning.tarot

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.EditText
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import com.tarot.card.meaning.R
import com.tarot.card.meaning.core.extensions.checkInternet
import com.tarot.card.meaning.core.extensions.policy
import com.tarot.card.meaning.core.extensions.shareApp
import com.tarot.card.meaning.core.helper.SharePreferenceHelper
import com.tarot.card.meaning.core.utils.DataLocal
import com.tarot.card.meaning.core.utils.key.IntentKey
import com.tarot.card.meaning.databinding.ActivityTarotSettingsBinding
import com.tarot.card.meaning.databinding.DialogTarotEditBirthdayBinding
import com.tarot.card.meaning.databinding.DialogTarotEditNameBinding
import com.tarot.card.meaning.databinding.DialogTarotStarSignBinding
import com.tarot.card.meaning.databinding.ItemTarotSettingBinding
import com.tarot.card.meaning.ui.language.LanguageActivity

/** All Settings geometry is in activity_tarot_settings.xml and item_tarot_setting.xml. */
class TarotSettingsScreen(
    private val activity: AppCompatActivity,
    private val inflater: LayoutInflater,
    private val vm: TarotViewModel,
    private val ui: TarotUi,
    private val onThemeChanged: () -> Unit,
    private val onChanged: () -> Unit,
    private val showOverlay: (String, (LinearLayout, TarotSettingsOverlay) -> Unit) -> Unit
) {
    fun render(parent: LinearLayout) {
        val profile = vm.preferences.profile ?: return
        val form = ActivityTarotSettingsBinding.inflate(inflater, parent, true)
        settingRow(form.personalGroup, "name", ui.s(R.string.tarot_name), profile.name) { editName() }
        divider(form.personalGroup)
        settingRow(form.personalGroup, "birthday", ui.s(R.string.tarot_birthday), displayBirthday(profile.birthday)) { editBirthday() }
        divider(form.personalGroup)
        settingRow(form.personalGroup, "moon", ui.s(R.string.tarot_zodiac), profile.zodiac) { editSign() }

        listOf(false to form.lightTheme, true to form.darkTheme).forEach { (dark, choice) ->
            choice.isSelected = dark == vm.preferences.dark
            if (dark == vm.preferences.dark) {
                choice.setBackgroundResource(R.drawable.tarot_bg_button)
                if (ui.dark) choice.backgroundTintList = ColorStateList.valueOf(ui.green)
                choice.setTextColor(Color.WHITE)
            }
            choice.setOnClickListener {
                if (dark != vm.preferences.dark) { vm.preferences.dark = dark; onThemeChanged() }
            }
        }
        form.guidanceToggle.isChecked = vm.preferences.showGuidance
        form.guidanceToggle.setOnCheckedChangeListener { _, checked -> vm.preferences.showGuidance = checked }
        form.reversePercent.text = "${vm.preferences.reverseChance}%"
        form.reverseChance.progress = vm.preferences.reverseChance
        form.reverseChance.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                form.reversePercent.text = "$progress%"
                if (fromUser) vm.preferences.reverseChance = progress
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        })
        form.reverseInfo.setOnClickListener {
            showOverlay(ui.s(R.string.tarot_reverse_chance)) { _, dialog ->
                dialog.showReverseInfo()
            }
        }
        settingRow(form.additionalGroup, "privacy", ui.s(R.string.tarot_privacy)) {
            activity.checkInternet { activity.policy() }
        }
        divider(form.additionalGroup)
        settingRow(form.additionalGroup, "share", ui.s(R.string.tarot_share)) {
            activity.shareApp()
        }
        divider(form.additionalGroup)
        val languageCode = SharePreferenceHelper(activity).getPreLanguage()
        val languageName = if (languageCode == "en") ui.s(R.string.tarot_language_english_us)
            else DataLocal.getLanguageList().firstOrNull { it.code == languageCode }?.name ?: languageCode
        settingRow(form.additionalGroup, "language", ui.s(R.string.tarot_language), trailing = languageName) {
            activity.startActivity(Intent(activity, LanguageActivity::class.java).putExtra(IntentKey.INTENT_KEY, "tarot_settings"))
        }
    }

    private fun settingRow(parent: LinearLayout, icon: String, title: String, value: String = "", trailing: String = "", action: () -> Unit) {
        val row = ItemTarotSettingBinding.inflate(inflater, parent, true)
        row.settingIcon.kind = icon
        if (ui.dark) row.settingChevron.imageTintList = ColorStateList.valueOf(ui.muted)
        if (icon == "moon") {
            row.settingIcon.setPadding(0, 0, 0, 0)
            row.settingIcon.background = null
            row.settingIcon.imageTintList = null
            row.settingIcon.setImageResource(R.drawable.tarot_ic_settings_star_sign)
        }
        row.settingTitle.text = title
        row.settingValue.isVisible = value.isNotEmpty()
        row.settingValue.text = if (icon == "moon") ui.translations.text(value) else value
        row.settingZodiacIcon.isVisible = icon == "moon" && value.isNotEmpty()
        if (row.settingZodiacIcon.isVisible) row.settingZodiacIcon.setImageResource(ui.zodiacResource(value))
        row.settingTrailing.text = trailing
        if (value.isEmpty()) {
            row.settingTitle.textSize = ui.sp(14)
            row.settingTitle.setTextColor(ui.ink)
            row.root.layoutParams.height = ui.dp(48)
        }
        row.root.setOnClickListener { action() }
    }

    private fun divider(parent: LinearLayout) {
        inflater.inflate(R.layout.tarot_setting_divider, parent, true)
    }

    private fun editName() {
        showOverlay(ui.s(R.string.tarot_edit_name)) { box, dialog ->
            val form = DialogTarotEditNameBinding.inflate(inflater, box, true)
            val field = form.nameField
            field.nameInput.setText(vm.preferences.profile?.name.orEmpty())
            field.nameCounter.text = "${field.nameInput.length()}/24"
            field.nameInput.doAfterTextChanged { field.nameCounter.text = "${it?.length ?: 0}/24" }
            form.submit.setOnClickListener {
                saveProfile(vm.preferences.profile?.copy(name = field.nameInput.text.toString()), dialog, field.nameInput)
            }
        }
    }

    private fun editBirthday() {
        val profile = vm.preferences.profile ?: return
        showOverlay(ui.s(R.string.tarot_edit_birthday)) { box, dialog ->
            val form = DialogTarotEditBirthdayBinding.inflate(inflater, box, true)
            form.birthdayDescription.text = ui.s(R.string.tarot_birthday_for_sign,
                ui.translations.text(profile.zodiac), ZodiacDates.range(profile.zodiac).label)
            form.birthdayPicker.bind(ui, profile.birthday, profile.zodiac)
            form.submit.setOnClickListener {
                saveProfile(vm.preferences.profile?.copy(birthday = form.birthdayPicker.value()), dialog)
            }
        }
    }

    private fun editSign() {
        val profile = vm.preferences.profile ?: return
        var selected = profile.zodiac
        var wheel: BirthdayWheel? = null
        showOverlay(ui.s(R.string.tarot_star_sign)) { box, dialog ->
            val form = DialogTarotStarSignBinding.inflate(inflater, box, true)
            fun update() {
                ui.bindZodiacGrid(form.zodiacGrid.root, selected) { selected = it; update() }
                val needsBirthday = ZodiacDates.signFor(profile.birthday) != selected
                form.birthdayDescription.isVisible = needsBirthday
                form.birthdayPicker.isVisible = needsBirthday
                wheel = null
                if (needsBirthday) {
                    form.birthdayDescription.text = ui.s(R.string.tarot_change_sign_birthday,
                        ui.translations.text(selected), ZodiacDates.range(selected).label)
                    form.birthdayPicker.bind(ui, profile.birthday, selected)
                    wheel = form.birthdayPicker
                }
            }
            update()
            form.submit.setOnClickListener {
                saveProfile(profile.copy(zodiac = selected, birthday = wheel?.value() ?: profile.birthday), dialog)
            }
        }
    }

    private fun saveProfile(profile: Profile?, dialog: TarotSettingsOverlay, nameInput: EditText? = null) {
        if (profile == null) {
            Toast.makeText(activity, R.string.tarot_profile_invalid, Toast.LENGTH_SHORT).show()
            return
        }
        val error = vm.preferences.saveProfile(profile)
        if (error != null) showProfileValidationError(activity, error, nameInput)
        else {
            dialog.dismiss()
            onChanged()
        }
    }

    private fun displayBirthday(value: String): String = value.split('-').takeIf { it.size == 3 }?.let { "${it[2]}/${it[1]}/${it[0]}" } ?: value
}

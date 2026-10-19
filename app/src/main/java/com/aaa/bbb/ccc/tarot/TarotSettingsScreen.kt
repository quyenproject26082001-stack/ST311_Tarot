package com.aaa.bbb.ccc.tarot

import android.content.Intent
import android.graphics.Color
import android.widget.LinearLayout
import android.widget.EditText
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import com.aaa.bbb.ccc.R
import com.aaa.bbb.ccc.core.extensions.hideNavigation
import com.aaa.bbb.ccc.core.helper.SharePreferenceHelper
import com.aaa.bbb.ccc.core.utils.key.IntentKey
import com.aaa.bbb.ccc.databinding.ActivityTarotSettingsBinding
import com.aaa.bbb.ccc.databinding.ItemTarotSettingBinding
import com.aaa.bbb.ccc.databinding.TarotFieldNameBinding
import com.aaa.bbb.ccc.ui.language.LanguageActivity
import com.google.android.material.bottomsheet.BottomSheetDialog

/** All Settings geometry is in activity_tarot_settings.xml and item_tarot_setting.xml. */
class TarotSettingsScreen(
    private val activity: AppCompatActivity,
    private val vm: TarotViewModel,
    private val ui: TarotUi,
    private val onThemeChanged: () -> Unit,
    private val onChanged: () -> Unit,
    private val onSheetOpened: (BottomSheetDialog) -> Unit
) {
    fun render(parent: LinearLayout) {
        val profile = vm.preferences.profile ?: return
        val form = ActivityTarotSettingsBinding.inflate(activity.layoutInflater, parent, true)
        settingRow(form.personalGroup, "name", ui.s(R.string.tarot_name), profile.name) { editName() }
        divider(form.personalGroup)
        settingRow(form.personalGroup, "birthday", ui.s(R.string.tarot_birthday), displayBirthday(profile.birthday)) { editBirthday() }
        divider(form.personalGroup)
        settingRow(form.personalGroup, "moon", ui.s(R.string.tarot_zodiac), profile.zodiac) { editSign() }

        listOf(false to form.lightTheme, true to form.darkTheme).forEach { (dark, choice) ->
            choice.isSelected = dark == vm.preferences.dark
            if (dark == vm.preferences.dark) {
                choice.setBackgroundResource(R.drawable.tarot_bg_button)
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
            showSheet(ui.s(R.string.tarot_reverse_chance)) { box, dialog ->
                ui.add(box, ui.text(ui.s(R.string.tarot_reverse_description), 14f, color = ui.muted), bottom = 18)
                ui.add(box, ui.button(ui.s(R.string.tarot_close), true) { dialog.dismiss() })
            }
        }
        settingRow(form.additionalGroup, "privacy", ui.s(R.string.tarot_privacy)) {
            AlertDialog.Builder(activity).setTitle(R.string.tarot_privacy).setMessage(R.string.tarot_privacy_content)
                .setPositiveButton(R.string.tarot_close, null).show().also { it.hideNavigation(!ui.dark) }
        }
        divider(form.additionalGroup)
        settingRow(form.additionalGroup, "share", ui.s(R.string.tarot_share)) {
            val share = Intent(Intent.ACTION_SEND).setType("text/plain")
                .putExtra(Intent.EXTRA_TEXT, "${ui.s(R.string.tarot_app_title)}\nhttps://play.google.com/store/apps/details?id=${activity.packageName}")
            activity.startActivity(Intent.createChooser(share, ui.s(R.string.tarot_share)))
        }
        divider(form.additionalGroup)
        settingRow(form.additionalGroup, "language", ui.s(R.string.tarot_language), trailing = SharePreferenceHelper(activity).getPreLanguage()) {
            activity.startActivity(Intent(activity, LanguageActivity::class.java).putExtra(IntentKey.INTENT_KEY, "tarot_settings"))
        }
    }

    private fun settingRow(parent: LinearLayout, icon: String, title: String, value: String = "", trailing: String = "", action: () -> Unit) {
        val row = ItemTarotSettingBinding.inflate(activity.layoutInflater, parent, true)
        row.settingIcon.kind = icon
        row.settingTitle.text = title
        row.settingValue.isVisible = value.isNotEmpty()
        row.settingValue.text = value
        row.settingZodiacIcon.isVisible = icon == "moon" && value.isNotEmpty()
        if (row.settingZodiacIcon.isVisible) row.settingZodiacIcon.setImageResource(ui.zodiacResource(value))
        row.settingTrailing.text = trailing
        if (value.isEmpty()) {
            row.settingTitle.textSize = 14f
            row.settingTitle.setTextColor(ui.ink)
            row.root.layoutParams.height = ui.dp(48)
        }
        row.root.setOnClickListener { action() }
    }

    private fun divider(parent: LinearLayout) {
        activity.layoutInflater.inflate(R.layout.tarot_setting_divider, parent, true)
    }

    private fun showSheet(title: String, content: (LinearLayout, BottomSheetDialog) -> Unit) {
        onSheetOpened(ui.sheet(title, content))
    }

    private fun editName() {
        showSheet(ui.s(R.string.tarot_edit_name)) { box, dialog ->
            val field = TarotFieldNameBinding.inflate(activity.layoutInflater, box, true)
            field.nameInput.setText(vm.preferences.profile?.name.orEmpty())
            field.nameCounter.text = "${field.nameInput.length()}/24"
            field.nameInput.doAfterTextChanged { field.nameCounter.text = "${it?.length ?: 0}/24" }
            ui.add(box, ui.button(ui.s(R.string.tarot_submit)) {
                saveProfile(vm.preferences.profile?.copy(name = field.nameInput.text.toString()), dialog, field.nameInput)
            })
        }
    }

    private fun editBirthday() {
        val profile = vm.preferences.profile ?: return
        showSheet(ui.s(R.string.tarot_edit_birthday)) { box, dialog ->
            ui.add(box, ui.text(ui.s(R.string.tarot_birthday_for_sign, profile.zodiac,
                ZodiacDates.range(profile.zodiac).label), 12f, color = ui.muted), bottom = 10)
            val wheel = BirthdayWheel(ui, profile.birthday, profile.zodiac)
            ui.add(box, wheel, bottom = 18)
            ui.add(box, ui.button(ui.s(R.string.tarot_submit)) {
                saveProfile(vm.preferences.profile?.copy(birthday = wheel.value()), dialog)
            })
        }
    }

    private fun editSign() {
        val profile = vm.preferences.profile ?: return
        var selected = profile.zodiac
        var wheel: BirthdayWheel? = null
        showSheet(ui.s(R.string.tarot_star_sign)) { box, dialog ->
            ui.add(box, ui.text(ui.s(R.string.tarot_star_description), 12f, color = ui.muted), bottom = 10)
            val host = android.widget.FrameLayout(activity)
            val birthdayBox = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
            fun update() {
                host.removeAllViews()
                host.addView(ui.zodiacGrid(selected) { selected = it; update() })
                birthdayBox.removeAllViews()
                wheel = null
                if (ZodiacDates.signFor(profile.birthday) != selected) {
                    // Choosing a different sign never silently changes a real birthday.
                    // Show the new date explicitly and commit both only on Submit.
                    ui.add(birthdayBox, ui.text(ui.s(R.string.tarot_change_sign_birthday, selected,
                        ZodiacDates.range(selected).label), 12f, color = ui.muted), bottom = 10)
                    wheel = BirthdayWheel(ui, profile.birthday, selected)
                    ui.add(birthdayBox, requireNotNull(wheel))
                }
            }
            ui.add(box, host, bottom = 18)
            ui.add(box, birthdayBox, bottom = 18)
            update()
            ui.add(box, ui.button(ui.s(R.string.tarot_submit)) {
                saveProfile(profile.copy(zodiac = selected, birthday = wheel?.value() ?: profile.birthday), dialog)
            })
        }
    }

    private fun saveProfile(profile: Profile?, dialog: BottomSheetDialog, nameInput: EditText? = null) {
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

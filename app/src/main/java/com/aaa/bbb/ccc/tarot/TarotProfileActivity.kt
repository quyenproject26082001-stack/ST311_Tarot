package com.aaa.bbb.ccc.tarot

import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import com.aaa.bbb.ccc.R
import com.aaa.bbb.ccc.core.base.BaseActivity
import com.aaa.bbb.ccc.databinding.ActivityTarotProfileBinding
import com.aaa.bbb.ccc.ui.home.HomeActivity

/** Profile layout is entirely XML; state is restored when Android recreates it. */
class TarotProfileActivity : BaseActivity<ActivityTarotProfileBinding>() {
    private lateinit var ui: TarotUi
    private var step = 0
    private var sign = ""
    private var enteredName = ""
    private var birthday = "1994-11-14"
    private var birthdayWheel: BirthdayWheel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(if (TarotPreferences(this).dark) R.style.TarotThemeDark else R.style.TarotThemeLight)
        step = savedInstanceState?.getInt("step") ?: 0
        sign = savedInstanceState?.getString("sign").orEmpty()
        enteredName = savedInstanceState?.getString("name").orEmpty()
        birthday = savedInstanceState?.getString("birthday") ?: birthday
        super.onCreate(savedInstanceState)
    }

    override fun setViewBinding() = ActivityTarotProfileBinding.inflate(layoutInflater)

    override fun useDarkStatusBarIcons(): Boolean = !TarotPreferences(this).dark

    override fun initView() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        ui = TarotUi(this, TarotPreferences(this).dark)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val safe = insets.getInsets(WindowInsetsCompat.Type.statusBars() or
                WindowInsetsCompat.Type.displayCutout() or WindowInsetsCompat.Type.ime())
            binding.profilePage.setPadding(0, safe.top, 0, safe.bottom)
            insets
        }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (step == 1) { capture(); step = 0; render() } else finish()
            }
        })
        render()
    }

    override fun viewListener() {
        binding.nameField.nameInput.doAfterTextChanged {
            binding.nameField.nameCounter.text = "${it?.length ?: 0}/24"
        }
    }

    override fun initActionBar() = Unit // Title/back depend on the current profile step.

    @Suppress("DEPRECATION")
    override fun onBackPressed() = onBackPressedDispatcher.onBackPressed()

    private fun capture() {
        if (step == 1) {
            enteredName = binding.nameField.nameInput.text.toString()
            birthdayWheel?.let { birthday = it.value() }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        capture()
        outState.putInt("step", step)
        outState.putString("sign", sign)
        outState.putString("name", enteredName)
        outState.putString("birthday", birthday)
        super.onSaveInstanceState(outState)
    }

    private fun render() {
        if (step == 1 && !ProfileValidator.isValidZodiac(sign)) step = 0
        birthdayWheel = null
        binding.zodiacGrid.removeAllViews()
        binding.birthdayHost.removeAllViews()
        binding.pageBackground.alpha = if (ui.dark) .06f else 1f
        val chooseSign = step == 0
        binding.actionBar.tvCenter.apply {
            isVisible = true
            setText(if (chooseSign) R.string.tarot_star_sign else R.string.tarot_create_profile)
        }
        // Previous below the form owns step navigation; the reference title is left aligned.
        binding.actionBar.btnActionBarLeft.isVisible = false
        binding.actionBar.btnActionBarLeft.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        binding.profileDescription.setText(if (chooseSign) R.string.tarot_star_description else R.string.tarot_profile_description)
        binding.birthdayHost.contentDescription = if (!chooseSign)
            getString(R.string.tarot_birthday_for_sign, sign, ZodiacDates.range(sign).label) else null
        binding.zodiacGrid.isVisible = chooseSign
        binding.profileFields.isVisible = !chooseSign
        if (chooseSign) {
            binding.zodiacGrid.addView(ui.zodiacGrid(sign) { sign = it; render() })
        } else {
            binding.nameField.nameInput.setText(enteredName)
            binding.nameField.nameCounter.text = "${enteredName.length}/24"
            birthdayWheel = BirthdayWheel(ui, birthday, sign)
            binding.birthdayHost.addView(birthdayWheel)
        }
        binding.secondStep.setBackgroundResource(if (chooseSign) R.drawable.tarot_bg_step_inactive else R.drawable.tarot_bg_step_active)
        binding.previous.isVisible = !chooseSign
        binding.previous.setOnClickListener { capture(); step = 0; render() }
        binding.next.isEnabled = !chooseSign || ProfileValidator.isValidZodiac(sign)
        binding.next.setOnClickListener {
            if (chooseSign) {
                if (ProfileValidator.isValidZodiac(sign)) { step = 1; render() }
                else showProfileValidationError(this, ProfileValidationError.INVALID_ZODIAC)
            }
            else saveProfile()
        }
        ViewCompat.requestApplyInsets(binding.root)
    }

    private fun saveProfile() {
        capture()
        val error = TarotPreferences(this).saveProfile(Profile(enteredName, birthday, sign))
        if (error != null) {
            showProfileValidationError(this, error, binding.nameField.nameInput)
            return
        }
        startActivity(Intent(this, HomeActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        finish()
    }
}

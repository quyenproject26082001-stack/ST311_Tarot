package com.tarot.card.meaning.ui.language

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.lvt.ads.util.Admob
import com.tarot.card.meaning.R
import com.tarot.card.meaning.core.base.BaseActivity
import com.tarot.card.meaning.core.helper.LanguageHelper
import com.tarot.card.meaning.core.extensions.applyActionBarTopInset
import com.tarot.card.meaning.core.extensions.gone
import com.tarot.card.meaning.core.extensions.handleBackLeftToRight
import com.tarot.card.meaning.core.extensions.invisible
import com.tarot.card.meaning.core.extensions.select
import com.tarot.card.meaning.core.extensions.startIntentRightToLeft
import com.tarot.card.meaning.core.extensions.startIntentWithClearTop
import com.tarot.card.meaning.core.extensions.visible
import com.tarot.card.meaning.core.utils.key.IntentKey
import com.tarot.card.meaning.databinding.ActivityLanguageBinding
import com.tarot.card.meaning.ui.home.HomeActivity
import com.tarot.card.meaning.ui.intro.IntroActivity
import com.tarot.card.meaning.tarot.TarotPreferences
import com.tarot.card.meaning.tarot.TarotUi
import com.tarot.card.meaning.tarot.applyTarotPageBackground
import com.tarot.card.meaning.core.extensions.tap
import com.tarot.card.meaning.core.extensions.strings
import kotlinx.coroutines.launch
import kotlin.system.exitProcess

class LanguageActivity : BaseActivity<ActivityLanguageBinding>() {
    private val viewModel: LanguageViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        if (TarotPreferences(this).dark) setTheme(R.style.TarotThemeDark)
        super.onCreate(savedInstanceState)
    }

    override fun useDarkStatusBarIcons(): Boolean = !TarotPreferences(this).dark

    private val languageAdapter by lazy { LanguageAdapter(this) }

    override fun setViewBinding(): ActivityLanguageBinding {
        return ActivityLanguageBinding.inflate(LayoutInflater.from(this))
    }

    override fun initView() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding.root.applyActionBarTopInset(binding.layoutActionBar)
        if (TarotPreferences(this).dark) {
            binding.btnBackground.background = null
            binding.btnBackground.scaleType = android.widget.ImageView.ScaleType.FIT_XY
            binding.btnBackground.applyTarotPageBackground(true)
        }
        initRcv()
        val intentValue = intent.getStringExtra(IntentKey.INTENT_KEY)
        val currentLang = sharePreference.getPreLanguage()
        viewModel.setFirstLanguage(intentValue == null)
        viewModel.loadLanguages(currentLang)
        binding.actionBar.tvStart.select()
        binding.actionBar.tvCenter.select()
    }

    override fun dataObservable() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.isFirstLanguage.collect { isFirst ->
                        languageAdapter.isFirstLanguage = isFirst
                        updateActionBar(isFirst)
                    }
                }
                launch {
                    viewModel.languageList.collect { list ->
                        languageAdapter.submitList(list)
                    }
                }
                launch {
                    viewModel.codeLang.collect { code ->
                        if (code.isNotEmpty() && viewModel.isFirstLanguage.value) {
                            binding.actionBar.btnActionBarRightLang.visible()
                        }
                    }
                }
            }
        }
    }

    override fun viewListener() {
        binding.apply {
            actionBar.btnActionBarLeft.tap { handleBackLeftToRight() }
            actionBar.btnActionBarRightLang.tap { handleDone() }
        }
        handleRcv()
    }

    override fun initText() {
        binding.actionBar.tvCenter.select()
        binding.actionBar.tvStart.select()
    }

    override fun initActionBar() {
        binding.actionBar.apply {
            tvCenter.text = strings(R.string.language)
            tvStart.text = strings(R.string.language)
        }
        updateActionBar(viewModel.isFirstLanguage.value)
    }

    private fun updateActionBar(isFirstLanguage: Boolean) {
        binding.actionBar.apply {
            if (isFirstLanguage) {
                btnActionBarLeft.invisible()
                tvStart.visible()
                btnActionBarRightLang.setImageResource(R.drawable.ic_done)
                btnActionBarRightLang.invisible()
            } else {
                tvCenter.visible()

                btnActionBarLeft.setImageResource(R.drawable.ic_back)
                btnActionBarLeft.visible()
                btnActionBarRightLang.setImageResource(R.drawable.ic_done)
                btnActionBarRightLang.visible()
            }
        }
        if (TarotPreferences(this).dark) {
            val ink = TarotUi(this, true).ink
            binding.actionBar.tvCenter.apply {
                setTextColor(ink)
                setDoubleStroke(Color.TRANSPARENT, 0f, Color.TRANSPARENT, 0f)
            }
            binding.actionBar.tvStart.apply {
                setTextColor(ink)
                setDoubleStroke(Color.TRANSPARENT, 0f, Color.TRANSPARENT, 0f)
            }
            binding.actionBar.btnActionBarLeft.imageTintList = ColorStateList.valueOf(ink)
            binding.actionBar.btnActionBarRightLang.imageTintList = ColorStateList.valueOf(ink)
        }
    }

    private fun initRcv() {
        binding.rcv.apply {
            adapter = languageAdapter
            itemAnimator = null
        }
    }

    private fun handleRcv() {
        binding.apply {
            languageAdapter.onItemClick = { code ->
                //  binding.actionBar.btnDone.visible()
                viewModel.selectLanguage(code)
            }
        }
    }

    private fun handleDone() {
        val code = viewModel.codeLang.value
        if (code.isEmpty()) {
            showToast(R.string.not_select_lang)
            return
        }
        LanguageHelper.changeLang(code, this)

        if (viewModel.isFirstLanguage.value) {
            sharePreference.setIsFirstLang(false)
            startIntentRightToLeft(IntroActivity::class.java)
            finishAffinity()
        } else if (intent.getStringExtra(IntentKey.INTENT_KEY) == "tarot_settings") {
            finish()
        } else {
            startIntentWithClearTop(HomeActivity::class.java)
        }
    }

    @SuppressLint("MissingSuperCall", "GestureBackNavigation")
    override fun onBackPressed() {
        if (!viewModel.isFirstLanguage.value) {
            handleBackLeftToRight()
        } else {
            exitProcess(0)
        }
    }

    // Chỉ tắt nhạc khi là màn language đầu tiên
    override fun shouldPlayBackgroundMusic(): Boolean = false

    override fun initAds() {
//        Admob.getInstance().loadNativeAd(this@LanguageActivity, getString(R.string.native_language), binding.nativeAds, R.layout.ads_native_big_btn_top)
    }

}

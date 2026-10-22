package com.tarot.card.meaning.ui.splash

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.content.Intent
import android.view.LayoutInflater
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.lvt.ads.callback.InterCallback
import com.lvt.ads.util.Admob
import com.tarot.card.meaning.R
import com.tarot.card.meaning.core.base.BaseActivity
import com.tarot.card.meaning.core.extensions.checkShowSplashWhenFail
import com.tarot.card.meaning.core.extensions.loadNativeCollabAds
import com.tarot.card.meaning.core.extensions.loadSplashInterAds
import com.tarot.card.meaning.core.utils.state.HandleState
import com.tarot.card.meaning.databinding.ActivitySplashBinding
import com.tarot.card.meaning.tarot.TarotPreferences
import com.tarot.card.meaning.ui.intro.IntroActivity
import com.tarot.card.meaning.ui.language.LanguageActivity
import com.tarot.card.meaning.ui.home.DataViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashActivity : BaseActivity<ActivitySplashBinding>() {
    var intentActivity: Intent? = null
    private val dataViewModel: DataViewModel by viewModels()
    var interCallBack: InterCallback? = null

    private val MIN_SPLASH_MS = 2000L  // Reduced from 3000ms to 1500ms for faster startup
    private val DATA_FALLBACK_MS = 8000L
    private val ADS_FALLBACK_MS = 12000L
    private var minTimePassed = false
    private var dataReady = false
    private var triggered = false
    private var navigated = false

    override fun setViewBinding(): ActivitySplashBinding {
        return ActivitySplashBinding.inflate(LayoutInflater.from(this)).also { splashBinding ->
            if (TarotPreferences(this).dark) {
                splashBinding.imgSplashBackground.setImageResource(R.drawable.img_splash_bg_dark_mode)
                val gold = getColor(R.color.tarot_gold)
                splashBinding.ivLoading.imageTintList = ColorStateList.valueOf(gold)
                splashBinding.tvDes.setTextColor(gold)
            }
        }
    }

    override fun initView() {
        // Start loading animation
        val rotateAnimation = android.view.animation.AnimationUtils.loadAnimation(this, R.anim.rotate_loading)
        binding.ivLoading.startAnimation(rotateAnimation)

        if (!isTaskRoot &&
            intent.hasCategory(Intent.CATEGORY_LAUNCHER) &&
            intent.action != null &&
            intent.action.equals(Intent.ACTION_MAIN)) {
            finish(); return
        }

        intentActivity = if (sharePreference.getIsFirstLang()) {
            Intent(this, LanguageActivity::class.java)
        } else {
            Intent(this, IntroActivity::class.java)
        }
//        Admob.getInstance().setTimeLimitShowAds(30000)
//        Admob.getInstance().setTimeCountdownNativeCollab(20000)
//        Admob.getInstance().setOpenShowAllAds(false)
        interCallBack = object : InterCallback() {
            override fun onNextAction() {
                super.onNextAction()
                navigateNext()
            }
        }
        dataViewModel.saveAndReadData(this)

        lifecycleScope.launch {
            kotlinx.coroutines.delay(MIN_SPLASH_MS)
            minTimePassed = true
            tryProceed()
        }

        lifecycleScope.launch {
            delay(DATA_FALLBACK_MS)
            dataReady = true
            tryProceed()
        }
    }

    override fun dataObservable() {
        lifecycleScope.launch {
            dataViewModel.dataLoadFinished.collect { finished ->
                if (finished) {
                    // Data is ready, no need to call API again
                    // (API already called in saveAndReadData if needed)
                    dataReady = true
                    tryProceed()
                }
            }
        }
    }

    private fun tryProceed() {
        if (triggered) return
        if (!minTimePassed || !dataReady) return

        triggered = true

//        loadSplashInterAds(getString(R.string.inter_splash), 30000, 2000, interCallBack)
        navigateNext()

        lifecycleScope.launch {
            delay(ADS_FALLBACK_MS)
            navigateNext()
        }
    }

    private fun navigateNext() {
        if (navigated || isFinishing || isDestroyed) return
        navigated = true
        startActivity(intentActivity)
        finishAffinity()
    }

    override fun viewListener() {
    }

    override fun initText() {}

    override fun initActionBar() {}

    @SuppressLint("GestureBackNavigation", "MissingSuperCall")
    override fun onBackPressed() {}

//    override fun initAds() {
//        initNativeCollab()
//    }

//    fun initNativeCollab() {
//
//        loadNativeCollabAds(R.string.native_splash, binding.flNativeCollab)
//
//
//    }

    override fun onResume() {
        super.onResume()
//        checkShowSplashWhenFail(interCallBack, 1000)
    }

    override fun shouldPlayBackgroundMusic(): Boolean = false

    override fun useDarkStatusBarIcons(): Boolean = !TarotPreferences(this).dark
}

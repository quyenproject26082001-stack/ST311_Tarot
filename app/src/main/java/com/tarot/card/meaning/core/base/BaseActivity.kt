package com.tarot.card.meaning.core.base

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.Nullable
import androidx.appcompat.app.AppCompatActivity

import androidx.viewbinding.ViewBinding
import com.tarot.card.meaning.R
import com.tarot.card.meaning.core.extensions.handleBackLeftToRight
import com.tarot.card.meaning.core.extensions.hideNavigation
import com.tarot.card.meaning.core.helper.LanguageHelper
import com.tarot.card.meaning.core.helper.MusicHelper
import com.tarot.card.meaning.core.helper.SharePreferenceHelper
import com.tarot.card.meaning.core.helper.SoundHelper
import com.tarot.card.meaning.dialog.WaitingDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.lazy


abstract class BaseActivity<T : ViewBinding> : AppCompatActivity() {

    private var _binding: T? = null
    val binding get() = _binding!!

    protected abstract fun setViewBinding(): T

    protected fun replaceViewBinding(viewBinding: T, initialize: () -> Unit) {
        _binding = viewBinding
        initialize()
        setContentView(viewBinding.root)
    }

    protected abstract fun initView()

    protected abstract fun viewListener()

    open fun dataObservable() {}

    open fun initText() {}

    protected abstract fun initActionBar()

    open fun initAds() {}

    // Override this to disable background music for specific activities (splash, intro, language, permission)
    protected open fun shouldPlayBackgroundMusic(): Boolean = false

    // Light screens use dark status icons; dark screens can override this.
    protected open fun useDarkStatusBarIcons(): Boolean = true

    protected val loadingDialog: WaitingDialog by lazy {
        WaitingDialog(this)
    }
    protected val sharePreference: SharePreferenceHelper by lazy {
        SharePreferenceHelper(this)
    }

    protected lateinit var toast: Toast

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LanguageHelper.setLocale(this)
        _binding = setViewBinding()
        setContentView(binding.root)
        setUpUI()

    }

    private fun setUpUI() {
        toast = Toast.makeText(this, "", Toast.LENGTH_SHORT)
        initView()
        if (!SoundHelper.isSoundNotNull(R.raw.touch)) {
            SoundHelper.loadSound(this, R.raw.touch)
        }
        // Initialize background music only if activity should play it
        if (shouldPlayBackgroundMusic()) {
            MusicHelper.init(this)
            if (sharePreference.isMusicEnabled()) {
                MusicHelper.play()
            }
        }
        initAds()
        dataObservable()
        viewListener()
        initActionBar()
        findViewById<TextView>(R.id.tvLeftText)?.isSelected = true
        findViewById<TextView>(R.id.tvCenter)?.isSelected = true
        findViewById<TextView>(R.id.tvRightText)?.isSelected = true
        initText()
    }

    override fun onResume() {
        super.onResume()
        hideNavigation(useDarkStatusBarIcons())
        // Resume music if enabled and activity should play it
        if (shouldPlayBackgroundMusic() && sharePreference.isMusicEnabled()) {
            MusicHelper.play()
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        // Dialogs, permission windows and returning from another app can reveal navigation.
        if (hasFocus) hideNavigation(useDarkStatusBarIcons())
    }

    override fun onPause() {
        super.onPause()
        // Pause music when app goes to background (only if this activity plays music)
        if (shouldPlayBackgroundMusic()) {
            MusicHelper.pause()
        }
    }

    @SuppressLint("MissingSuperCall", "GestureBackNavigation")
    override fun onBackPressed() {
        handleBackLeftToRight()
    }

    suspend fun showLoading() {
        withContext(Dispatchers.Main) {
            if (loadingDialog.isShowing.not()) {
                LanguageHelper.setLocale(this@BaseActivity)
                loadingDialog.show()
            }
        }
    }


    suspend fun dismissLoading(isBlack: Boolean = true) {
        withContext(Dispatchers.Main) {
            if (loadingDialog.isShowing) {
                loadingDialog.dismiss()
                hideNavigation(useDarkStatusBarIcons())
            }
        }
    }

    fun showToast(content: Any) {
        if (toast != null){
            toast.cancel()
        }
        val contentString = when (content) {
            is String -> content
            is Int -> getString(content)
            else -> {""}
        }
        toast = Toast.makeText(this, contentString, Toast.LENGTH_SHORT)
        toast.show()
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}

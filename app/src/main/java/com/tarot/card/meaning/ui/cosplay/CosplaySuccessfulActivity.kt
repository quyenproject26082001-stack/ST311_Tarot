package com.tarot.card.meaning.ui.cosplay

import android.annotation.SuppressLint
import android.content.Intent
import android.view.LayoutInflater
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.tarot.card.meaning.R
import com.tarot.card.meaning.core.base.BaseActivity
import com.tarot.card.meaning.core.extensions.handleBackLeftToRight
import com.tarot.card.meaning.core.extensions.loadImage
import com.tarot.card.meaning.core.extensions.showInterAll
import com.tarot.card.meaning.core.extensions.startIntentWithClearTop
import com.tarot.card.meaning.core.extensions.tap
import com.tarot.card.meaning.core.extensions.visible
import com.tarot.card.meaning.core.utils.key.IntentKey
import com.tarot.card.meaning.databinding.ActivityCosplaySuccessfulBinding
import com.tarot.card.meaning.ui.home.HomeActivity
import com.tarot.card.meaning.ui.success.SuccessViewModel
import kotlinx.coroutines.launch

class CosplaySuccessfulActivity : BaseActivity<ActivityCosplaySuccessfulBinding>() {
    private var characterPosition = 0

    private val viewModel: SuccessViewModel by viewModels()

    private var finalProgress = 0

    override fun setViewBinding(): ActivityCosplaySuccessfulBinding {
        return ActivityCosplaySuccessfulBinding.inflate(LayoutInflater.from(this))
    }

    override fun initView() {
        val path = intent.getStringExtra(IntentKey.COSPLAY_RESULT_PATH_KEY) ?: ""
        finalProgress = intent.getIntExtra(IntentKey.COSPLAY_PROGRESS_KEY, 0)
        characterPosition = intent.getIntExtra(
            IntentKey.COSPLAY_CHARACTER_POSITION_KEY,
            0
        )

        binding.includeLayoutBottom.tvShare.isSelected = true
        viewModel.setPath(path)
        updateProgressDisplay()
    }

    override fun dataObservable() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.pathInternal.collect { path ->
                    if (path.isNotEmpty()) {
                        loadImage(this@CosplaySuccessfulActivity, path, binding.imvImage)
                    }
                }
            }
        }
    }

    override fun viewListener() {
        binding.apply {
            actionBar.btnActionBarRight.tap {
                showInterAll {
                    startIntentWithClearTop(HomeActivity::class.java)
                }

            }
            actionBar.btnActionBarLeft.tap {
                handleBackLeftToRight()
            }
            includeLayoutBottom.btnTryAgain.tap(800) {
                showInterAll {
                    val intent = Intent(
                        this@CosplaySuccessfulActivity,
                        CosplayCustomizeActivity::class.java
                    ).apply {
                        putExtra(
                            IntentKey.COSPLAY_CHARACTER_POSITION_KEY,
                            characterPosition
                        )
                    }

                    startActivity(intent)
                    finish()
                }
            }


        }
    }

    override fun initActionBar() {
        binding.actionBar.apply {
            btnActionBarRight.visible()
            btnActionBarRight.setImageResource(R.drawable.ic_home)
            tvCenter.visible()
            tvCenter.setText(R.string.successfully1)
        }
    }

    private fun updateProgressDisplay() {
        val progress = finalProgress.coerceIn(0, 100)
        binding.tvProgress.text = "$progress/100"
        val starRes = when {
            progress == 0 -> R.drawable.zero_star_ss
            progress < 20 -> R.drawable.one_star_ss
            progress < 40 -> R.drawable.two_star_ss
            progress < 60 -> R.drawable.three_star_ss
            progress < 80 -> R.drawable.four_star_ss
            else -> R.drawable.five_star_ss
        }
        binding.starSS.setImageResource(starRes)
    }

    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {
        handleBackLeftToRight()
    }
}

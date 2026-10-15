package com.aaa.bbb.ccc.ui.intro

import android.annotation.SuppressLint
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import com.lvt.ads.util.Admob
import com.aaa.bbb.ccc.R
import com.aaa.bbb.ccc.core.base.BaseActivity
import com.aaa.bbb.ccc.core.extensions.gone
import com.aaa.bbb.ccc.core.utils.DataLocal
import com.aaa.bbb.ccc.databinding.ActivityIntroBinding
import com.aaa.bbb.ccc.ui.home.HomeActivity
import com.aaa.bbb.ccc.tarot.TarotPreferences
import com.aaa.bbb.ccc.tarot.TarotProfileActivity
import com.aaa.bbb.ccc.ui.permission.PermissionActivity
import com.aaa.bbb.ccc.core.extensions.tap
import com.aaa.bbb.ccc.core.extensions.visible
import kotlin.system.exitProcess
import kotlin.text.toInt

class IntroActivity : BaseActivity<ActivityIntroBinding>() {
    private val introAdapter by lazy { IntroAdapter(this) }
    private val dotIndicators = listOf(
        R.drawable.ic_dot_1,
        R.drawable.ic_dot_2,
        R.drawable.ic_dot_3
    )

    override fun setViewBinding(): ActivityIntroBinding {
        return ActivityIntroBinding.inflate(LayoutInflater.from(this))
    }


    override fun initView() {
        initVpg()
    }


    override fun viewListener() {
        binding.btnNext.tap { handleNext() }

        binding.vpgTutorial.registerOnPageChangeCallback(object :
            ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                binding.dotsIndicator.setImageResource(
                    dotIndicators[position.coerceIn(dotIndicators.indices)]
                )
                val params = binding.btnNext.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams

//                if (position == 0) {
//                    binding.nativeAds.gone()
//                    // Set lại width về 100dp
//                    params.width = ViewGroup.LayoutParams.MATCH_PARENT
//                    params.height = (48 * resources.displayMetrics.density).toInt()
//                    params.bottomMargin = (5 * resources.displayMetrics.density).toInt()
//                    params.marginEnd = (5 * resources.displayMetrics.density).toInt()
//
//                    // Chiều cao
//                    binding.btnNext.layoutParams = params
//                    binding.btnNext.setTextColor(resources.getColor(R.color.white, null))
//                    binding.btnNext.setBackgroundResource(R.drawable.bg_btn_ads)
//                } else {
////                    binding.nativeAds.visible()
//                    binding.nativeAds.gone()
//                    // Đổi sang wrap_content
//                    params.width = ViewGroup.LayoutParams.WRAP_CONTENT
//                    params.marginEnd = (16 * resources.displayMetrics.density).toInt()
//                    params.bottomMargin = (0 * resources.displayMetrics.density).toInt()
//                    binding.btnNext.layoutParams = params
//                    binding.btnNext.setTextColor(ContextCompat.getColor(this@IntroActivity,R.color.app))
//                    binding.btnNext.background = null
//                }
            }
        })
    }

    override fun initText() {}

    override fun initActionBar() {}

    private fun initVpg() {
        binding.apply {
            binding.vpgTutorial.adapter = introAdapter
            binding.dotsIndicator.setImageResource(dotIndicators.first())
            introAdapter.submitList(DataLocal.itemIntroList)
        }
    }

    private fun handleNext() {
        binding.apply {
            val nextItem = binding.vpgTutorial.currentItem + 1
            if (nextItem < DataLocal.itemIntroList.size) {
                vpgTutorial.setCurrentItem(nextItem, true)
            } else {
                val intent =
                    if (sharePreference.getIsFirstPermission()) {
                        Intent(this@IntroActivity, PermissionActivity::class.java)
                    } else {
                        Intent(this@IntroActivity, if (TarotPreferences(this@IntroActivity).profile == null) TarotProfileActivity::class.java else HomeActivity::class.java)
                    }
                startActivity(intent)
                finishAffinity()
            }
        }
    }

    @SuppressLint("MissingSuperCall", "GestureBackNavigation")
    override fun onBackPressed() { exitProcess(0) }

    override fun shouldPlayBackgroundMusic(): Boolean = false

    override fun initAds() {
//        Admob.getInstance().loadNativeAd(this, getString(R.string.native_intro), binding.nativeAds, R.layout.ads_native_medium_btn_bottom)
    }
}

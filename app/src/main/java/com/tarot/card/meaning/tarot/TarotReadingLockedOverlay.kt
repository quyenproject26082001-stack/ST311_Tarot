package com.tarot.card.meaning.tarot

import android.os.CountDownTimer
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.tarot.card.meaning.R
import com.tarot.card.meaning.databinding.OverlayTarotReadingLockedBinding
import java.util.Locale

class TarotReadingLockedOverlay(
    private val activity: AppCompatActivity,
    private val host: FrameLayout,
    private val page: View,
    remaining: Long,
    onWatch: () -> Unit,
    onClose: () -> Unit,
    onExpired: () -> Unit
) {
    private val binding = OverlayTarotReadingLockedBinding.inflate(activity.layoutInflater, host, false)
    private val originalAccessibility = page.importantForAccessibility
    val isShowing: Boolean get() = binding.root.parent != null
    private val timer = object : CountDownTimer(remaining, 1_000L) {
        override fun onTick(millisUntilFinished: Long) = updateRemaining(millisUntilFinished)

        override fun onFinish() = onExpired()
    }

    init {
        val form = binding.lockedForm
        form.lockedDescription.text = activity.getString(R.string.tarot_locked_description,
            TarotRules.COOLDOWN_MS / 3_600_000)
        form.close.setOnClickListener { onClose() }
        form.watchAd.setOnClickListener { onWatch() }
        updateRemaining(remaining)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val safe = insets.getInsets(WindowInsetsCompat.Type.statusBars() or
                WindowInsetsCompat.Type.displayCutout() or WindowInsetsCompat.Type.ime())
            view.setPadding(safe.left, safe.top, safe.right, safe.bottom)
            insets
        }
        page.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
        host.addView(binding.root)
        binding.root.requestFocus()
        ViewCompat.requestApplyInsets(binding.root)
        timer.start()
    }

    fun setLoading(loading: Boolean) {
        binding.lockedForm.watchAd.isEnabled = !loading
        binding.lockedForm.watchAdLabel.setText(
            if (loading) R.string.tarot_ad_loading else R.string.tarot_watch_ad)
    }

    private fun updateRemaining(milliseconds: Long) {
        val minutes = (milliseconds.coerceAtLeast(0L) + 59_999) / 60_000
        val time = String.format(Locale.getDefault(), "%02d:%02d", minutes / 60, minutes % 60)
        binding.lockedForm.remainingTime.text = activity.getString(R.string.tarot_wait_time, time)
    }

    fun dismiss() {
        timer.cancel()
        if (!isShowing) return
        host.removeView(binding.root)
        page.importantForAccessibility = originalAccessibility
        page.requestFocus()
    }
}

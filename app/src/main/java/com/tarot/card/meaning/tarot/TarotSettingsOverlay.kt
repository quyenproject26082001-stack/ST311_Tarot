package com.tarot.card.meaning.tarot

import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.tarot.card.meaning.databinding.OverlayTarotSettingsBinding
import com.tarot.card.meaning.databinding.OverlayTarotReverseInfoBinding
import com.tarot.card.meaning.R

class TarotSettingsOverlay(
    private val activity: AppCompatActivity,
    private val inflater: LayoutInflater,
    private val host: FrameLayout,
    private val page: View,
    title: String,
    build: (LinearLayout, TarotSettingsOverlay) -> Unit
) {
    private val binding = OverlayTarotSettingsBinding.inflate(inflater, host, false)
    private val originalAccessibility = page.importantForAccessibility

    init {
        binding.dialogForm.sheetTitle.text = title
        binding.dialogForm.close.setOnClickListener { dismiss() }
        binding.dialogForm.root.background = null
        build(binding.dialogForm.sheetContent, this)
        val padding = binding.root.paddingLeft
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val safe = insets.getInsets(WindowInsetsCompat.Type.statusBars() or
                WindowInsetsCompat.Type.displayCutout() or WindowInsetsCompat.Type.ime())
            view.setPadding(padding + safe.left, padding + safe.top,
                padding + safe.right, padding + safe.bottom)
            insets
        }
        page.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
        host.addView(binding.root)
        binding.root.requestFocus()
        ViewCompat.requestApplyInsets(binding.root)
    }

    fun showReverseInfo() {
        val form = OverlayTarotReverseInfoBinding.inflate(inflater)
        form.closeIcon.setOnClickListener { dismiss() }
        form.closeButton.setOnClickListener { dismiss() }
        binding.dialogForm.root.apply {
            removeAllViews()
            setPadding(0, 0, 0, 0)
            addView(form.root)
        }
        binding.dialogScroll.setBackgroundResource(R.drawable.tarot_reverse_info_panel)
    }

    fun dismiss() {
        if (binding.root.parent == null) return
        WindowCompat.getInsetsController(activity.window, binding.root).hide(WindowInsetsCompat.Type.ime())
        host.removeView(binding.root)
        page.importantForAccessibility = originalAccessibility
        page.requestFocus()
    }
}

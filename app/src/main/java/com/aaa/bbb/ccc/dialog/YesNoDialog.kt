package com.aaa.bbb.ccc.dialog

import android.app.Activity
import android.graphics.Color
import android.widget.LinearLayout
import androidx.core.view.updateLayoutParams
import com.aaa.bbb.ccc.core.extensions.gone
import com.aaa.bbb.ccc.core.extensions.hideNavigation
import com.aaa.bbb.ccc.core.extensions.tap
import com.aaa.bbb.ccc.R
import com.aaa.bbb.ccc.core.base.BaseDialog
import com.aaa.bbb.ccc.core.extensions.strings
import com.aaa.bbb.ccc.databinding.DialogConfirmBinding

class YesNoDialog(
    val context: Activity,
    val title: Int,
    val description: Int,
    val isError: Boolean = false
) : BaseDialog<DialogConfirmBinding>(context, maxWidth = true, maxHeight = true) {
    override val layoutId: Int = R.layout.dialog_confirm
    override val isCancelOnTouchOutside: Boolean = false
    override val isCancelableByBack: Boolean = false

    var onNoClick: (() -> Unit) = {}
    var onYesClick: (() -> Unit) = {}
    var onDismissClick: (() -> Unit) = {}

    override fun initView() {
        initText()
        initBackground()
        if (isError) {
            binding.btnNo.gone()
            binding.btnYes.updateLayoutParams<LinearLayout.LayoutParams> {
                weight = 0f
                width = context.resources.getDimensionPixelSize(R.dimen.dp_144)
            }

        }
        context.hideNavigation()
        binding.tvTitle.isSelected = true
    }

    private fun initBackground() {
        binding.containerDialog.setBackgroundResource(R.drawable.bg_dialog_delete_exit)
        binding.btnNo.setBackgroundResource(R.drawable.ic_no_dialog)
        binding.btnYes.setBackgroundResource(R.drawable.ic_yes_dialog)
        val paddingVertical = context.resources.getDimensionPixelSize(R.dimen.dp_9)
        binding.btnNo.setPadding(0, paddingVertical, 0, paddingVertical)
        binding.btnYes.setPadding(0, paddingVertical, 0, paddingVertical)
    }

    override fun initAction() {
        binding.apply {
            btnNo.tap { onNoClick.invoke() }
            btnYes.tap { onYesClick.invoke() }
            flOutSide.tap { onDismissClick.invoke() }
        }
    }

    override fun onDismissListener() {}

    private fun initText() {
        binding.apply {
            tvTitle.text = context.strings(title)
            tvDescription.text = context.strings(description)
            if (isError) {
                btnYes.text = context.strings(R.string.ok)
            }
        }
    }
}

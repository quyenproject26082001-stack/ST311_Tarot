package com.tarot.card.meaning.dialog

import android.app.Activity
import android.widget.LinearLayout
import androidx.core.view.updateLayoutParams
import com.tarot.card.meaning.core.extensions.gone
import com.tarot.card.meaning.core.extensions.hideNavigation
import com.tarot.card.meaning.core.extensions.tap
import com.tarot.card.meaning.R
import com.tarot.card.meaning.core.base.BaseDialog
import com.tarot.card.meaning.core.extensions.strings
import com.tarot.card.meaning.databinding.DialogConfirmBinding

class YesNoDialog(
    val context: Activity,
    val title: Int,
    val description: Int,
    val isError: Boolean = false,
    val noButtonText: Int = R.string.no,
    val yesButtonText: Int = R.string.yes
) : BaseDialog<DialogConfirmBinding>(context, maxWidth = true, maxHeight = true) {
    override val layoutId: Int = R.layout.dialog_confirm
    override val isCancelOnTouchOutside: Boolean = false
    override val isCancelableByBack: Boolean = false

    var onNoClick: (() -> Unit) = {}
    var onYesClick: (() -> Unit) = {}
    var onDismissClick: (() -> Unit) = {}

    override fun initView() {
        initText()
        if (isError) {
            binding.btnNo.gone()
            binding.btnYes.updateLayoutParams<LinearLayout.LayoutParams> {
                weight = 0f
                width = context.resources.getDimensionPixelSize(R.dimen.dp_144)
                marginStart = 0
            }

        }
        context.hideNavigation()
        binding.tvTitle.isSelected = true
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
            btnNo.text = context.strings(noButtonText)
            btnYes.text = context.strings(yesButtonText)
            if (isError) {
                btnYes.text = context.strings(R.string.ok)
            }
        }
    }
}

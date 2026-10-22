package com.tarot.card.meaning.dialog

import android.app.Activity
import android.widget.Toast
import com.tarot.card.meaning.R
import com.tarot.card.meaning.core.base.BaseDialog
import com.tarot.card.meaning.core.extensions.hideNavigation
import com.tarot.card.meaning.core.extensions.tap
import com.tarot.card.meaning.databinding.DialogCreateNameBinding
import kotlin.apply
import kotlin.text.trim
import kotlin.toString


class CreateNameDialog(val context: Activity) :
    BaseDialog<DialogCreateNameBinding>(context, maxWidth = true, maxHeight = true) {
    override val layoutId: Int = R.layout.dialog_create_name
    override val isCancelOnTouchOutside: Boolean = false
    override val isCancelableByBack: Boolean = false

    var onNoClick: (() -> Unit) = {}
    var onYesClick: ((String) -> Unit) = {}

    override fun initView() {
        context.hideNavigation()
    }

    override fun initAction() {
        binding.apply {
            tvNo.tap {
                onNoClick.invoke()
            }
            tvYes.tap {
                val input = edtName.text.toString().trim()

                when {
                    input == "" -> {
                        Toast.makeText(context, context.getString(R.string.please_enter_your_package_name), Toast.LENGTH_SHORT).show()
                    }


                    else -> {
                        onYesClick.invoke(input)
                    }
                }
            }
        }
    }

    override fun onDismissListener() {

    }
}

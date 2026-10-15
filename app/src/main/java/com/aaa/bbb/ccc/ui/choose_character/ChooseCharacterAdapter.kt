package com.aaa.bbb.ccc.ui.choose_character

import androidx.core.content.ContextCompat
import com.aaa.bbb.ccc.R
import com.aaa.bbb.ccc.core.base.BaseAdapter
import com.aaa.bbb.ccc.core.extensions.gone
import com.aaa.bbb.ccc.core.extensions.loadImage
import com.aaa.bbb.ccc.core.extensions.tap
import com.aaa.bbb.ccc.data.model.custom.CustomizeModel
import com.aaa.bbb.ccc.databinding.ItemChooseAvatarBinding

class ChooseCharacterAdapter : BaseAdapter<CustomizeModel, ItemChooseAvatarBinding>(ItemChooseAvatarBinding::inflate) {
    var onItemClick: ((position: Int) -> Unit) = {}


    override fun onBind(binding: ItemChooseAvatarBinding, item: CustomizeModel, position: Int) {
        binding.apply {

            loadImage(item.avatar, imvImage, onDismissLoading = {
                sflShimmer.stopShimmer()
                sflShimmer.gone()
            })
            root.tap { onItemClick.invoke(position) }
        }
    }
}
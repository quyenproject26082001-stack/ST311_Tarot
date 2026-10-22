package com.tarot.card.meaning.ui.choose_character

import androidx.core.content.ContextCompat
import com.tarot.card.meaning.R
import com.tarot.card.meaning.core.base.BaseAdapter
import com.tarot.card.meaning.core.extensions.gone
import com.tarot.card.meaning.core.extensions.loadImage
import com.tarot.card.meaning.core.extensions.tap
import com.tarot.card.meaning.data.model.custom.CustomizeModel
import com.tarot.card.meaning.databinding.ItemChooseAvatarBinding

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
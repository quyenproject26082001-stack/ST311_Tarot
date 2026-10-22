package com.tarot.card.meaning.ui.add_character.adapter

import com.tarot.card.meaning.core.base.BaseAdapter
import com.tarot.card.meaning.core.extensions.loadImage
import com.tarot.card.meaning.core.extensions.loadImageSticker
import com.tarot.card.meaning.core.extensions.tap
import com.tarot.card.meaning.data.model.SelectedModel
import com.tarot.card.meaning.databinding.ItemStickerBinding

class StickerAdapter : BaseAdapter<SelectedModel, ItemStickerBinding>(ItemStickerBinding::inflate) {
    var onItemClick : ((String) -> Unit) = {}
    override fun onBind(binding: ItemStickerBinding, item: SelectedModel, position: Int) {
        binding.apply {
            loadImageSticker(root, item.path, imvSticker)
            root.tap { onItemClick.invoke(item.path) }
        }
    }
}
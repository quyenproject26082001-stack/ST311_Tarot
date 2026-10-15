package com.aaa.bbb.ccc.ui.add_character.adapter

import com.aaa.bbb.ccc.core.base.BaseAdapter
import com.aaa.bbb.ccc.core.extensions.loadImage
import com.aaa.bbb.ccc.core.extensions.loadImageSticker
import com.aaa.bbb.ccc.core.extensions.tap
import com.aaa.bbb.ccc.data.model.SelectedModel
import com.aaa.bbb.ccc.databinding.ItemStickerBinding

class StickerAdapter : BaseAdapter<SelectedModel, ItemStickerBinding>(ItemStickerBinding::inflate) {
    var onItemClick : ((String) -> Unit) = {}
    override fun onBind(binding: ItemStickerBinding, item: SelectedModel, position: Int) {
        binding.apply {
            loadImageSticker(root, item.path, imvSticker)
            root.tap { onItemClick.invoke(item.path) }
        }
    }
}
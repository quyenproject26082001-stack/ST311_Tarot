package com.aaa.bbb.ccc.ui.add_character.adapter

import com.aaa.bbb.ccc.core.base.BaseAdapter
import com.aaa.bbb.ccc.core.extensions.loadImageSticker
import com.aaa.bbb.ccc.core.extensions.tap
import com.aaa.bbb.ccc.data.model.SelectedModel
import com.aaa.bbb.ccc.databinding.ItemSpeechBinding

class SpeechAdapter : BaseAdapter<SelectedModel, ItemSpeechBinding>(ItemSpeechBinding::inflate) {
    var onItemClick: ((String) -> Unit) = {}

    override fun onBind(binding: ItemSpeechBinding, item: SelectedModel, position: Int) {
        binding.apply {
            loadImageSticker(root, item.path, imvSpeech)
            root.tap { onItemClick.invoke(item.path) }
        }
    }
}

package com.tarot.card.meaning.ui.add_character.adapter

import com.tarot.card.meaning.core.base.BaseAdapter
import com.tarot.card.meaning.core.extensions.loadImageSticker
import com.tarot.card.meaning.core.extensions.tap
import com.tarot.card.meaning.data.model.SelectedModel
import com.tarot.card.meaning.databinding.ItemSpeechBinding

class SpeechAdapter : BaseAdapter<SelectedModel, ItemSpeechBinding>(ItemSpeechBinding::inflate) {
    var onItemClick: ((String) -> Unit) = {}

    override fun onBind(binding: ItemSpeechBinding, item: SelectedModel, position: Int) {
        binding.apply {
            loadImageSticker(root, item.path, imvSpeech)
            root.tap { onItemClick.invoke(item.path) }
        }
    }
}

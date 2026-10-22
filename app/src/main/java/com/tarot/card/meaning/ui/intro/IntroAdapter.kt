package com.tarot.card.meaning.ui.intro

import android.content.Context
import com.tarot.card.meaning.core.base.BaseAdapter
import com.tarot.card.meaning.core.extensions.loadImage
import com.tarot.card.meaning.core.extensions.select
import com.tarot.card.meaning.core.extensions.strings
import com.tarot.card.meaning.data.model.IntroModel
import com.tarot.card.meaning.databinding.ItemIntroBinding
import com.tarot.card.meaning.tarot.TarotPreferences
import com.tarot.card.meaning.tarot.TarotUi

class IntroAdapter(val context: Context) : BaseAdapter<IntroModel, ItemIntroBinding>(
    ItemIntroBinding::inflate
) {
    override fun onBind(binding: ItemIntroBinding, item: IntroModel, position: Int) {
        val dark = TarotPreferences(context).dark
        binding.apply {
            loadImage(root, if (dark) item.darkImage ?: item.image else item.image, imvImage, false)
            tvContent.text = context.strings(item.content)
            if (dark) tvContent.setTextColor(TarotUi(context, true).ink)
            tvContent.select()
        }
    }
}

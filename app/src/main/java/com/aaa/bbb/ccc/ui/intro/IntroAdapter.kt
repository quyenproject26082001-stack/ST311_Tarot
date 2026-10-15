package com.aaa.bbb.ccc.ui.intro

import android.content.Context
import com.aaa.bbb.ccc.core.base.BaseAdapter
import com.aaa.bbb.ccc.core.extensions.loadImage
import com.aaa.bbb.ccc.core.extensions.select
import com.aaa.bbb.ccc.core.extensions.strings
import com.aaa.bbb.ccc.data.model.IntroModel
import com.aaa.bbb.ccc.databinding.ItemIntroBinding

class IntroAdapter(val context: Context) : BaseAdapter<IntroModel, ItemIntroBinding>(
    ItemIntroBinding::inflate
) {
    override fun onBind(binding: ItemIntroBinding, item: IntroModel, position: Int) {
        binding.apply {
            loadImage(root, item.image, imvImage, false)
            tvContent.text = context.strings(item.content)
            tvContent.select()
        }
    }
}
package com.aaa.bbb.ccc.ui.language

import android.annotation.SuppressLint
import android.content.Context
import com.aaa.bbb.ccc.R
import com.aaa.bbb.ccc.core.base.BaseAdapter
import com.aaa.bbb.ccc.core.extensions.gone
import com.aaa.bbb.ccc.core.extensions.loadImage
import com.aaa.bbb.ccc.core.extensions.tap
import com.aaa.bbb.ccc.core.extensions.visible
import com.aaa.bbb.ccc.data.model.LanguageModel
import com.aaa.bbb.ccc.databinding.ItemLanguageBinding

class LanguageAdapter(val context: Context) : BaseAdapter<LanguageModel, ItemLanguageBinding>(
    ItemLanguageBinding::inflate
) {
    var onItemClick: ((code: String) -> Unit) = {}
    var isFirstLanguage: Boolean = false

    override fun submitList(list: List<LanguageModel>) {
        if (items.isEmpty()) {
            items.addAll(list)
            notifyDataSetChanged()
        } else {
            val oldList = items.toList()
            items.clear()
            items.addAll(list)

            // Find changed items
            val changedPositions = mutableListOf<Int>()
            for (i in list.indices) {
                if (i < oldList.size && oldList[i].activate != list[i].activate) {
                    changedPositions.add(i)
                }
            }

            // Only notify changed items
            if (changedPositions.isNotEmpty()) {
                changedPositions.forEach { notifyItemChanged(it) }
            }
        }
    }

    override fun onBind(
        binding: ItemLanguageBinding, item: LanguageModel, position: Int
    ) {
        binding.apply {
            loadImage(root, item.flag, imvFlag, false)
            tvLang.text = item.name

            val textColor = android.graphics.Color.parseColor("#38306B")

            // Remove shader and set solid color
            tvLang.paint.shader = null
            tvLang.setTextColor(textColor)

            val ratio = if (item.activate) {
                R.drawable.ic_tick_lang
            } else {
                R.drawable.ic_not_tick_lang
            }
            loadImage(root, ratio, btnRadio, false)

            // Apply color tint when activated and not first language
//            if (item.activate && !isFirstLanguage) {
//                btnRadio.setColorFilter(
//                    android.graphics.Color.parseColor("#01579B"),
//                    android.graphics.PorterDuff.Mode.SRC_IN
//                )
//            } else {
//                btnRadio.clearColorFilter()
//            }

            // Set selected state to trigger the selector drawable
            flMain.isSelected = item.activate

            root.tap {
                onItemClick.invoke(item.code)
            }
        }
    }
}

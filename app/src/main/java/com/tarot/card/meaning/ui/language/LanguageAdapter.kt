package com.tarot.card.meaning.ui.language

import android.annotation.SuppressLint
import android.content.Context
import com.tarot.card.meaning.R
import com.tarot.card.meaning.core.base.BaseAdapter
import com.tarot.card.meaning.core.extensions.gone
import com.tarot.card.meaning.core.extensions.loadImage
import com.tarot.card.meaning.core.extensions.tap
import com.tarot.card.meaning.core.extensions.visible
import com.tarot.card.meaning.data.model.LanguageModel
import com.tarot.card.meaning.databinding.ItemLanguageBinding
import com.tarot.card.meaning.tarot.TarotPreferences
import com.tarot.card.meaning.tarot.TarotUi

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

            val dark = TarotPreferences(context).dark
            val textColor = TarotUi(context, dark).ink
            if (dark) flMain.setBackgroundResource(R.drawable.tarot_language_dark_selector)

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

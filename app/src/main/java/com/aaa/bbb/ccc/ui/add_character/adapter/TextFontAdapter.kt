package com.aaa.bbb.ccc.ui.add_character.adapter

import android.annotation.SuppressLint
import android.content.Context
import com.aaa.bbb.ccc.R
import com.aaa.bbb.ccc.core.base.BaseAdapter
import com.aaa.bbb.ccc.core.extensions.setFont
import com.aaa.bbb.ccc.core.extensions.tap
import com.aaa.bbb.ccc.data.model.SelectedModel
import com.aaa.bbb.ccc.databinding.ItemFontBinding

class TextFontAdapter(val context: Context) : BaseAdapter<SelectedModel, ItemFontBinding>(ItemFontBinding::inflate) {
    var onTextFontClick: ((Int, Int) -> Unit) = { _, _ -> }
    private var currentSelected = 0

    override fun onBind(binding: ItemFontBinding, item: SelectedModel, position: Int) {
        binding.apply {
            tvFont.setFont(item.color)

            if (item.isSelected) {
                // Selected state - set selected background and change text color
                cvMain.setBackgroundResource(R.drawable.bg_item_font_selected)
                tvFont.setTextColor(android.graphics.Color.parseColor("#38306B"))
            } else {
                // Not selected state - white circle background
                cvMain.setBackgroundResource(R.drawable.bg_item_font_not_selected)
                tvFont.setTextColor(android.graphics.Color.parseColor("#1F1F1F"))
            }

            root.tap { onTextFontClick.invoke(item.color, position) }
        }
    }

    fun submitItem(position: Int, list: ArrayList<SelectedModel>) {
        if (position != currentSelected) {
            items.clear()
            items.addAll(list)

            notifyItemChanged(currentSelected)
            notifyItemChanged(position)

            currentSelected = position
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun submitListReset(list: ArrayList<SelectedModel>){
        items.clear()
        items.addAll(list)
        currentSelected = 0
        notifyDataSetChanged()
    }
}
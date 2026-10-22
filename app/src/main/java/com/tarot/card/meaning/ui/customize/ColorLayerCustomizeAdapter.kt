package com.tarot.card.meaning.ui.customize

import android.content.Context
import android.graphics.drawable.GradientDrawable
import androidx.core.graphics.toColorInt
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.tarot.card.meaning.R
import com.tarot.card.meaning.core.base.BaseAdapter
import com.tarot.card.meaning.core.extensions.tap
import com.tarot.card.meaning.data.model.custom.ItemColorModel
import com.tarot.card.meaning.databinding.ItemColorBinding

class ColorLayerCustomizeAdapter(val context: Context) :
    BaseAdapter<ItemColorModel, ItemColorBinding>(ItemColorBinding::inflate) {
    var onItemClick: ((Int) -> Unit) = {}
    override fun onBind(binding: ItemColorBinding, item: ItemColorModel, position: Int) {
        binding.apply {
            val colorSize = root.resources.getDimensionPixelSize(
                if (item.isSelected) R.dimen.dp_28 else R.dimen.dp_24
            )
            imvImage.layoutParams = imvImage.layoutParams.apply {
                width = colorSize
                height = colorSize
            }
            imvImage.background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(item.color.toColorInt())
            }
            imvFocus.isVisible = item.isSelected
            root.tap {
                val rv = root.parent as? RecyclerView ?: return@tap
                val currentPosition = rv.getChildAdapterPosition(root)
                if (currentPosition != RecyclerView.NO_POSITION) {
                    onItemClick.invoke(currentPosition)
                }
            }
        }
    }
}

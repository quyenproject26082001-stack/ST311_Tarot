package com.tarot.card.meaning.tarot

import android.content.Context
import android.util.AttributeSet
import android.widget.GridLayout

class TarotZodiacGrid @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : GridLayout(context, attrs, defStyleAttr) {
    init {
        if (isInEditMode) TarotUi(context).bindZodiacGrid(this, null) {}
    }
}

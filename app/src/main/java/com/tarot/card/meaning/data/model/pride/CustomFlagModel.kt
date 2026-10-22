package com.tarot.card.meaning.data.model.pride

import android.graphics.Color

data class CustomFlagModel(
    val name: String,
    val colors: MutableList<Int> = mutableListOf(Color.BLACK)
)

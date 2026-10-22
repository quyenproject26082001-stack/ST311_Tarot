package com.tarot.card.meaning.tarot

import android.graphics.ColorMatrixColorFilter
import android.widget.ImageView
import com.tarot.card.meaning.R

fun ImageView.applyTarotPageBackground(dark: Boolean) {
    setImageResource(R.drawable.bg_app)
    alpha = 1f
    colorFilter = if (dark) {
        ColorMatrixColorFilter(floatArrayOf(
            0f, 0f, -0.76f, 0f, 200.8f,
            0f, 0f, -0.55f, 0f, 169.25f,
            0f, 0f, -0.21f, 0f, 85.55f,
            0f, 0f, 0f, 1f, 0f,
        ))
    } else null
}

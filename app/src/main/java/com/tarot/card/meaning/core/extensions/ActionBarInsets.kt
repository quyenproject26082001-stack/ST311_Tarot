package com.tarot.card.meaning.core.extensions

import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams

fun View.applyActionBarTopInset(actionBar: View) {
    val originalTopMargin = (actionBar.layoutParams as ViewGroup.MarginLayoutParams).topMargin
    ViewCompat.setOnApplyWindowInsetsListener(this) { _, insets ->
        val safe = insets.getInsets(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.displayCutout())
        val topMargin = originalTopMargin + safe.top
        if ((actionBar.layoutParams as ViewGroup.MarginLayoutParams).topMargin != topMargin) {
            actionBar.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                this.topMargin = topMargin
            }
        }
        insets
    }
    ViewCompat.requestApplyInsets(this)
}

package com.aaa.bbb.ccc.tarot

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import com.aaa.bbb.ccc.R

/** Binds an icon name to an XML vector. No Canvas paths or text glyphs. */
class TarotIcon @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : AppCompatImageView(context, attrs) {
    var kind: String = ""
        set(value) { field = value; updateIcon() }
    var color: Int = Color.parseColor("#172A2A")
        set(value) { field = value; imageTintList = ColorStateList.valueOf(value) }

    init {
        scaleType = ScaleType.FIT_CENTER
        val inset = (7 * resources.displayMetrics.density).toInt()
        if (paddingLeft == 0 && paddingTop == 0 && paddingRight == 0 && paddingBottom == 0) {
            setPadding(inset, inset, inset, inset)
        }
        context.obtainStyledAttributes(attrs, R.styleable.TarotIcon).apply {
            kind = getString(R.styleable.TarotIcon_tarotIconName).orEmpty()
            color = getColor(R.styleable.TarotIcon_tarotIconColor, color)
            recycle()
        }
    }

    private fun updateIcon() {
        val resource = when (kind) {
            "back" -> R.drawable.tarot_ic_back
            "chevron" -> R.drawable.ic_arrow_settings
            "close" -> R.drawable.tarot_ic_close
            "home" -> R.drawable.tarot_ic_home
            "spreads" -> R.drawable.tarot_ic_spreads
            "cards" -> R.drawable.tarot_ic_cards
            "history" -> R.drawable.tarot_ic_history
            "settings" -> R.drawable.tarot_ic_settings
            "grid" -> R.drawable.tarot_ic_grid
            "list" -> R.drawable.tarot_ic_list
            "birthday" -> R.drawable.tarot_ic_birthday
            "name" -> R.drawable.tarot_ic_name
            "moon" -> R.drawable.tarot_ic_moon
            "sun" -> R.drawable.tarot_ic_sun
            "privacy" -> R.drawable.tarot_ic_privacy
            "share" -> R.drawable.tarot_ic_share
            "language" -> R.drawable.tarot_ic_language
            "info" -> R.drawable.tarot_ic_info
            "lock" -> R.drawable.tarot_ic_lock
            "heart_filled" -> R.drawable.tarot_ic_heart_filled
            else -> R.drawable.tarot_ic_heart
        }
        setImageResource(resource)
    }
}

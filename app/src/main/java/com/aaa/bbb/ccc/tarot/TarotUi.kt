package com.aaa.bbb.ccc.tarot

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import com.aaa.bbb.ccc.R
import com.aaa.bbb.ccc.databinding.ItemTarotZodiacBinding
import com.aaa.bbb.ccc.databinding.SheetTarotBinding
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlin.math.roundToInt

data class Zodiac(val name: String, val image: Int) {
    val dates: String get() = ZodiacDates.range(name).label
}

val zodiacSigns = listOf(
    Zodiac("Aries", R.drawable.tarot_zodiac_aries),
    Zodiac("Taurus", R.drawable.tarot_zodiac_taurus),
    Zodiac("Gemini", R.drawable.tarot_zodiac_gemini),
    Zodiac("Cancer", R.drawable.tarot_zodiac_cancer),
    Zodiac("Leo", R.drawable.tarot_zodiac_leo),
    Zodiac("Virgo", R.drawable.tarot_zodiac_virgo),
    Zodiac("Libra", R.drawable.tarot_zodiac_libra),
    Zodiac("Scorpio", R.drawable.tarot_zodiac_scorpio),
    Zodiac("Sagittarius", R.drawable.tarot_zodiac_sagittarius),
    Zodiac("Capricorn", R.drawable.tarot_zodiac_capricorn),
    Zodiac("Aquarius", R.drawable.tarot_zodiac_aquarius),
    Zodiac("Pisces", R.drawable.tarot_zodiac_pisces)
)

/** Small binding helpers. Screen layouts and static styling live in XML. */
class TarotUi(val context: Context, val dark: Boolean = false) {
    val ink = Color.parseColor(if (dark) "#EFF5F3" else "#172A2A")
    val muted = Color.parseColor(if (dark) "#B1BEBD" else "#60706D")
    val paper = Color.parseColor(if (dark) "#172829" else "#FFFFFF")
    val surface = Color.parseColor(if (dark) "#223A3B" else "#FCFBF7")
    val gold = context.getColor(R.color.tarot_gold)
    val green = context.getColor(R.color.tarot_green)
    fun dp(value: Int) = (value * context.resources.displayMetrics.density).roundToInt()
    fun s(id: Int, vararg args: Any): String = context.getString(id, *args)

    // Used only where the fill is data-dependent (spread type / selected mode).
    fun shape(fill: Int, radius: Int, border: Int? = null) = GradientDrawable().apply {
        setColor(fill)
        cornerRadius = dp(radius).toFloat()
        border?.let { setStroke(maxOf(1, dp(1)), it) }
    }

    fun text(value: String, size: Float = 14f, bold: Boolean = false, color: Int = ink): TextView =
        (LayoutInflater.from(context).inflate(R.layout.tarot_view_text, null) as TextView).apply {
            text = value
            textSize = size
            setTextColor(color)
            if (bold) typeface = ResourcesCompat.getFont(context, R.font.nunito_bold)
        }

    fun button(value: String, outline: Boolean = false, action: () -> Unit): TextView =
        (LayoutInflater.from(context).inflate(R.layout.tarot_view_button, null) as TextView).apply {
            text = value
            if (outline) { setBackgroundResource(R.drawable.tarot_bg_outline); setTextColor(gold) }
            setOnClickListener { action() }
        }

    fun add(parent: LinearLayout, view: View, height: Int = ViewGroup.LayoutParams.WRAP_CONTENT, bottom: Int = 0) {
        val actualHeight = if (height < 0) height else dp(height)
        val params = LinearLayout.LayoutParams(-1, actualHeight).apply { bottomMargin = dp(bottom) }
        // Buttons inflated without a parent must retain their XML height.
        if (height == ViewGroup.LayoutParams.WRAP_CONTENT && view is TextView && view.isClickable) params.height = dp(46)
        parent.addView(view, params)
    }

    fun heading(parent: LinearLayout, value: String) = add(parent, text(value, 18f, true), bottom = 12)

    fun image(slug: String, url: String, height: Int): ImageView = ImageView(context).apply {
        contentDescription = slug.replace('-', ' ')
        scaleType = ImageView.ScaleType.FIT_CENTER
        minimumHeight = dp(height)
        Glide.with(this).load(url).placeholder(R.drawable.tarot_deck_back).error(R.drawable.tarot_deck_back).into(this)
    }

    fun zodiacResource(name: String): Int = zodiacSigns.firstOrNull { it.name == name }?.image ?: R.drawable.tarot_zodiac_aries

    fun zodiacGrid(selected: String?, onSelect: (String) -> Unit): GridLayout = GridLayout(context).apply {
        columnCount = 3
        zodiacSigns.forEach { sign ->
            val item = ItemTarotZodiacBinding.inflate(LayoutInflater.from(context), this, false)
            item.root.isSelected = selected == sign.name
            item.zodiacIcon.setImageResource(sign.image)
            item.zodiacName.text = sign.name
            item.zodiacDates.text = sign.dates
            if (item.root.isSelected) {
                item.zodiacName.setTextColor(ink)
                item.zodiacName.typeface = ResourcesCompat.getFont(context, R.font.nunito_bold)
            }
            item.root.setOnClickListener { onSelect(sign.name) }
            addView(item.root)
        }
    }

    fun sheet(title: String, build: (LinearLayout, BottomSheetDialog) -> Unit): BottomSheetDialog {
        val dialog = BottomSheetDialog(context)
        val form = SheetTarotBinding.inflate(LayoutInflater.from(context))
        form.sheetTitle.text = title
        form.close.setOnClickListener { dialog.dismiss() }
        build(form.sheetContent, dialog)
        val scroll = ScrollView(context).apply { addView(form.root); isVerticalScrollBarEnabled = false }
        dialog.setContentView(scroll)
        dialog.window?.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        dialog.setOnShowListener {
            (scroll.parent as? View)?.setBackgroundColor(Color.TRANSPARENT)
            dialog.window?.setLayout(minOf(context.resources.displayMetrics.widthPixels, dp(520)), -2)
            dialog.behavior.state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
        }
        dialog.show()
        return dialog
    }
}

package com.tarot.card.meaning.tarot

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
import androidx.core.view.ViewCompat
import com.tarot.card.meaning.R
import com.tarot.card.meaning.core.extensions.hideNavigation
import com.tarot.card.meaning.databinding.ItemTarotZodiacBinding
import com.tarot.card.meaning.databinding.SheetTarotBinding
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlin.math.roundToInt

data class Zodiac(val name: String, val image: Int) {
    val dates: String get() = ZodiacDates.range(name).label
}

val zodiacSigns = listOf(
    Zodiac("Aries", R.drawable.ic_aries),
    Zodiac("Taurus", R.drawable.ic_taurus),
    Zodiac("Gemini", R.drawable.ic_gemini),
    Zodiac("Cancer", R.drawable.ic_cancer),
    Zodiac("Leo", R.drawable.ic_leo),
    Zodiac("Virgo", R.drawable.ic_virgo),
    Zodiac("Libra", R.drawable.ic_libra),
    Zodiac("Scorpio", R.drawable.ic_scorpio),
    Zodiac("Sagittarius", R.drawable.ic_sagittarius),
    Zodiac("Capricorn", R.drawable.ic_capricorn),
    Zodiac("Aquarius", R.drawable.ic_aquarius),
    Zodiac("Pisces", R.drawable.ic_pisces)
)

/** Small binding helpers. Screen layouts and static styling live in XML. */
class TarotUi(val context: Context, val dark: Boolean = false) {
    val translations = TarotTranslations(context)
    val ink = Color.parseColor(if (dark) "#E3E2CE" else "#172A2A")
    val muted = Color.parseColor(if (dark) "#CDDEDA" else "#60706D")
    val paper = Color.parseColor(if (dark) "#071D20" else "#FFFFFF")
    val surface = Color.parseColor(if (dark) "#1E4748" else "#FCFBF7")
    val gold = context.getColor(R.color.tarot_gold)
    val green = context.getColor(R.color.tarot_green)
    fun dp(value: Int): Int {
        val resourceId = context.resources.getIdentifier("dp_$value", "dimen", context.packageName)
        return if (resourceId != 0) {
            context.resources.getDimensionPixelSize(resourceId)
        } else {
            val tabletScale = if (context.resources.configuration.smallestScreenWidthDp >= 600) 1.5f else 1f
            (value * context.resources.displayMetrics.density * tabletScale).roundToInt()
        }
    }

    fun sp(value: Int): Float {
        val resourceId = context.resources.getIdentifier("sp_$value", "dimen", context.packageName)
        return if (resourceId != 0) {
            context.resources.getDimension(resourceId) / context.resources.displayMetrics.scaledDensity
        } else {
            val tabletScale = if (context.resources.configuration.smallestScreenWidthDp >= 600) 1.5f else 1f
            value * tabletScale
        }
    }
    fun s(id: Int, vararg args: Any): String = context.getString(id, *args)

    // Used only where the fill is data-dependent (spread type / selected mode).
    fun shape(fill: Int, radius: Int, border: Int? = null) = GradientDrawable().apply {
        setColor(fill)
        cornerRadius = dp(radius).toFloat()
        border?.let { setStroke(maxOf(1, dp(1)), it) }
    }

    fun text(value: String, size: Int = 14, bold: Boolean = false, color: Int = ink): TextView =
        (LayoutInflater.from(context).inflate(R.layout.tarot_view_text, null) as TextView).apply {
            text = value
            textSize = sp(size)
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

    fun heading(parent: LinearLayout, value: String) = add(parent, text(value, 18, true), bottom = 12)

    fun image(slug: String, url: String, height: Int): ImageView = ImageView(context).apply {
        contentDescription = slug.replace('-', ' ')
        scaleType = ImageView.ScaleType.FIT_CENTER
        minimumHeight = dp(height)
        Glide.with(this).load(url).placeholder(R.drawable.tarot_deck_back).error(R.drawable.tarot_deck_back).into(this)
    }

    fun zodiacResource(name: String): Int = zodiacSigns.firstOrNull { it.name == name }?.image ?: R.drawable.ic_aries

    fun zodiacGrid(selected: String?, onSelect: (String) -> Unit): GridLayout =
        (LayoutInflater.from(context).inflate(R.layout.tarot_zodiac_grid, null) as GridLayout).apply {
            bindZodiacGrid(this, selected, onSelect)
        }

    fun bindZodiacGrid(grid: GridLayout, selected: String?, onSelect: (String) -> Unit) {
        grid.removeAllViews()
        zodiacSigns.forEach { sign ->
            val item = ItemTarotZodiacBinding.inflate(LayoutInflater.from(context), grid, false)
            item.root.isSelected = selected == sign.name
            item.zodiacIcon.setImageResource(sign.image)
            item.zodiacName.text = translations.text(sign.name)
            item.zodiacDates.text = sign.dates
            if (item.root.isSelected) {
                item.zodiacName.setTextColor(ink)
                item.zodiacName.typeface = ResourcesCompat.getFont(context, R.font.nunito_bold)
            }
            item.root.setOnClickListener { onSelect(sign.name) }
            grid.addView(item.root)
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
            // A bottom sheet needs a full-height window, not a centered WRAP_CONTENT dialog.
            dialog.window?.setLayout(minOf(context.resources.displayMetrics.widthPixels, dp(520)), -1)
            dialog.behavior.isGestureInsetBottomIgnored = true
            fitSheetToHiddenNavigation(dialog)
            dialog.behavior.state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
            dialog.hideNavigation(!dark)
        }
        dialog.show()
        return dialog
    }

    fun fitSheetToHiddenNavigation(dialog: BottomSheetDialog) {
        val surface = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet) ?: return
        // Material otherwise retains a navigation-bar padding even after immersive mode hides it.
        ViewCompat.setOnApplyWindowInsetsListener(surface) { view, insets ->
            view.setPadding(view.paddingLeft, view.paddingTop, view.paddingRight, 0)
            insets
        }
        surface.setPadding(surface.paddingLeft, surface.paddingTop, surface.paddingRight, 0)
        ViewCompat.requestApplyInsets(surface)
    }
}

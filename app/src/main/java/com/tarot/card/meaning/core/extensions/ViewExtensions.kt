package com.tarot.card.meaning.core.extensions

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.FontRes
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.FragmentActivity
import androidx.core.graphics.createBitmap
import com.tarot.card.meaning.R
import com.tarot.card.meaning.core.helper.RateHelper
import com.tarot.card.meaning.core.helper.SharePreferenceHelper
import com.tarot.card.meaning.core.helper.SoundHelper
import com.tarot.card.meaning.core.utils.DataLocal
import com.tarot.card.meaning.core.utils.state.RateState
import kotlin.math.roundToInt

// ----------------------------
// Visibility extensions
// ----------------------------
fun View.visible() { visibility = View.VISIBLE }
fun View.invisible() { visibility = View.INVISIBLE }
fun View.gone() { visibility = View.GONE }
fun View.select() { isSelected = true }


// ----------------------------
// Navigation / Transition
// ----------------------------
fun Activity.handleBackLeftToRight() {
    finish()
    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
}
fun Activity.handleBackRightToLeft() {
    finish()
    overridePendingTransition(R.anim.slide_out_right, R.anim.slide_in_left)
}
fun Context.handleBackFragmentFromRight() {
    if (this is FragmentActivity) {
        overridePendingTransition(R.anim.slide_out_left, R.anim.slide_in_right)
        supportFragmentManager.popBackStack()
    }
}
fun Activity.hideNavigation(isBlack: Boolean = true) {
    window?.setFlags(
        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
    )

    // Set status bar color — always transparent so app content shows through
    window.statusBarColor = android.graphics.Color.TRANSPARENT

    window.decorView.systemUiVisibility = navigationUiFlags(isBlack)
}

/** Dialogs own a separate window; don't apply Activity's NO_LIMITS geometry to a sheet. */
fun Dialog.hideNavigation(isBlack: Boolean = true) {
    window?.apply {
        statusBarColor = android.graphics.Color.TRANSPARENT
        decorView.systemUiVisibility = navigationUiFlags(isBlack)
    }
}

private fun navigationUiFlags(darkStatusIcons: Boolean): Int {
    val flags = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
    return if (darkStatusIcons) flags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR else flags
}
// ----------------------------
// Click utils
// ----------------------------
fun View.tap(interval: Long = 200, action: (View) -> Unit) {
    setOnClickListener {
        if (System.currentTimeMillis() - DataLocal.lastClickTime >= interval) {
            action(it)
            DataLocal.lastClickTime = System.currentTimeMillis()
        }
    }
}

fun View.tapAndHold(action: () -> Unit) {
    val handler = Handler(Looper.getMainLooper())
    val repeatDelay = 50L
    val runnable = object : Runnable {
        override fun run() {
            if (!isEnabled) {
                handler.removeCallbacks(this)
                return
            }
            action()
            handler.postDelayed(this, repeatDelay)
        }
    }

    setOnTouchListener { _, event ->
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (!isEnabled) return@setOnTouchListener true
                action()
                handler.postDelayed(runnable, 400L)
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                handler.removeCallbacks(runnable)
            }
        }
        true
    }
}

fun View.tapWithSound(interval: Long = 500, action: (View) -> Unit) {
    setOnClickListener {
        if (System.currentTimeMillis() - DataLocal.lastClickTime >= interval) {
            SoundHelper.playSound(R.raw.touch)
            action(it)
            DataLocal.lastClickTime = System.currentTimeMillis()
        }
    }
}
fun Int.dp(context: Context): Int =
    context.resources.getIdentifier("dp_$this", "dimen", context.packageName).let { resourceId ->
        if (resourceId != 0) {
            context.resources.getDimensionPixelSize(resourceId)
        } else {
            val tabletScale = if (context.resources.configuration.smallestScreenWidthDp >= 600) 1.5f else 1f
            (this * context.resources.displayMetrics.density * tabletScale).roundToInt()
        }
    }
fun View.setMargins(
    left: Int? = null,
    top: Int? = null,
    right: Int? = null,
    bottom: Int? = null
) {
    val params = layoutParams as ViewGroup.MarginLayoutParams
    params.setMargins(
        left ?: params.leftMargin,
        top ?: params.topMargin,
        right ?: params.rightMargin,
        bottom ?: params.bottomMargin
    )
    layoutParams = params
}
// ----------------------------
// UI Capture
// ----------------------------
@Throws(OutOfMemoryError::class)
fun createBitmapFromView(view: View): Bitmap {
    return try {
        val output = createBitmap(view.width, view.height)
        val canvas = Canvas(output)
        view.draw(canvas)
        output
    } catch (error: OutOfMemoryError) {
        throw error
    }
}

// ----------------------------
// TextView
// ----------------------------
fun TextView.setFont(@FontRes resId: Int) {
    typeface = ResourcesCompat.getFont(context, resId)
}

fun TextView.setTextContent(context: Context, resId: Int) {
    text = context.getString(resId)
}

fun Context.strings(resId: Int) : String {
    return getString(resId)
}
fun Context.strings(resId: Int, value: String) : String {
    return getString(resId, value)
}
fun setImageActionBar(imageView: ImageView, res: Int) {
    imageView.setImageResource(res)
    imageView.visible()
}

fun setTextActionBar(textView: TextView, text: String) {
    textView.text = text
    textView.visible()
    textView.visible()
}

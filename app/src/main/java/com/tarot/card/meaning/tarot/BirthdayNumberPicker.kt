package com.tarot.card.meaning.tarot

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.EditText
import android.widget.NumberPicker
import com.tarot.card.meaning.R

class BirthdayNumberPicker @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.numberPickerStyle
) : NumberPicker(context, attrs, defStyleAttr) {
    private val selectionPaint = Paint()
    private val selectionHalfHeight = resources.getDimension(R.dimen.dp_20)
    private val showSelectedInput = Runnable {
        for (index in 0 until childCount) {
            (getChildAt(index) as? EditText)?.visibility = View.VISIBLE
        }
        invalidate()
    }

    init {
        setOnScrollListener { _, state ->
            removeCallbacks(showSelectedInput)
            if (state == OnScrollListener.SCROLL_STATE_IDLE) {
                postDelayed(showSelectedInput, 850L)
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN) removeCallbacks(showSelectedInput)
        val handled = super.onTouchEvent(event)
        if (event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_CANCEL) {
            removeCallbacks(showSelectedInput)
            postDelayed(showSelectedInput, 850L)
        }
        return handled
    }

    var selectedTextColor: Int = Color.BLACK
        set(value) {
            field = value
            selectionPaint.colorFilter = PorterDuffColorFilter(value, PorterDuff.Mode.SRC_IN)
            invalidate()
        }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val selectionTop = height / 2f - selectionHalfHeight
        val selectionBottom = height / 2f + selectionHalfHeight
        val checkpoint = canvas.save()
        canvas.clipRect(0f, selectionTop, width.toFloat(), selectionBottom)
        canvas.saveLayer(0f, selectionTop, width.toFloat(), selectionBottom, selectionPaint)
        super.onDraw(canvas)
        canvas.restoreToCount(checkpoint)
    }
}

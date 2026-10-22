package com.tarot.card.meaning.tarot

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.view.View
import android.widget.EditText
import androidx.appcompat.view.ContextThemeWrapper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.tarot.card.meaning.R
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.abs
import kotlin.math.roundToInt

@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 29)
class BirthdayWheelHighlightTest {
    @Test fun selectedTextStaysHighlightedWhenNativeInputIsHiddenForScrolling() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            for (dark in listOf(false, true)) {
                val context = ContextThemeWrapper(instrumentation.targetContext,
                    if (dark) R.style.TarotThemeDark else R.style.TarotThemeLight)
                val ui = TarotUi(context, dark)
                val wheel = BirthdayWheel(ui, "1994-11-14", "Scorpio")
                val density = context.resources.displayMetrics.density
                wheel.measure(View.MeasureSpec.makeMeasureSpec((320 * density).roundToInt(), View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
                wheel.layout(0, 0, wheel.measuredWidth, wheel.measuredHeight)
                for (pickerId in listOf(R.id.month, R.id.day, R.id.year)) {
                    val picker = wheel.findViewById<BirthdayNumberPicker>(pickerId)
                    for (childIndex in 0 until picker.childCount) {
                        (picker.getChildAt(childIndex) as? EditText)?.visibility = View.INVISIBLE
                    }
                    for (offsetDp in listOf(0, -6, 12, -6)) {
                        picker.scrollBy(0, (offsetDp * density).roundToInt())
                        val bitmap = Bitmap.createBitmap(picker.width, picker.height, Bitmap.Config.ARGB_8888)
                        try {
                            picker.draw(Canvas(bitmap))
                            val halfBand = (20 * density).roundToInt()
                            var highlightedPixels = 0
                            for (pixelY in picker.height / 2 - halfBand until picker.height / 2 + halfBand) {
                                for (pixelX in 0 until picker.width) {
                                    val pixel = bitmap.getPixel(pixelX, pixelY)
                                    if (Color.alpha(pixel) > 200 &&
                                        abs(Color.red(pixel) - Color.red(ui.ink)) <= 3 &&
                                        abs(Color.green(pixel) - Color.green(ui.ink)) <= 3 &&
                                        abs(Color.blue(pixel) - Color.blue(ui.ink)) <= 3) highlightedPixels++
                                }
                            }
                            assertTrue("Selected text lost highlight: picker=$pickerId, dark=$dark, offset=$offsetDp",
                                highlightedPixels > 5)
                        } finally {
                            bitmap.recycle()
                        }
                    }
                    assertTrue(wheel.valid())
                }
            }
        }
    }
}

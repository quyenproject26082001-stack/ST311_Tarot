package com.aaa.bbb.ccc.tarot

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.content.ContextCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.aaa.bbb.ccc.R
import com.aaa.bbb.ccc.databinding.ActivityTarotSettingsBinding
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.roundToInt
import kotlin.math.abs

@RunWith(AndroidJUnit4::class)
class ReverseChanceSeekBarTest {
    @Test fun progressRemainsContinuousUnderTheThumb() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            for (theme in listOf(R.style.TarotThemeLight, R.style.TarotThemeDark)) {
                val context = ContextThemeWrapper(instrumentation.targetContext, theme)
                val seekBar = ActivityTarotSettingsBinding.inflate(LayoutInflater.from(context)).reverseChance
                assertFalse(seekBar.splitTrack)
                val density = context.resources.displayMetrics.density
                val green = ContextCompat.getColor(context, R.color.tarot_green)
                for (widthDp in listOf(280, 480)) {
                    val width = (widthDp * density).roundToInt()
                    val height = (32 * density).roundToInt()
                    seekBar.measure(
                        View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)
                    )
                    seekBar.layout(0, 0, width, height)
                    for (progress in listOf(25, 50, 75)) {
                        seekBar.progress = progress
                        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        try {
                            seekBar.draw(Canvas(bitmap))
                            val thumbCenter = seekBar.thumb.bounds.centerX() + seekBar.paddingLeft - seekBar.thumbOffset
                            val sampleTop = height / 2 - (3 * density).roundToInt()
                            val sampleStart = thumbCenter - (12 * density).roundToInt()
                            for (sampleX in sampleStart until thumbCenter) {
                                val pixel = bitmap.getPixel(sampleX, sampleTop)
                                val continuous = Color.alpha(pixel) >= 253 &&
                                    abs(Color.red(pixel) - Color.red(green)) <= 2 &&
                                    abs(Color.green(pixel) - Color.green(green)) <= 2 &&
                                    abs(Color.blue(pixel) - Color.blue(green)) <= 2
                                assertTrue("Gap at progress=$progress, width=$widthDp, x=$sampleX, color=$pixel", continuous)
                            }
                        } finally {
                            bitmap.recycle()
                        }
                    }
                }
            }
        }
    }
}

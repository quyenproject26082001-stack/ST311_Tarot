package com.tarot.card.meaning.tarot

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.util.LruCache
import android.widget.ImageView
import androidx.annotation.DrawableRes

private val darkArtwork = LruCache<Int, Bitmap>(4)

fun ImageView.applyTarotArtwork(@DrawableRes resource: Int, dark: Boolean) {
    if (!dark) {
        setImageResource(resource)
        return
    }
    val bitmap = darkArtwork.get(resource) ?: run {
        val source = BitmapFactory.decodeResource(resources, resource)
        val width = source.width
        val height = source.height
        val pixels = IntArray(width * height)
        source.getPixels(pixels, 0, width, 0, 0, width, height)
        val visited = BooleanArray(pixels.size)
        val queue = IntArray(pixels.size)
        var head = 0
        var tail = 0
        fun enqueue(index: Int) {
            if (visited[index]) return
            visited[index] = true
            val pixel = pixels[index]
            if (Color.alpha(pixel) == 0 ||
                minOf(Color.red(pixel), Color.green(pixel), Color.blue(pixel)) >= 240) {
                queue[tail++] = index
            }
        }
        for (column in 0 until width) {
            enqueue(column)
            enqueue((height - 1) * width + column)
        }
        for (row in 0 until height) {
            enqueue(row * width)
            enqueue(row * width + width - 1)
        }
        while (head < tail) {
            val index = queue[head++]
            pixels[index] = Color.TRANSPARENT
            if (index % width > 0) enqueue(index - 1)
            if (index % width < width - 1) enqueue(index + 1)
            if (index >= width) enqueue(index - width)
            if (index + width < pixels.size) enqueue(index + width)
        }
        Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888).also {
            darkArtwork.put(resource, it)
        }
    }
    setImageBitmap(bitmap)
}

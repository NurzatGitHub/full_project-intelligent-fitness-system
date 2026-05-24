package com.example.fitnesscoachai.ui.workout.shared

import android.graphics.Bitmap
import androidx.camera.core.ImageProxy
import java.nio.ByteBuffer

object RgbaToBitmap {

    fun toBitmap(image: ImageProxy): Bitmap {
        val plane = image.planes[0]
        val buffer = plane.buffer
        val rowStride = plane.rowStride
        val pixelStride = plane.pixelStride // обычно 4
        val width = image.width
        val height = image.height

        buffer.rewind()

        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        // если нет паддинга — быстро
        if (rowStride == width * pixelStride) {
            bmp.copyPixelsFromBuffer(buffer)
            return bmp
        }

        // если есть паддинг — копируем построчно
        val row = ByteArray(rowStride)
        val pixels = IntArray(width)

        for (y in 0 until height) {
            buffer.get(row, 0, rowStride)

            var idx = 0
            for (x in 0 until width) {
                val r = row[idx].toInt() and 0xFF
                val g = row[idx + 1].toInt() and 0xFF
                val b = row[idx + 2].toInt() and 0xFF
                val a = row[idx + 3].toInt() and 0xFF
                pixels[x] = (a shl 24) or (r shl 16) or (g shl 8) or b
                idx += pixelStride
            }
            bmp.setPixels(pixels, 0, width, 0, y, width, 1)
        }
        return bmp
    }
}
package com.example.fitnesscoachai.ui.workout.shared

import android.graphics.*
import androidx.camera.core.ImageProxy
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

/**
 * Рабочий конвертер ImageProxy (YUV_420_888) -> Bitmap.
 * Это НЕ самый быстрый способ (через JPEG), но 100% компилируется без NDK.
 * Потом заменим на libyuv для скорости.
 */
object ImageProxyToBitmapConverter {

    fun toBitmap(image: ImageProxy): Bitmap? {
        return try {
            val nv21 = yuv420888ToNv21(image)
            val yuvImage = YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)

            val out = ByteArrayOutputStream()
            yuvImage.compressToJpeg(Rect(0, 0, image.width, image.height), 80, out)
            val jpegBytes = out.toByteArray()

            BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size)
        } catch (e: Exception) {
            null
        }
    }

    // --- helpers ---

    private fun yuv420888ToNv21(image: ImageProxy): ByteArray {
        val width = image.width
        val height = image.height

        val yPlane = image.planes[0]
        val uPlane = image.planes[1]
        val vPlane = image.planes[2]

        val ySize = width * height
        val uvSize = width * height / 2
        val out = ByteArray(ySize + uvSize)

        // Copy Y
        copyPlane(
            planeBuffer = yPlane.buffer,
            rowStride = yPlane.rowStride,
            pixelStride = yPlane.pixelStride,
            width = width,
            height = height,
            out = out,
            outOffset = 0,
            outPixelStride = 1
        )

        // NV21 = Y + VU interleaved
        // Copy V then U into the same uv buffer (interleaved)
        val uvHeight = height / 2
        val uvWidth = width / 2

        // V
        copyPlaneInterleavedVU(
            planeBuffer = vPlane.buffer,
            rowStride = vPlane.rowStride,
            pixelStride = vPlane.pixelStride,
            width = uvWidth,
            height = uvHeight,
            out = out,
            outOffset = ySize,
            outStep = 2,          // V every 2 bytes
            outStart = 0          // write at [ySize + 0], [ySize + 2], ...
        )

        // U
        copyPlaneInterleavedVU(
            planeBuffer = uPlane.buffer,
            rowStride = uPlane.rowStride,
            pixelStride = uPlane.pixelStride,
            width = uvWidth,
            height = uvHeight,
            out = out,
            outOffset = ySize,
            outStep = 2,          // U every 2 bytes
            outStart = 1          // write at [ySize + 1], [ySize + 3], ...
        )

        return out
    }

    private fun copyPlane(
        planeBuffer: ByteBuffer,
        rowStride: Int,
        pixelStride: Int,
        width: Int,
        height: Int,
        out: ByteArray,
        outOffset: Int,
        outPixelStride: Int
    ) {
        planeBuffer.rewind()
        var outIndex = outOffset

        val row = ByteArray(rowStride)
        for (y in 0 until height) {
            planeBuffer.get(row, 0, rowStride)
            var colIndex = 0
            for (x in 0 until width) {
                out[outIndex] = row[colIndex]
                outIndex += outPixelStride
                colIndex += pixelStride
            }
        }
    }

    private fun copyPlaneInterleavedVU(
        planeBuffer: ByteBuffer,
        rowStride: Int,
        pixelStride: Int,
        width: Int,
        height: Int,
        out: ByteArray,
        outOffset: Int,
        outStep: Int,
        outStart: Int
    ) {
        planeBuffer.rewind()
        val row = ByteArray(rowStride)
        var outIndexBase: Int

        for (y in 0 until height) {
            planeBuffer.get(row, 0, rowStride)
            outIndexBase = outOffset + y * width * 2

            var colIndex = 0
            for (x in 0 until width) {
                out[outIndexBase + outStart + x * outStep] = row[colIndex]
                colIndex += pixelStride
            }
        }
    }
}
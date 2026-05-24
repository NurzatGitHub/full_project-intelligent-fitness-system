package com.example.fitnesscoachai.ui.workout.shared

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.math.max

class OverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val paintLine = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 8f
        strokeCap = Paint.Cap.ROUND
    }

    private val paintPoint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.WHITE
    }

    private var points: List<PosePoint>? = null
    private var segments: List<Segment> = emptyList()

    var mirrorX: Boolean = false

    private var imageW: Int = 1
    private var imageH: Int = 1

    fun setImageSize(w: Int, h: Int) {
        imageW = max(1, w)
        imageH = max(1, h)
    }

    fun updatePose(points: List<PosePoint>?, segments: List<Segment>) {
        this.points = points
        this.segments = segments
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val pts = points ?: return
        if (pts.size < 18) return

        val viewW = width.toFloat()
        val viewH = height.toFloat()
        val imgW = imageW.toFloat()
        val imgH = imageH.toFloat()

        // center-crop (как PreviewView FILL_CENTER)
        val scale = max(viewW / imgW, viewH / imgH)
        val scaledW = imgW * scale
        val scaledH = imgH * scale
        val dx = (scaledW - viewW) / 2f
        val dy = (scaledH - viewH) / 2f

        fun mapX(xNorm: Float): Float {
            var xn = xNorm.coerceIn(0f, 1f)
            if (mirrorX) xn = 1f - xn
            val xPx = xn * imgW
            return xPx * scale - dx
        }

        fun mapY(yNorm: Float): Float {
            val yn = yNorm.coerceIn(0f, 1f)
            val yPx = yn * imgH
            return yPx * scale - dy
        }

        // линии
        for (s in segments) {
            if (s.a !in pts.indices || s.b !in pts.indices) continue
            paintLine.color = runCatching { Color.parseColor(s.color) }.getOrElse { Color.GREEN }

            val a = pts[s.a]
            val b = pts[s.b]
            canvas.drawLine(
                mapX(a.x), mapY(a.y),
                mapX(b.x), mapY(b.y),
                paintLine
            )
        }

        // точки
        val r = max(6f, viewW * 0.008f)
        for (p in pts) {
            canvas.drawCircle(mapX(p.x), mapY(p.y), r, paintPoint)
        }
    }
}
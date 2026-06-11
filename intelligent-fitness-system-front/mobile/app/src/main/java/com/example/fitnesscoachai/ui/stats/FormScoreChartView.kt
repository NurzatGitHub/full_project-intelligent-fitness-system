package com.example.fitnesscoachai.ui.stats

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import com.example.fitnesscoachai.data.models.FormScorePoint

/**
 * Vertical bar chart of the user's form-score history.
 *
 * Why hand-rolled (instead of MPAndroidChart): we only need a single series
 * with brand-tinted gradient bars, axis ticks, and a small "AVG" marker.
 * Pulling in a 1MB chart library just for that would bloat the APK. Drawing
 * it ourselves on a Canvas is ~120 lines and matches our design tokens
 * exactly.
 *
 * Data is set with [setData]. Calling it again triggers a redraw.
 *
 * Visual reference: Apple Watch Heart-Rate "vertical bar with avg line" chart.
 */
class FormScoreChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private var points: List<FormScorePoint> = emptyList()
    private var avgScore: Float = 0f

    // ----- Paints --------------------------------------------------------

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val axisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x33FFFFFF
        strokeWidth = dp(1f)
        style = Paint.Style.STROKE
    }

    private val avgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFCB73.toInt()  // warm amber so it pops against purple bars
        strokeWidth = dp(2f)
        style = Paint.Style.STROKE
        pathEffect = android.graphics.DashPathEffect(floatArrayOf(dp(6f), dp(4f)), 0f)
    }

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xAAFFFFFF.toInt()
        textSize = dp(11f) * resources.displayMetrics.scaledDensity / resources.displayMetrics.density
        isFakeBoldText = true
    }

    private val avgLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFCB73.toInt()
        textSize = dp(11f) * resources.displayMetrics.scaledDensity / resources.displayMetrics.density
        isFakeBoldText = true
    }

    private val emptyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xAAFFFFFF.toInt()
        textSize = dp(13f) * resources.displayMetrics.scaledDensity / resources.displayMetrics.density
        textAlign = Paint.Align.CENTER
    }

    // ----- Public API ----------------------------------------------------

    fun setData(history: List<FormScorePoint>, avg: Float) {
        // Backend already sends chronological order (oldest → newest) — keep
        // it as is so the latest workout sits on the right edge.
        this.points = history
        this.avgScore = avg
        invalidate()
    }

    // ----- Drawing -------------------------------------------------------

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()
        val padL = dp(28f)
        val padR = dp(12f)
        val padT = dp(20f)
        val padB = dp(28f)

        val plotLeft = padL
        val plotRight = w - padR
        val plotTop = padT
        val plotBottom = h - padB
        val plotH = plotBottom - plotTop

        // ----- Empty state ----------
        if (points.isEmpty()) {
            canvas.drawText(
                "No workouts scored yet",
                w / 2f, h / 2f, emptyPaint,
            )
            return
        }

        // ----- Y axis ticks (0 / 50 / 100) ----------
        for (tick in intArrayOf(0, 50, 100)) {
            val y = plotBottom - (tick / 100f) * plotH
            canvas.drawLine(plotLeft, y, plotRight, y, axisPaint)
            canvas.drawText(tick.toString(), dp(4f), y + dp(4f), labelPaint)
        }

        // ----- Bars ----------
        val slotWidth = (plotRight - plotLeft) / points.size
        val barWidth = (slotWidth * 0.55f).coerceAtLeast(dp(4f))
        val cornerRadius = dp(3f)

        for ((index, point) in points.withIndex()) {
            val score = point.form_score.coerceIn(0f, 100f)
            val barH = (score / 100f) * plotH
            val cx = plotLeft + slotWidth * (index + 0.5f)
            val barLeft = cx - barWidth / 2f
            val barRight = cx + barWidth / 2f
            val barTop = plotBottom - barH

            // Gradient tint per quality zone.
            val (topColor, bottomColor) = colorsFor(score)
            barPaint.shader = LinearGradient(
                cx, barTop, cx, plotBottom,
                topColor, bottomColor,
                Shader.TileMode.CLAMP,
            )

            val rect = RectF(barLeft, barTop, barRight, plotBottom)
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, barPaint)
            barPaint.shader = null
        }

        // ----- Avg line (only when meaningful) ----------
        if (avgScore > 0f) {
            val avgY = plotBottom - (avgScore.coerceIn(0f, 100f) / 100f) * plotH
            val path = Path().apply {
                moveTo(plotLeft, avgY)
                lineTo(plotRight, avgY)
            }
            canvas.drawPath(path, avgPaint)
            canvas.drawText(
                "AVG ${avgScore.toInt()}",
                plotRight - dp(58f),
                avgY - dp(6f),
                avgLabelPaint,
            )
        }
    }

    /**
     * Color stops for the bar gradient. Mirrors the same thresholds the
     * backend uses to bucket sessions into Excellent / Good / Average / Poor.
     */
    private fun colorsFor(score: Float): Pair<Int, Int> {
        return when {
            score >= 85 -> 0xFF00E5A8.toInt() to 0xFF00B383.toInt()   // mint (excellent)
            score >= 70 -> 0xFF7C5CFF.toInt() to 0xFF5435C7.toInt()   // brand purple (good)
            score >= 50 -> 0xFFFFCB73.toInt() to 0xFFE0A040.toInt()   // amber (avg)
            else        -> 0xFFFF7A59.toInt() to 0xFFCC4D33.toInt()   // coral (poor)
        }
    }

    private fun dp(value: Float): Float {
        return value * resources.displayMetrics.density
    }
}

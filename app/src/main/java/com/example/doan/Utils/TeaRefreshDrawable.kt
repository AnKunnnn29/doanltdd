package com.example.doan.Utils

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.view.animation.LinearInterpolator
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

/**
 * 🍵 TeaRefreshDrawable - Custom drawable cho Pull-to-Refresh
 * Hiển thị animation ly trà đang được pha
 */
class TeaRefreshDrawable(private val context: Context) : Drawable(), Animatable {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val cupPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val teaPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val steamPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    
    private var progress = 0f
    private var isRunning = false
    private var animator: ValueAnimator? = null
    private var steamOffset = 0f

    // Colors
    private val cupColor = Color.parseColor("#931923") // Wine primary
    private val teaColor = Color.parseColor("#B8293A") // Wine accent
    private val steamColor = Color.parseColor("#FFFFFF")

    init {
        cupPaint.apply {
            color = cupColor
            style = Paint.Style.STROKE
            strokeWidth = 8f
            strokeCap = Paint.Cap.ROUND
        }

        teaPaint.apply {
            color = teaColor
            style = Paint.Style.FILL
        }

        steamPaint.apply {
            color = steamColor
            style = Paint.Style.STROKE
            strokeWidth = 4f
            strokeCap = Paint.Cap.ROUND
            alpha = 150
        }
    }

    override fun draw(canvas: Canvas) {
        val bounds = bounds
        val centerX = bounds.centerX().toFloat()
        val centerY = bounds.centerY().toFloat()
        val size = minOf(bounds.width(), bounds.height()) * 0.4f

        // Vẽ ly trà
        drawCup(canvas, centerX, centerY, size)
        
        // Vẽ trà trong ly (theo progress)
        drawTea(canvas, centerX, centerY, size)
        
        // Vẽ hơi nước (khi đang refresh)
        if (isRunning) {
            drawSteam(canvas, centerX, centerY, size)
        }
    }

    private fun drawCup(canvas: Canvas, cx: Float, cy: Float, size: Float) {
        val cupPath = Path()
        
        // Thân ly (hình thang)
        val topWidth = size * 0.8f
        val bottomWidth = size * 0.6f
        val height = size * 0.7f
        
        cupPath.moveTo(cx - topWidth / 2, cy - height / 2)
        cupPath.lineTo(cx - bottomWidth / 2, cy + height / 2)
        cupPath.lineTo(cx + bottomWidth / 2, cy + height / 2)
        cupPath.lineTo(cx + topWidth / 2, cy - height / 2)
        
        canvas.drawPath(cupPath, cupPaint)
        
        // Quai ly
        val handlePath = Path()
        handlePath.moveTo(cx + topWidth / 2, cy - height / 4)
        handlePath.quadTo(
            cx + topWidth / 2 + size * 0.3f, cy,
            cx + bottomWidth / 2 + size * 0.1f, cy + height / 4
        )
        canvas.drawPath(handlePath, cupPaint)
        
        // Đĩa lót
        canvas.drawLine(
            cx - size * 0.5f, cy + height / 2 + 10,
            cx + size * 0.5f, cy + height / 2 + 10,
            cupPaint
        )
    }

    private fun drawTea(canvas: Canvas, cx: Float, cy: Float, size: Float) {
        val topWidth = size * 0.8f
        val bottomWidth = size * 0.6f
        val height = size * 0.7f
        
        // Tính toán mức trà dựa trên progress
        val teaLevel = progress.coerceIn(0f, 1f)
        val teaHeight = height * teaLevel * 0.8f
        
        if (teaLevel > 0) {
            val teaPath = Path()
            
            // Tính width tại mức trà hiện tại
            val currentWidth = bottomWidth + (topWidth - bottomWidth) * teaLevel * 0.8f
            val teaTop = cy + height / 2 - teaHeight
            
            teaPath.moveTo(cx - currentWidth / 2 + 8, teaTop)
            teaPath.lineTo(cx - bottomWidth / 2 + 8, cy + height / 2 - 4)
            teaPath.lineTo(cx + bottomWidth / 2 - 8, cy + height / 2 - 4)
            teaPath.lineTo(cx + currentWidth / 2 - 8, teaTop)
            teaPath.close()
            
            // Wave effect
            if (isRunning) {
                teaPaint.alpha = 200
            } else {
                teaPaint.alpha = 255
            }
            
            canvas.drawPath(teaPath, teaPaint)
        }
    }

    private fun drawSteam(canvas: Canvas, cx: Float, cy: Float, size: Float) {
        val height = size * 0.7f
        val steamStartY = cy - height / 2 - 10
        
        // Vẽ 3 đường hơi nước
        for (i in 0..2) {
            val offsetX = (i - 1) * size * 0.2f
            val phase = steamOffset + i * 0.3f
            
            val steamPath = Path()
            steamPath.moveTo(cx + offsetX, steamStartY)
            
            // Đường cong hơi nước
            val waveAmplitude = 8f
            val waveLength = 20f
            
            for (y in 0..30 step 2) {
                val yPos = steamStartY - y
                val xOffset = Math.sin((y / waveLength + phase).toDouble()).toFloat() * waveAmplitude
                steamPath.lineTo(cx + offsetX + xOffset, yPos)
            }
            
            steamPaint.alpha = (150 * (1 - (steamOffset % 1))).toInt().coerceIn(50, 150)
            canvas.drawPath(steamPath, steamPaint)
        }
    }

    fun setProgress(progress: Float) {
        this.progress = progress
        invalidateSelf()
    }

    override fun start() {
        if (isRunning) return
        isRunning = true
        
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1000
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                steamOffset = it.animatedValue as Float
                invalidateSelf()
            }
            start()
        }
    }

    override fun stop() {
        isRunning = false
        animator?.cancel()
        animator = null
        invalidateSelf()
    }

    override fun isRunning(): Boolean = isRunning

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
    }

    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}

/**
 * Extension function để setup SwipeRefreshLayout với Tea theme
 */
fun SwipeRefreshLayout.setupTeaRefresh() {
    setColorSchemeColors(
        Color.parseColor("#931923"), // Wine primary
        Color.parseColor("#B8293A"), // Wine accent
        Color.parseColor("#FFD700")  // Gold
    )
    setProgressBackgroundColorSchemeColor(Color.WHITE)
}

package com.example.doan.Views

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class SpinWheelView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val colors = listOf(
        Color.parseColor("#95A5A6"),  // 0% - Xám
        Color.parseColor("#3498DB"),  // 10% - Xanh dương
        Color.parseColor("#2ECC71"),  // 20% - Xanh lá
        Color.parseColor("#F39C12"),  // 50% - Cam
        Color.parseColor("#E74C3C")   // 100% - Đỏ
    )

    private val labels = listOf("0%", "10%", "20%", "50%", "100%")
    
    private var currentRotation = 0f
    private var isSpinning = false
    private var onSpinComplete: ((Int) -> Unit)? = null

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#34495E")
        style = Paint.Style.STROKE
        strokeWidth = 12f
    }

    private var centerX = 0f
    private var centerY = 0f
    private var radius = 0f

    fun spin(targetIndex: Int, onComplete: (Int) -> Unit) {
        if (isSpinning) return
        
        isSpinning = true
        onSpinComplete = onComplete

        val sweepAngle = 360f / labels.size
        val targetAngle = 360f - (targetIndex * sweepAngle + sweepAngle / 2)
        val totalRotation = 360f * 6 + targetAngle - (currentRotation % 360)

        val animator = ValueAnimator.ofFloat(currentRotation, currentRotation + totalRotation)
        animator.duration = 5000
        animator.interpolator = DecelerateInterpolator(1.8f)
        
        animator.addUpdateListener { animation ->
            currentRotation = animation.animatedValue as Float
            invalidate()
        }
        
        animator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                isSpinning = false
                onSpinComplete?.invoke(targetIndex)
            }
        })
        
        animator.start()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        centerX = w / 2f
        centerY = h / 2f
        radius = min(w, h) / 2f - 30f
        textPaint.textSize = radius / 4
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.save()
        canvas.rotate(currentRotation, centerX, centerY)

        val sweepAngle = 360f / labels.size
        var startAngle = -90f - sweepAngle / 2

        for (i in labels.indices) {
            // Vẽ phần bánh
            paint.color = colors[i]
            canvas.drawArc(
                centerX - radius, centerY - radius,
                centerX + radius, centerY + radius,
                startAngle, sweepAngle, true, paint
            )

            // Vẽ text
            val textAngle = Math.toRadians((startAngle + sweepAngle / 2).toDouble())
            val textRadius = radius * 0.6f
            val textX = centerX + (textRadius * cos(textAngle)).toFloat()
            val textY = centerY + (textRadius * sin(textAngle)).toFloat()

            canvas.save()
            canvas.rotate(startAngle + sweepAngle / 2 + 90, textX, textY)
            canvas.drawText(labels[i], textX, textY + 10, textPaint)
            canvas.restore()

            startAngle += sweepAngle
        }

        canvas.restore()

        // Vẽ viền
        canvas.drawCircle(centerX, centerY, radius, borderPaint)

        // Vẽ tâm
        paint.color = Color.parseColor("#2C3E50")
        canvas.drawCircle(centerX, centerY, radius / 5, paint)
        paint.color = Color.parseColor("#ECF0F1")
        canvas.drawCircle(centerX, centerY, radius / 7, paint)

        // Vẽ kim chỉ
        drawPointer(canvas)
    }

    private fun drawPointer(canvas: Canvas) {
        val pointerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E74C3C")
            style = Paint.Style.FILL
        }
        
        val pointerPath = Path()
        val pointerSize = radius / 4
        
        pointerPath.moveTo(centerX, centerY - radius + 5)
        pointerPath.lineTo(centerX - pointerSize / 2, centerY - radius - pointerSize)
        pointerPath.lineTo(centerX + pointerSize / 2, centerY - radius - pointerSize)
        pointerPath.close()
        
        canvas.drawPath(pointerPath, pointerPaint)
        
        // Viền kim
        pointerPaint.color = Color.parseColor("#C0392B")
        pointerPaint.style = Paint.Style.STROKE
        pointerPaint.strokeWidth = 3f
        canvas.drawPath(pointerPath, pointerPaint)
    }

    fun isSpinning() = isSpinning
}

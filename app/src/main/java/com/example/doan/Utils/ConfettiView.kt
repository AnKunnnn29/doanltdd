package com.example.doan.Utils

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateInterpolator
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * 🎉 ConfettiView - Hiệu ứng confetti khi đặt hàng thành công
 * 
 * Sử dụng:
 * confettiView.startConfetti() // Bắt đầu animation
 */
class ConfettiView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val confettiPieces = mutableListOf<ConfettiPiece>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var animator: ValueAnimator? = null
    private var isAnimating = false

    // Màu confetti theo theme wine
    private val confettiColors = listOf(
        Color.parseColor("#931923"), // Wine primary
        Color.parseColor("#B8293A"), // Wine accent
        Color.parseColor("#FFD700"), // Gold
        Color.parseColor("#FF6B6B"), // Light red
        Color.parseColor("#4ECDC4"), // Teal
        Color.parseColor("#FFE66D"), // Yellow
        Color.parseColor("#95E1D3"), // Mint
        Color.parseColor("#F38181")  // Coral
    )

    data class ConfettiPiece(
        var x: Float,
        var y: Float,
        var velocityX: Float,
        var velocityY: Float,
        var rotation: Float,
        var rotationSpeed: Float,
        var color: Int,
        var size: Float,
        var shape: Shape,
        var alpha: Float = 1f
    )

    enum class Shape { RECTANGLE, CIRCLE, TRIANGLE, STAR }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (!isAnimating) return

        confettiPieces.forEach { piece ->
            paint.color = piece.color
            paint.alpha = (piece.alpha * 255).toInt()

            canvas.save()
            canvas.translate(piece.x, piece.y)
            canvas.rotate(piece.rotation)

            when (piece.shape) {
                Shape.RECTANGLE -> {
                    canvas.drawRect(
                        -piece.size / 2, -piece.size / 4,
                        piece.size / 2, piece.size / 4,
                        paint
                    )
                }
                Shape.CIRCLE -> {
                    canvas.drawCircle(0f, 0f, piece.size / 2, paint)
                }
                Shape.TRIANGLE -> {
                    val path = Path()
                    path.moveTo(0f, -piece.size / 2)
                    path.lineTo(-piece.size / 2, piece.size / 2)
                    path.lineTo(piece.size / 2, piece.size / 2)
                    path.close()
                    canvas.drawPath(path, paint)
                }
                Shape.STAR -> {
                    drawStar(canvas, piece.size / 2, paint)
                }
            }

            canvas.restore()
        }
    }

    private fun drawStar(canvas: Canvas, radius: Float, paint: Paint) {
        val path = Path()
        val innerRadius = radius * 0.4f
        for (i in 0 until 10) {
            val r = if (i % 2 == 0) radius else innerRadius
            val angle = Math.PI * i / 5 - Math.PI / 2
            val x = (r * cos(angle)).toFloat()
            val y = (r * sin(angle)).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        canvas.drawPath(path, paint)
    }

    /**
     * 🎊 Bắt đầu hiệu ứng confetti
     * @param duration Thời gian animation (ms), mặc định 3000ms
     * @param pieceCount Số lượng confetti, mặc định 150
     */
    fun startConfetti(duration: Long = 3000L, pieceCount: Int = 150) {
        if (isAnimating) return

        visibility = VISIBLE
        isAnimating = true
        confettiPieces.clear()

        // Tạo confetti từ giữa trên màn hình
        val centerX = width / 2f
        val startY = 0f

        repeat(pieceCount) {
            val angle = Random.nextFloat() * Math.PI.toFloat() - Math.PI.toFloat() / 2
            val speed = Random.nextFloat() * 15 + 8

            confettiPieces.add(
                ConfettiPiece(
                    x = centerX + Random.nextFloat() * 200 - 100,
                    y = startY - Random.nextFloat() * 100,
                    velocityX = cos(angle) * speed * (if (Random.nextBoolean()) 1 else -1),
                    velocityY = sin(angle) * speed + Random.nextFloat() * 5,
                    rotation = Random.nextFloat() * 360,
                    rotationSpeed = Random.nextFloat() * 20 - 10,
                    color = confettiColors.random(),
                    size = Random.nextFloat() * 15 + 8,
                    shape = Shape.values().random()
                )
            )
        }

        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            this.duration = duration
            interpolator = AccelerateInterpolator(0.5f)
            addUpdateListener {
                updateConfetti()
                invalidate()
            }
            start()
        }

        postDelayed({
            stopConfetti()
        }, duration)
    }

    private fun updateConfetti() {
        val gravity = 0.3f
        val friction = 0.99f

        confettiPieces.forEach { piece ->
            piece.velocityY += gravity
            piece.velocityX *= friction
            piece.x += piece.velocityX
            piece.y += piece.velocityY
            piece.rotation += piece.rotationSpeed

            // Fade out khi gần cuối
            if (piece.y > height * 0.7f) {
                piece.alpha = maxOf(0f, piece.alpha - 0.02f)
            }
        }

        // Xóa confetti đã ra khỏi màn hình
        confettiPieces.removeAll { it.y > height + 50 || it.alpha <= 0 }
    }

    /**
     * Dừng hiệu ứng confetti
     */
    fun stopConfetti() {
        isAnimating = false
        animator?.cancel()
        confettiPieces.clear()
        visibility = GONE
        invalidate()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator?.cancel()
    }
}

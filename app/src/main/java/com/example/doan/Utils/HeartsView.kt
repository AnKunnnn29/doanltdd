package com.example.doan.Utils

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.sin
import kotlin.random.Random

/**
 * 💕 HeartsView - Hiệu ứng trái tim bay (Valentine)
 */
class HeartsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val hearts = mutableListOf<Heart>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var isAnimating = false
    private val heartCount = 30

    private val heartColors = listOf(
        Color.parseColor("#FF6B81"),  // Hồng đậm
        Color.parseColor("#FF4757"),  // Đỏ hồng
        Color.parseColor("#FF7F8A"),  // Hồng nhạt
        Color.parseColor("#E84393"),  // Magenta
        Color.parseColor("#FD79A8"),  // Pink pastel
        Color.parseColor("#FF1744"),  // Đỏ tươi
        Color.parseColor("#FFFFFF")   // Trắng
    )

    data class Heart(
        var x: Float,
        var y: Float,
        var size: Float,
        var rotation: Float,
        var rotationSpeed: Float,
        var speedY: Float,
        var speedX: Float,
        var swayPhase: Float,
        var color: Int,
        var alpha: Int,
        var scale: Float
    )

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (hearts.isEmpty() && w > 0 && h > 0) {
            initHearts()
        }
    }

    private fun initHearts() {
        hearts.clear()
        repeat(heartCount) {
            hearts.add(createHeart(randomY = true))
        }
    }

    private fun createHeart(randomY: Boolean = false): Heart {
        return Heart(
            x = Random.nextFloat() * width,
            y = if (randomY) Random.nextFloat() * height else height + Random.nextFloat() * 100f,
            size = Random.nextFloat() * 18f + 12f,
            rotation = Random.nextFloat() * 30f - 15f,
            rotationSpeed = Random.nextFloat() * 2f - 1f,
            speedY = -(Random.nextFloat() * 2f + 1.5f), // Bay lên
            speedX = Random.nextFloat() * 1f - 0.5f,
            swayPhase = Random.nextFloat() * Math.PI.toFloat() * 2,
            color = heartColors.random(),
            alpha = Random.nextInt(150, 255),
            scale = Random.nextFloat() * 0.5f + 0.7f
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!isAnimating) return

        hearts.forEach { heart ->
            drawHeart(canvas, heart)
            updateHeart(heart)
        }

        postInvalidateOnAnimation()
    }


    private fun drawHeart(canvas: Canvas, heart: Heart) {
        canvas.save()
        canvas.translate(heart.x, heart.y)
        canvas.rotate(heart.rotation)
        canvas.scale(heart.scale, heart.scale)

        paint.color = heart.color
        paint.alpha = heart.alpha
        paint.style = Paint.Style.FILL

        // Vẽ trái tim
        val path = Path()
        val size = heart.size
        
        path.moveTo(0f, size * 0.3f)
        // Bên trái
        path.cubicTo(
            -size * 0.5f, -size * 0.3f,
            -size, size * 0.1f,
            0f, size
        )
        // Bên phải
        path.cubicTo(
            size, size * 0.1f,
            size * 0.5f, -size * 0.3f,
            0f, size * 0.3f
        )
        path.close()

        canvas.drawPath(path, paint)
        
        // Thêm highlight
        paint.alpha = (heart.alpha * 0.4f).toInt()
        paint.color = Color.WHITE
        canvas.drawCircle(-size * 0.25f, size * 0.1f, size * 0.15f, paint)
        
        canvas.restore()
    }

    private fun updateHeart(heart: Heart) {
        heart.swayPhase += 0.04f
        heart.x += heart.speedX + sin(heart.swayPhase) * 0.8f
        heart.y += heart.speedY
        heart.rotation += heart.rotationSpeed
        
        // Fade out khi bay lên cao
        if (heart.y < height * 0.3f) {
            heart.alpha = (heart.alpha * 0.98f).toInt().coerceAtLeast(0)
        }

        if (heart.y < -50 || heart.alpha <= 0) {
            resetHeart(heart)
        }
        if (heart.x < -50) heart.x = width + 50f
        if (heart.x > width + 50) heart.x = -50f
    }

    private fun resetHeart(heart: Heart) {
        heart.x = Random.nextFloat() * width
        heart.y = height + Random.nextFloat() * 100f
        heart.size = Random.nextFloat() * 18f + 12f
        heart.speedY = -(Random.nextFloat() * 2f + 1.5f)
        heart.alpha = Random.nextInt(150, 255)
        heart.color = heartColors.random()
        heart.scale = Random.nextFloat() * 0.5f + 0.7f
    }

    fun startFloating() {
        isAnimating = true
        if (hearts.isEmpty() && width > 0) initHearts()
        invalidate()
    }

    fun stopFloating() {
        isAnimating = false
    }

    fun isFloating(): Boolean = isAnimating
}

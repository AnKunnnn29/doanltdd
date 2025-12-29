package com.example.doan.Utils

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * ☀️ SunshineView - Hiệu ứng bong bóng/ánh nắng mùa hè
 */
class SunshineView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val bubbles = mutableListOf<Bubble>()
    private val sunRays = mutableListOf<SunRay>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var isAnimating = false
    private val bubbleCount = 25
    private val rayCount = 8
    private var rayRotation = 0f

    private val bubbleColors = listOf(
        Color.parseColor("#87CEEB"),  // Sky blue
        Color.parseColor("#00CED1"),  // Dark turquoise
        Color.parseColor("#48D1CC"),  // Medium turquoise
        Color.parseColor("#40E0D0"),  // Turquoise
        Color.parseColor("#7FFFD4"),  // Aquamarine
        Color.parseColor("#AFEEEE"),  // Pale turquoise
        Color.parseColor("#E0FFFF")   // Light cyan
    )

    data class Bubble(
        var x: Float,
        var y: Float,
        var radius: Float,
        var speedY: Float,
        var speedX: Float,
        var wobblePhase: Float,
        var color: Int,
        var alpha: Int
    )

    data class SunRay(
        var angle: Float,
        var length: Float,
        var alpha: Int
    )

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (bubbles.isEmpty() && w > 0 && h > 0) {
            initBubbles()
            initSunRays()
        }
    }

    private fun initBubbles() {
        bubbles.clear()
        repeat(bubbleCount) {
            bubbles.add(createBubble(randomY = true))
        }
    }

    private fun initSunRays() {
        sunRays.clear()
        repeat(rayCount) { i ->
            sunRays.add(SunRay(
                angle = (360f / rayCount) * i,
                length = Random.nextFloat() * 50f + 80f,
                alpha = Random.nextInt(30, 80)
            ))
        }
    }

    private fun createBubble(randomY: Boolean = false): Bubble {
        return Bubble(
            x = Random.nextFloat() * width,
            y = if (randomY) Random.nextFloat() * height else height + Random.nextFloat() * 100f,
            radius = Random.nextFloat() * 15f + 8f,
            speedY = -(Random.nextFloat() * 1.5f + 0.5f),
            speedX = Random.nextFloat() * 0.6f - 0.3f,
            wobblePhase = Random.nextFloat() * Math.PI.toFloat() * 2,
            color = bubbleColors.random(),
            alpha = Random.nextInt(80, 180)
        )
    }


    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!isAnimating) return

        // Vẽ ánh nắng ở góc trên bên phải
        drawSunshine(canvas)
        
        // Vẽ bong bóng
        bubbles.forEach { bubble ->
            drawBubble(canvas, bubble)
            updateBubble(bubble)
        }

        // Update sun rays rotation
        rayRotation += 0.2f
        if (rayRotation >= 360f) rayRotation = 0f

        postInvalidateOnAnimation()
    }

    private fun drawSunshine(canvas: Canvas) {
        val sunX = width - 60f
        val sunY = 80f
        
        // Vẽ các tia nắng
        sunRays.forEach { ray ->
            paint.color = Color.parseColor("#FFD700")
            paint.alpha = ray.alpha
            paint.strokeWidth = 8f
            paint.style = Paint.Style.STROKE
            paint.strokeCap = Paint.Cap.ROUND
            
            val angle = Math.toRadians((ray.angle + rayRotation).toDouble())
            val startX = sunX + cos(angle).toFloat() * 35f
            val startY = sunY + sin(angle).toFloat() * 35f
            val endX = sunX + cos(angle).toFloat() * (35f + ray.length)
            val endY = sunY + sin(angle).toFloat() * (35f + ray.length)
            
            canvas.drawLine(startX, startY, endX, endY, paint)
        }
        
        // Vẽ mặt trời
        paint.style = Paint.Style.FILL
        
        // Gradient cho mặt trời
        val sunGradient = RadialGradient(
            sunX, sunY, 35f,
            intArrayOf(
                Color.parseColor("#FFEB3B"),
                Color.parseColor("#FFC107"),
                Color.parseColor("#FF9800")
            ),
            floatArrayOf(0f, 0.6f, 1f),
            Shader.TileMode.CLAMP
        )
        paint.shader = sunGradient
        paint.alpha = 200
        canvas.drawCircle(sunX, sunY, 35f, paint)
        paint.shader = null
    }

    private fun drawBubble(canvas: Canvas, bubble: Bubble) {
        // Vẽ bong bóng với gradient
        val gradient = RadialGradient(
            bubble.x - bubble.radius * 0.3f,
            bubble.y - bubble.radius * 0.3f,
            bubble.radius,
            intArrayOf(
                Color.argb((bubble.alpha * 0.8f).toInt(), 255, 255, 255),
                bubble.color,
                Color.argb((bubble.alpha * 0.3f).toInt(), 
                    Color.red(bubble.color), 
                    Color.green(bubble.color), 
                    Color.blue(bubble.color))
            ),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        
        paint.shader = gradient
        paint.style = Paint.Style.FILL
        paint.alpha = bubble.alpha
        canvas.drawCircle(bubble.x, bubble.y, bubble.radius, paint)
        paint.shader = null
        
        // Highlight
        paint.color = Color.WHITE
        paint.alpha = (bubble.alpha * 0.6f).toInt()
        canvas.drawCircle(
            bubble.x - bubble.radius * 0.3f,
            bubble.y - bubble.radius * 0.3f,
            bubble.radius * 0.25f,
            paint
        )
    }

    private fun updateBubble(bubble: Bubble) {
        bubble.wobblePhase += 0.05f
        bubble.x += bubble.speedX + sin(bubble.wobblePhase) * 0.5f
        bubble.y += bubble.speedY
        
        // Fade out khi bay lên cao
        if (bubble.y < height * 0.2f) {
            bubble.alpha = (bubble.alpha * 0.97f).toInt().coerceAtLeast(0)
        }

        if (bubble.y < -50 || bubble.alpha <= 0) {
            resetBubble(bubble)
        }
        if (bubble.x < -50) bubble.x = width + 50f
        if (bubble.x > width + 50) bubble.x = -50f
    }

    private fun resetBubble(bubble: Bubble) {
        bubble.x = Random.nextFloat() * width
        bubble.y = height + Random.nextFloat() * 100f
        bubble.radius = Random.nextFloat() * 15f + 8f
        bubble.speedY = -(Random.nextFloat() * 1.5f + 0.5f)
        bubble.alpha = Random.nextInt(80, 180)
        bubble.color = bubbleColors.random()
    }

    fun startShining() {
        isAnimating = true
        if (bubbles.isEmpty() && width > 0) {
            initBubbles()
            initSunRays()
        }
        invalidate()
    }

    fun stopShining() {
        isAnimating = false
    }

    fun isShining(): Boolean = isAnimating
}

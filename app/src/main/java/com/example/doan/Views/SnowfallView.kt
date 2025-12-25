package com.example.doan.Views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.random.Random

/**
 * Custom View hiệu ứng tuyết rơi
 * Sử dụng: Thêm vào layout XML hoặc programmatically
 */
class SnowfallView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val snowflakes = mutableListOf<Snowflake>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var isSnowing = true
    
    // Cấu hình
    var snowflakeCount = 100
    var minRadius = 2f
    var maxRadius = 8f
    var minSpeed = 1f
    var maxSpeed = 5f
    var snowColor = Color.WHITE
    
    init {
        paint.color = snowColor
        paint.style = Paint.Style.FILL
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        initSnowflakes()
    }

    private fun initSnowflakes() {
        snowflakes.clear()
        repeat(snowflakeCount) {
            snowflakes.add(createSnowflake(randomY = true))
        }
    }

    private fun createSnowflake(randomY: Boolean = false): Snowflake {
        return Snowflake(
            x = Random.nextFloat() * width,
            y = if (randomY) Random.nextFloat() * height else -maxRadius,
            radius = Random.nextFloat() * (maxRadius - minRadius) + minRadius,
            speed = Random.nextFloat() * (maxSpeed - minSpeed) + minSpeed,
            alpha = Random.nextInt(150, 255),
            drift = Random.nextFloat() * 2f - 1f // -1 to 1 for horizontal movement
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        if (!isSnowing) return

        snowflakes.forEach { snowflake ->
            paint.alpha = snowflake.alpha
            canvas.drawCircle(snowflake.x, snowflake.y, snowflake.radius, paint)
        }

        updateSnowflakes()
        invalidate()
    }

    private fun updateSnowflakes() {
        snowflakes.forEachIndexed { index, snowflake ->
            // Di chuyển xuống
            snowflake.y += snowflake.speed
            // Di chuyển ngang nhẹ (gió)
            snowflake.x += snowflake.drift
            
            // Reset khi ra khỏi màn hình
            if (snowflake.y > height + snowflake.radius) {
                snowflakes[index] = createSnowflake(randomY = false)
            }
            
            // Wrap around horizontally
            if (snowflake.x < -snowflake.radius) {
                snowflake.x = width + snowflake.radius
            } else if (snowflake.x > width + snowflake.radius) {
                snowflake.x = -snowflake.radius
            }
        }
    }

    fun startSnowing() {
        isSnowing = true
        invalidate()
    }

    fun stopSnowing() {
        isSnowing = false
    }

    fun setSnowflakeColor(color: Int) {
        snowColor = color
        paint.color = color
    }

    private data class Snowflake(
        var x: Float,
        var y: Float,
        val radius: Float,
        val speed: Float,
        val alpha: Int,
        val drift: Float
    )
}

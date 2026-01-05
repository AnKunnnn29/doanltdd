package com.example.doan.Utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * ❄️ SnowfallView - Hiệu ứng tuyết rơi đẹp mắt
 * Sử dụng trong layout XML hoặc add programmatically
 * 
 * Ví dụ XML:
 * <com.example.doan.Utils.SnowfallView
 *     android:id="@+id/snowfall_view"
 *     android:layout_width="match_parent"
 *     android:layout_height="match_parent" />
 */
class SnowfallView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val snowflakes = mutableListOf<Snowflake>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var isSnowing = true
    private var snowflakeCount = 80
    private var maxSnowflakeSize = 12f
    private var minSnowflakeSize = 3f
    private var fallSpeedMultiplier = 1f

    data class Snowflake(
        var x: Float,
        var y: Float,
        var radius: Float,
        var speed: Float,
        var alpha: Int,
        var angle: Float,
        var oscillationSpeed: Float,
        var oscillationAmplitude: Float
    )

    init {
        paint.color = Color.WHITE
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
        val radius = Random.nextFloat() * (maxSnowflakeSize - minSnowflakeSize) + minSnowflakeSize
        return Snowflake(
            x = Random.nextFloat() * width,
            y = if (randomY) Random.nextFloat() * height else -radius * 2,
            radius = radius,
            speed = (Random.nextFloat() * 3 + 1) * fallSpeedMultiplier,
            alpha = Random.nextInt(100, 255),
            angle = Random.nextFloat() * 360,
            oscillationSpeed = Random.nextFloat() * 0.02f + 0.01f,
            oscillationAmplitude = Random.nextFloat() * 30 + 10
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        if (!isSnowing) return

        snowflakes.forEach { snowflake ->
            paint.alpha = snowflake.alpha
            
            // Vẽ snowflake với hiệu ứng blur nhẹ
            canvas.drawCircle(snowflake.x, snowflake.y, snowflake.radius, paint)
            
            // Vẽ thêm một vòng nhỏ hơn để tạo hiệu ứng sáng
            paint.alpha = snowflake.alpha / 2
            canvas.drawCircle(
                snowflake.x - snowflake.radius * 0.3f,
                snowflake.y - snowflake.radius * 0.3f,
                snowflake.radius * 0.4f,
                paint
            )
        }

        updateSnowflakes()
        
        if (isSnowing) {
            postInvalidateDelayed(16) // ~60 FPS
        }
    }

    private fun updateSnowflakes() {
        snowflakes.forEachIndexed { index, snowflake ->
            // Di chuyển xuống
            snowflake.y += snowflake.speed
            
            // Dao động ngang (wind effect)
            snowflake.angle += snowflake.oscillationSpeed
            snowflake.x += sin(snowflake.angle.toDouble()).toFloat() * 0.5f
            
            // Reset khi ra khỏi màn hình
            if (snowflake.y > height + snowflake.radius * 2) {
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

    /**
     * Bắt đầu hiệu ứng tuyết rơi
     */
    fun startSnowing() {
        isSnowing = true
        // Reinitialize snowflakes nếu cần
        if (snowflakes.isEmpty() && width > 0 && height > 0) {
            initSnowflakes()
        }
        // Visibility được quản lý bởi SeasonalEffectManager
        invalidate()
    }

    /**
     * Dừng hiệu ứng tuyết rơi
     */
    fun stopSnowing() {
        isSnowing = false
        // Visibility được quản lý bởi SeasonalEffectManager
    }

    /**
     * Cấu hình số lượng bông tuyết
     */
    fun setSnowflakeCount(count: Int) {
        snowflakeCount = count
        initSnowflakes()
    }

    /**
     * Cấu hình tốc độ rơi (1.0 = bình thường)
     */
    fun setFallSpeed(multiplier: Float) {
        fallSpeedMultiplier = multiplier
    }

    /**
     * Kiểm tra xem có đang tuyết rơi không
     */
    fun isSnowing(): Boolean = isSnowing
}

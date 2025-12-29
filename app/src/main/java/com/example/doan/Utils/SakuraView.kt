package com.example.doan.Utils

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * 🌸 SakuraView - Hiệu ứng hoa đào rơi (Mùa xuân/Tết)
 */
class SakuraView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val petals = mutableListOf<Petal>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var isAnimating = false
    private val petalCount = 40

    private val petalColors = listOf(
        Color.parseColor("#FFB7C5"),  // Hồng nhạt
        Color.parseColor("#FFC0CB"),  // Pink
        Color.parseColor("#FFD1DC"),  // Hồng pastel
        Color.parseColor("#FFAEC9"),  // Hồng đậm hơn
        Color.parseColor("#FFFFFF")   // Trắng
    )

    data class Petal(
        var x: Float,
        var y: Float,
        var size: Float,
        var rotation: Float,
        var rotationSpeed: Float,
        var speedY: Float,
        var speedX: Float,
        var swayAmplitude: Float,
        var swayPhase: Float,
        var color: Int,
        var alpha: Int
    )

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (petals.isEmpty() && w > 0 && h > 0) {
            initPetals()
        }
    }

    private fun initPetals() {
        petals.clear()
        repeat(petalCount) {
            petals.add(createPetal(randomY = true))
        }
    }

    private fun createPetal(randomY: Boolean = false): Petal {
        return Petal(
            x = Random.nextFloat() * width,
            y = if (randomY) Random.nextFloat() * height else -Random.nextFloat() * 100f,
            size = Random.nextFloat() * 15f + 10f,
            rotation = Random.nextFloat() * 360f,
            rotationSpeed = Random.nextFloat() * 3f - 1.5f,
            speedY = Random.nextFloat() * 2f + 1f,
            speedX = Random.nextFloat() * 1f - 0.5f,
            swayAmplitude = Random.nextFloat() * 30f + 20f,
            swayPhase = Random.nextFloat() * Math.PI.toFloat() * 2,
            color = petalColors.random(),
            alpha = Random.nextInt(150, 255)
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!isAnimating) return

        petals.forEach { petal ->
            drawPetal(canvas, petal)
            updatePetal(petal)
        }

        postInvalidateOnAnimation()
    }

    private fun drawPetal(canvas: Canvas, petal: Petal) {
        canvas.save()
        canvas.translate(petal.x, petal.y)
        canvas.rotate(petal.rotation)

        paint.color = petal.color
        paint.alpha = petal.alpha
        paint.style = Paint.Style.FILL

        // Vẽ cánh hoa đào (hình oval với đầu nhọn)
        val path = Path()
        val size = petal.size
        
        path.moveTo(0f, -size)
        path.quadTo(size * 0.8f, -size * 0.3f, size * 0.5f, size * 0.3f)
        path.quadTo(0f, size * 0.8f, -size * 0.5f, size * 0.3f)
        path.quadTo(-size * 0.8f, -size * 0.3f, 0f, -size)
        path.close()

        canvas.drawPath(path, paint)
        canvas.restore()
    }

    private fun updatePetal(petal: Petal) {
        petal.swayPhase += 0.05f
        petal.x += petal.speedX + sin(petal.swayPhase) * 0.5f
        petal.y += petal.speedY
        petal.rotation += petal.rotationSpeed

        if (petal.y > height + 50) {
            resetPetal(petal)
        }
        if (petal.x < -50) petal.x = width + 50f
        if (petal.x > width + 50) petal.x = -50f
    }

    private fun resetPetal(petal: Petal) {
        petal.x = Random.nextFloat() * width
        petal.y = -Random.nextFloat() * 100f
        petal.size = Random.nextFloat() * 15f + 10f
        petal.speedY = Random.nextFloat() * 2f + 1f
        petal.alpha = Random.nextInt(150, 255)
    }

    fun startFalling() {
        isAnimating = true
        if (petals.isEmpty() && width > 0) initPetals()
        invalidate()
    }

    fun stopFalling() {
        isAnimating = false
    }

    fun isFalling(): Boolean = isAnimating
}

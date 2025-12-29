package com.example.doan.Utils

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * 🎆 FireworksView - Hiệu ứng pháo hoa cho Năm mới
 */
class FireworksView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val fireworks = mutableListOf<Firework>()
    private val particles = mutableListOf<Particle>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var isAnimating = false
    private var lastFireworkTime = 0L
    private val fireworkInterval = 800L // Mỗi 800ms bắn 1 pháo

    private val fireworkColors = listOf(
        Color.parseColor("#FF6B6B"),  // Đỏ
        Color.parseColor("#4ECDC4"),  // Cyan
        Color.parseColor("#FFE66D"),  // Vàng
        Color.parseColor("#95E1D3"),  // Mint
        Color.parseColor("#F38181"),  // Coral
        Color.parseColor("#AA96DA"),  // Tím nhạt
        Color.parseColor("#FCBAD3"),  // Hồng
        Color.parseColor("#A8D8EA"),  // Xanh nhạt
        Color.parseColor("#FF9F43"),  // Cam
        Color.parseColor("#EE5A24")   // Đỏ cam
    )

    data class Firework(
        var x: Float,
        var y: Float,
        var targetY: Float,
        var speed: Float,
        var color: Int,
        var exploded: Boolean = false
    )

    data class Particle(
        var x: Float,
        var y: Float,
        var vx: Float,
        var vy: Float,
        var color: Int,
        var alpha: Int,
        var size: Float,
        var life: Float,
        var decay: Float,
        var gravity: Float
    )

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!isAnimating) return

        val currentTime = System.currentTimeMillis()
        
        // Tạo pháo hoa mới định kỳ
        if (currentTime - lastFireworkTime > fireworkInterval && width > 0) {
            createFirework()
            lastFireworkTime = currentTime
        }

        // Vẽ và cập nhật pháo hoa đang bay lên
        val fireworksToRemove = mutableListOf<Firework>()
        fireworks.forEach { firework ->
            if (!firework.exploded) {
                drawRocket(canvas, firework)
                updateRocket(firework)
                
                if (firework.y <= firework.targetY) {
                    explodeFirework(firework)
                    fireworksToRemove.add(firework)
                }
            }
        }
        fireworks.removeAll(fireworksToRemove)

        // Vẽ và cập nhật particles
        val particlesToRemove = mutableListOf<Particle>()
        particles.forEach { particle ->
            drawParticle(canvas, particle)
            updateParticle(particle)
            
            if (particle.alpha <= 0 || particle.life <= 0) {
                particlesToRemove.add(particle)
            }
        }
        particles.removeAll(particlesToRemove)

        postInvalidateOnAnimation()
    }

    private fun createFirework() {
        val firework = Firework(
            x = Random.nextFloat() * width,
            y = height.toFloat(),
            targetY = Random.nextFloat() * height * 0.4f + height * 0.1f,
            speed = Random.nextFloat() * 8f + 12f,
            color = fireworkColors.random()
        )
        fireworks.add(firework)
    }

    private fun drawRocket(canvas: Canvas, firework: Firework) {
        // Vẽ đuôi pháo
        paint.color = firework.color
        paint.alpha = 255
        
        val gradient = LinearGradient(
            firework.x, firework.y,
            firework.x, firework.y + 30f,
            firework.color,
            Color.TRANSPARENT,
            Shader.TileMode.CLAMP
        )
        paint.shader = gradient
        canvas.drawRect(firework.x - 2f, firework.y, firework.x + 2f, firework.y + 30f, paint)
        paint.shader = null
        
        // Vẽ đầu pháo
        paint.alpha = 255
        canvas.drawCircle(firework.x, firework.y, 4f, paint)
    }

    private fun updateRocket(firework: Firework) {
        firework.y -= firework.speed
    }

    private fun explodeFirework(firework: Firework) {
        val particleCount = Random.nextInt(60, 100)
        val explosionType = Random.nextInt(3) // 0: tròn, 1: tim, 2: sao
        
        repeat(particleCount) { i ->
            val angle: Double
            val speed: Float
            
            when (explosionType) {
                0 -> { // Tròn
                    angle = Random.nextDouble() * Math.PI * 2
                    speed = Random.nextFloat() * 6f + 2f
                }
                1 -> { // Tim
                    val t = (i.toFloat() / particleCount) * Math.PI * 2
                    angle = t
                    speed = (4f + 2f * sin(t).toFloat()) * (Random.nextFloat() * 0.5f + 0.75f)
                }
                else -> { // Sao
                    angle = Random.nextDouble() * Math.PI * 2
                    val starFactor = if (i % 5 == 0) 1.5f else 0.8f
                    speed = (Random.nextFloat() * 4f + 2f) * starFactor
                }
            }
            
            particles.add(Particle(
                x = firework.x,
                y = firework.y,
                vx = (cos(angle) * speed).toFloat(),
                vy = (sin(angle) * speed).toFloat(),
                color = if (Random.nextFloat() > 0.3f) firework.color else fireworkColors.random(),
                alpha = 255,
                size = Random.nextFloat() * 4f + 2f,
                life = 1f,
                decay = Random.nextFloat() * 0.02f + 0.01f,
                gravity = 0.08f
            ))
        }
        
        // Thêm tia sáng trung tâm
        repeat(20) {
            val angle = Random.nextDouble() * Math.PI * 2
            particles.add(Particle(
                x = firework.x,
                y = firework.y,
                vx = (cos(angle) * 8f).toFloat(),
                vy = (sin(angle) * 8f).toFloat(),
                color = Color.WHITE,
                alpha = 255,
                size = 3f,
                life = 0.5f,
                decay = 0.05f,
                gravity = 0.02f
            ))
        }
    }


    private fun drawParticle(canvas: Canvas, particle: Particle) {
        paint.color = particle.color
        paint.alpha = particle.alpha
        paint.style = Paint.Style.FILL
        
        // Vẽ particle với glow effect
        canvas.drawCircle(particle.x, particle.y, particle.size, paint)
        
        // Vẽ trail
        paint.alpha = (particle.alpha * 0.3f).toInt()
        canvas.drawCircle(
            particle.x - particle.vx * 2,
            particle.y - particle.vy * 2,
            particle.size * 0.7f,
            paint
        )
    }

    private fun updateParticle(particle: Particle) {
        particle.x += particle.vx
        particle.y += particle.vy
        particle.vy += particle.gravity
        particle.vx *= 0.98f
        particle.vy *= 0.98f
        particle.life -= particle.decay
        particle.alpha = (particle.life * 255).toInt().coerceIn(0, 255)
        particle.size *= 0.99f
    }

    fun startFireworks() {
        isAnimating = true
        lastFireworkTime = System.currentTimeMillis()
        invalidate()
    }

    fun stopFireworks() {
        isAnimating = false
        fireworks.clear()
        particles.clear()
    }

    fun isPlaying(): Boolean = isAnimating
}

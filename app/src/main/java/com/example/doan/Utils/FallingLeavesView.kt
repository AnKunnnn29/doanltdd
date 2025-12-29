package com.example.doan.Utils

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * 🍂 FallingLeavesView - Hiệu ứng lá rơi (Mùa thu)
 */
class FallingLeavesView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val leaves = mutableListOf<Leaf>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var isAnimating = false
    private val leafCount = 35

    private val leafColors = listOf(
        Color.parseColor("#D2691E"),  // Nâu cam
        Color.parseColor("#CD853F"),  // Peru
        Color.parseColor("#DAA520"),  // Goldenrod
        Color.parseColor("#B8860B"),  // Dark goldenrod
        Color.parseColor("#FF8C00"),  // Dark orange
        Color.parseColor("#8B4513"),  // Saddle brown
        Color.parseColor("#A0522D"),  // Sienna
        Color.parseColor("#CD5C5C")   // Indian red
    )

    data class Leaf(
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
        var alpha: Int,
        var leafType: Int  // 0: maple, 1: oak, 2: simple
    )

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (leaves.isEmpty() && w > 0 && h > 0) {
            initLeaves()
        }
    }

    private fun initLeaves() {
        leaves.clear()
        repeat(leafCount) {
            leaves.add(createLeaf(randomY = true))
        }
    }

    private fun createLeaf(randomY: Boolean = false): Leaf {
        return Leaf(
            x = Random.nextFloat() * width,
            y = if (randomY) Random.nextFloat() * height else -Random.nextFloat() * 150f,
            size = Random.nextFloat() * 20f + 15f,
            rotation = Random.nextFloat() * 360f,
            rotationSpeed = Random.nextFloat() * 4f - 2f,
            speedY = Random.nextFloat() * 1.5f + 0.8f,
            speedX = Random.nextFloat() * 2f - 1f,
            swayAmplitude = Random.nextFloat() * 40f + 30f,
            swayPhase = Random.nextFloat() * Math.PI.toFloat() * 2,
            color = leafColors.random(),
            alpha = Random.nextInt(180, 255),
            leafType = Random.nextInt(3)
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!isAnimating) return

        leaves.forEach { leaf ->
            drawLeaf(canvas, leaf)
            updateLeaf(leaf)
        }

        postInvalidateOnAnimation()
    }

    private fun drawLeaf(canvas: Canvas, leaf: Leaf) {
        canvas.save()
        canvas.translate(leaf.x, leaf.y)
        canvas.rotate(leaf.rotation)

        paint.color = leaf.color
        paint.alpha = leaf.alpha
        paint.style = Paint.Style.FILL

        when (leaf.leafType) {
            0 -> drawMapleLeaf(canvas, leaf.size)
            1 -> drawOakLeaf(canvas, leaf.size)
            else -> drawSimpleLeaf(canvas, leaf.size)
        }

        canvas.restore()
    }

    private fun drawMapleLeaf(canvas: Canvas, size: Float) {
        val path = Path()
        // Lá phong đơn giản hóa
        path.moveTo(0f, -size)
        path.lineTo(size * 0.3f, -size * 0.5f)
        path.lineTo(size * 0.8f, -size * 0.3f)
        path.lineTo(size * 0.4f, 0f)
        path.lineTo(size * 0.6f, size * 0.5f)
        path.lineTo(0f, size * 0.3f)
        path.lineTo(-size * 0.6f, size * 0.5f)
        path.lineTo(-size * 0.4f, 0f)
        path.lineTo(-size * 0.8f, -size * 0.3f)
        path.lineTo(-size * 0.3f, -size * 0.5f)
        path.close()
        canvas.drawPath(path, paint)
    }

    private fun drawOakLeaf(canvas: Canvas, size: Float) {
        val path = Path()
        // Lá sồi đơn giản hóa
        path.moveTo(0f, -size)
        for (i in 0..5) {
            val angle = Math.PI * i / 5 - Math.PI / 2
            val r = if (i % 2 == 0) size * 0.8f else size * 0.5f
            path.lineTo((cos(angle) * r).toFloat(), (sin(angle) * r).toFloat() + size * 0.2f)
        }
        path.lineTo(0f, size)
        for (i in 5 downTo 0) {
            val angle = -Math.PI * i / 5 + Math.PI / 2
            val r = if (i % 2 == 0) size * 0.8f else size * 0.5f
            path.lineTo((cos(angle) * r).toFloat(), (sin(angle) * r).toFloat() + size * 0.2f)
        }
        path.close()
        canvas.drawPath(path, paint)
    }

    private fun drawSimpleLeaf(canvas: Canvas, size: Float) {
        val path = Path()
        // Lá đơn giản hình oval
        path.moveTo(0f, -size)
        path.quadTo(size * 0.6f, -size * 0.3f, size * 0.4f, size * 0.3f)
        path.quadTo(0f, size, -size * 0.4f, size * 0.3f)
        path.quadTo(-size * 0.6f, -size * 0.3f, 0f, -size)
        path.close()
        canvas.drawPath(path, paint)
        
        // Vẽ gân lá
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.5f
        paint.alpha = (paint.alpha * 0.7f).toInt()
        canvas.drawLine(0f, -size * 0.8f, 0f, size * 0.6f, paint)
        paint.style = Paint.Style.FILL
    }

    private fun updateLeaf(leaf: Leaf) {
        leaf.swayPhase += 0.03f
        leaf.x += leaf.speedX + sin(leaf.swayPhase) * 1.5f
        leaf.y += leaf.speedY
        leaf.rotation += leaf.rotationSpeed

        if (leaf.y > height + 50) {
            resetLeaf(leaf)
        }
        if (leaf.x < -50) leaf.x = width + 50f
        if (leaf.x > width + 50) leaf.x = -50f
    }

    private fun resetLeaf(leaf: Leaf) {
        leaf.x = Random.nextFloat() * width
        leaf.y = -Random.nextFloat() * 150f
        leaf.size = Random.nextFloat() * 20f + 15f
        leaf.speedY = Random.nextFloat() * 1.5f + 0.8f
        leaf.alpha = Random.nextInt(180, 255)
        leaf.leafType = Random.nextInt(3)
        leaf.color = leafColors.random()
    }

    fun startFalling() {
        isAnimating = true
        if (leaves.isEmpty() && width > 0) initLeaves()
        invalidate()
    }

    fun stopFalling() {
        isAnimating = false
    }

    fun isFalling(): Boolean = isAnimating
}

package com.example.doan.Utils

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * 🧧 TetView - Hiệu ứng Tết Nguyên Đán
 * Bao gồm: Bao lì xì, hoa mai vàng, hoa đào hồng
 */
class TetView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val items = mutableListOf<TetItem>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var isAnimating = false
    private val itemCount = 35

    // Màu sắc
    private val redEnvelopeColor = Color.parseColor("#E53935")  // Đỏ bao lì xì
    private val goldColor = Color.parseColor("#FFD700")         // Vàng
    private val maiYellowColors = listOf(
        Color.parseColor("#FFD700"),  // Vàng đậm
        Color.parseColor("#FFEB3B"),  // Vàng nhạt
        Color.parseColor("#FFF176")   // Vàng pastel
    )
    private val daoPinkColors = listOf(
        Color.parseColor("#FFB7C5"),  // Hồng nhạt
        Color.parseColor("#FF8FAB"),  // Hồng đậm
        Color.parseColor("#FFC0CB"),  // Pink
        Color.parseColor("#FFD1DC")   // Hồng pastel
    )

    enum class ItemType {
        RED_ENVELOPE,  // Bao lì xì 🧧
        MAI_FLOWER,    // Hoa mai vàng 🌼
        DAO_FLOWER     // Hoa đào hồng 🌸
    }

    data class TetItem(
        var x: Float,
        var y: Float,
        var size: Float,
        var rotation: Float,
        var rotationSpeed: Float,
        var speedY: Float,
        var speedX: Float,
        var swayPhase: Float,
        var type: ItemType,
        var color: Int,
        var alpha: Int
    )

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (items.isEmpty() && w > 0 && h > 0) {
            initItems()
        }
    }

    private fun initItems() {
        items.clear()
        repeat(itemCount) {
            items.add(createItem(randomY = true))
        }
    }

    private fun createItem(randomY: Boolean = false): TetItem {
        val type = when (Random.nextInt(10)) {
            in 0..1 -> ItemType.RED_ENVELOPE  // 20% bao lì xì
            in 2..5 -> ItemType.MAI_FLOWER    // 40% hoa mai
            else -> ItemType.DAO_FLOWER       // 40% hoa đào
        }
        
        val color = when (type) {
            ItemType.RED_ENVELOPE -> redEnvelopeColor
            ItemType.MAI_FLOWER -> maiYellowColors.random()
            ItemType.DAO_FLOWER -> daoPinkColors.random()
        }
        
        val size = when (type) {
            ItemType.RED_ENVELOPE -> Random.nextFloat() * 20f + 25f
            else -> Random.nextFloat() * 12f + 10f
        }

        return TetItem(
            x = Random.nextFloat() * width,
            y = if (randomY) Random.nextFloat() * height else -Random.nextFloat() * 150f,
            size = size,
            rotation = Random.nextFloat() * 360f,
            rotationSpeed = Random.nextFloat() * 3f - 1.5f,
            speedY = Random.nextFloat() * 1.5f + 0.8f,
            speedX = Random.nextFloat() * 1f - 0.5f,
            swayPhase = Random.nextFloat() * Math.PI.toFloat() * 2,
            type = type,
            color = color,
            alpha = Random.nextInt(180, 255)
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!isAnimating) return

        items.forEach { item ->
            when (item.type) {
                ItemType.RED_ENVELOPE -> drawRedEnvelope(canvas, item)
                ItemType.MAI_FLOWER -> drawMaiFlower(canvas, item)
                ItemType.DAO_FLOWER -> drawDaoFlower(canvas, item)
            }
            updateItem(item)
        }

        postInvalidateOnAnimation()
    }

    /**
     * Vẽ bao lì xì 🧧
     */
    private fun drawRedEnvelope(canvas: Canvas, item: TetItem) {
        canvas.save()
        canvas.translate(item.x, item.y)
        canvas.rotate(item.rotation * 0.3f) // Xoay nhẹ
        
        val size = item.size
        val halfW = size * 0.6f
        val halfH = size
        
        // Thân bao lì xì (đỏ)
        paint.color = item.color
        paint.alpha = item.alpha
        paint.style = Paint.Style.FILL
        
        val rect = RectF(-halfW, -halfH, halfW, halfH)
        canvas.drawRoundRect(rect, size * 0.15f, size * 0.15f, paint)
        
        // Viền vàng
        paint.color = goldColor
        paint.alpha = item.alpha
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        canvas.drawRoundRect(rect, size * 0.15f, size * 0.15f, paint)
        
        // Hình tròn vàng ở giữa (đồng xu)
        paint.style = Paint.Style.FILL
        canvas.drawCircle(0f, -size * 0.2f, size * 0.25f, paint)
        
        // Chữ "福" đơn giản hóa (hình vuông nhỏ)
        paint.color = item.color
        paint.alpha = item.alpha
        canvas.drawRect(-size * 0.12f, -size * 0.32f, size * 0.12f, -size * 0.08f, paint)
        
        // Nắp bao lì xì
        paint.color = Color.parseColor("#C62828")
        paint.alpha = item.alpha
        val flapPath = Path()
        flapPath.moveTo(-halfW, -halfH * 0.5f)
        flapPath.lineTo(0f, -halfH * 0.2f)
        flapPath.lineTo(halfW, -halfH * 0.5f)
        flapPath.lineTo(halfW, -halfH)
        flapPath.lineTo(-halfW, -halfH)
        flapPath.close()
        canvas.drawPath(flapPath, paint)
        
        canvas.restore()
    }

    /**
     * Vẽ hoa mai vàng 🌼
     */
    private fun drawMaiFlower(canvas: Canvas, item: TetItem) {
        canvas.save()
        canvas.translate(item.x, item.y)
        canvas.rotate(item.rotation)
        
        paint.color = item.color
        paint.alpha = item.alpha
        paint.style = Paint.Style.FILL
        
        val size = item.size
        
        // Vẽ 5 cánh hoa mai
        for (i in 0 until 5) {
            canvas.save()
            canvas.rotate(72f * i)
            
            val petalPath = Path()
            petalPath.moveTo(0f, 0f)
            petalPath.quadTo(size * 0.3f, -size * 0.5f, 0f, -size)
            petalPath.quadTo(-size * 0.3f, -size * 0.5f, 0f, 0f)
            canvas.drawPath(petalPath, paint)
            
            canvas.restore()
        }
        
        // Nhụy hoa (cam/nâu)
        paint.color = Color.parseColor("#FF8F00")
        paint.alpha = item.alpha
        canvas.drawCircle(0f, 0f, size * 0.2f, paint)
        
        // Các chấm nhụy nhỏ
        paint.color = Color.parseColor("#E65100")
        for (i in 0 until 5) {
            val angle = Math.toRadians((72.0 * i + 36))
            val px = (cos(angle) * size * 0.12f).toFloat()
            val py = (sin(angle) * size * 0.12f).toFloat()
            canvas.drawCircle(px, py, size * 0.05f, paint)
        }
        
        canvas.restore()
    }


    /**
     * Vẽ hoa đào hồng 🌸
     */
    private fun drawDaoFlower(canvas: Canvas, item: TetItem) {
        canvas.save()
        canvas.translate(item.x, item.y)
        canvas.rotate(item.rotation)
        
        paint.color = item.color
        paint.alpha = item.alpha
        paint.style = Paint.Style.FILL
        
        val size = item.size
        
        // Vẽ 5 cánh hoa đào (tròn hơn hoa mai)
        for (i in 0 until 5) {
            canvas.save()
            canvas.rotate(72f * i)
            
            // Cánh hoa đào có đầu tròn và khuyết ở giữa
            val petalPath = Path()
            petalPath.moveTo(0f, -size * 0.15f)
            petalPath.quadTo(size * 0.5f, -size * 0.4f, size * 0.3f, -size * 0.9f)
            petalPath.quadTo(0f, -size * 1.1f, -size * 0.3f, -size * 0.9f)
            petalPath.quadTo(-size * 0.5f, -size * 0.4f, 0f, -size * 0.15f)
            canvas.drawPath(petalPath, paint)
            
            canvas.restore()
        }
        
        // Nhụy hoa (hồng đậm/đỏ)
        paint.color = Color.parseColor("#E91E63")
        paint.alpha = item.alpha
        canvas.drawCircle(0f, 0f, size * 0.18f, paint)
        
        // Các nhụy nhỏ
        paint.color = Color.parseColor("#FFEB3B")
        paint.alpha = item.alpha
        for (i in 0 until 6) {
            val angle = Math.toRadians((60.0 * i))
            val px = (cos(angle) * size * 0.1f).toFloat()
            val py = (sin(angle) * size * 0.1f).toFloat()
            canvas.drawCircle(px, py, size * 0.04f, paint)
        }
        
        canvas.restore()
    }

    private fun updateItem(item: TetItem) {
        item.swayPhase += 0.04f
        item.x += item.speedX + sin(item.swayPhase) * 0.8f
        item.y += item.speedY
        item.rotation += item.rotationSpeed

        if (item.y > height + 50) {
            resetItem(item)
        }
        if (item.x < -50) item.x = width + 50f
        if (item.x > width + 50) item.x = -50f
    }

    private fun resetItem(item: TetItem) {
        val newItem = createItem(randomY = false)
        item.x = newItem.x
        item.y = newItem.y
        item.size = newItem.size
        item.type = newItem.type
        item.color = newItem.color
        item.speedY = newItem.speedY
        item.alpha = newItem.alpha
    }

    fun startFalling() {
        isAnimating = true
        if (items.isEmpty() && width > 0) initItems()
        invalidate()
    }

    fun stopFalling() {
        isAnimating = false
    }

    fun isFalling(): Boolean = isAnimating
}

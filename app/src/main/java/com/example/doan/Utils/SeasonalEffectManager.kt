package com.example.doan.Utils

import android.content.Context
import android.view.ViewGroup
import android.widget.FrameLayout
import java.util.Calendar

/**
 * 🎄 SeasonalEffectManager - Quản lý hiệu ứng theo mùa/lễ hội
 * 
 * Tự động hiển thị hiệu ứng phù hợp với thời điểm trong năm:
 * - Tháng 12 - 1: Tuyết rơi (Giáng sinh & Năm mới)
 * - Tết Nguyên Đán: Pháo hoa, hoa đào
 * - Valentine: Hearts
 */
object SeasonalEffectManager {

    enum class Season {
        WINTER,      // Tháng 12-1: Tuyết
        SPRING,      // Tháng 2-4: Hoa đào
        SUMMER,      // Tháng 5-8: Bình thường
        AUTUMN,      // Tháng 9-11: Lá rơi
        CHRISTMAS,   // 20-26/12: Giáng sinh
        NEW_YEAR,    // 28/12 - 5/1: Năm mới
        VALENTINE,   // 10-15/2: Valentine
        TET          // Tết Nguyên Đán (tùy năm)
    }

    private var snowfallView: SnowfallView? = null
    private var confettiView: ConfettiView? = null

    /**
     * Lấy mùa/lễ hội hiện tại
     */
    fun getCurrentSeason(): Season {
        val calendar = Calendar.getInstance()
        val month = calendar.get(Calendar.MONTH) + 1 // Calendar.MONTH bắt đầu từ 0
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        return when {
            // Christmas: 20-26/12
            month == 12 && day in 20..26 -> Season.CHRISTMAS
            
            // New Year: 28/12 - 5/1
            (month == 12 && day >= 28) || (month == 1 && day <= 5) -> Season.NEW_YEAR
            
            // Valentine: 10-15/2
            month == 2 && day in 10..15 -> Season.VALENTINE
            
            // Winter: Tháng 12-1
            month == 12 || month == 1 -> Season.WINTER
            
            // Spring: Tháng 2-4
            month in 2..4 -> Season.SPRING
            
            // Summer: Tháng 5-8
            month in 5..8 -> Season.SUMMER
            
            // Autumn: Tháng 9-11
            else -> Season.AUTUMN
        }
    }

    /**
     * Kiểm tra có nên hiển thị hiệu ứng tuyết không
     */
    fun shouldShowSnowfall(): Boolean {
        val season = getCurrentSeason()
        return season in listOf(Season.WINTER, Season.CHRISTMAS, Season.NEW_YEAR)
    }

    /**
     * Thêm hiệu ứng tuyết vào container
     * @param container ViewGroup để thêm SnowfallView
     * @param autoStart Tự động bắt đầu nếu đúng mùa
     */
    fun addSnowfallEffect(container: ViewGroup, autoStart: Boolean = true): SnowfallView {
        // Xóa view cũ nếu có
        snowfallView?.let { container.removeView(it) }

        val context = container.context
        snowfallView = SnowfallView(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            elevation = 100f // Đảm bảo hiển thị trên cùng
        }

        container.addView(snowfallView)

        if (autoStart && shouldShowSnowfall()) {
            snowfallView?.startSnowing()
        }

        return snowfallView!!
    }

    /**
     * Thêm hiệu ứng confetti vào container
     */
    fun addConfettiEffect(container: ViewGroup): ConfettiView {
        confettiView?.let { container.removeView(it) }

        val context = container.context
        confettiView = ConfettiView(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            elevation = 100f
            visibility = android.view.View.GONE
        }

        container.addView(confettiView)
        return confettiView!!
    }

    /**
     * Bắt đầu hiệu ứng tuyết
     */
    fun startSnowfall() {
        snowfallView?.startSnowing()
    }

    /**
     * Dừng hiệu ứng tuyết
     */
    fun stopSnowfall() {
        snowfallView?.stopSnowing()
    }

    /**
     * Toggle hiệu ứng tuyết
     */
    fun toggleSnowfall(): Boolean {
        return if (snowfallView?.isSnowing() == true) {
            stopSnowfall()
            false
        } else {
            startSnowfall()
            true
        }
    }

    /**
     * Hiển thị confetti (khi đặt hàng thành công)
     */
    fun showConfetti(duration: Long = 3000L, pieceCount: Int = 150) {
        confettiView?.startConfetti(duration, pieceCount)
    }

    /**
     * Lấy thông điệp chúc mừng theo mùa
     */
    fun getSeasonalGreeting(): String {
        return when (getCurrentSeason()) {
            Season.CHRISTMAS -> "🎄 Merry Christmas!"
            Season.NEW_YEAR -> "🎆 Happy New Year!"
            Season.VALENTINE -> "💕 Happy Valentine's Day!"
            Season.TET -> "🧧 Chúc Mừng Năm Mới!"
            Season.WINTER -> "❄️ Mùa đông ấm áp!"
            Season.SPRING -> "🌸 Xuân về rồi!"
            Season.SUMMER -> "☀️ Mùa hè sôi động!"
            Season.AUTUMN -> "🍂 Thu về lá bay!"
        }
    }

    /**
     * Lấy emoji theo mùa
     */
    fun getSeasonalEmoji(): String {
        return when (getCurrentSeason()) {
            Season.CHRISTMAS -> "🎄"
            Season.NEW_YEAR -> "🎆"
            Season.VALENTINE -> "💕"
            Season.TET -> "🧧"
            Season.WINTER -> "❄️"
            Season.SPRING -> "🌸"
            Season.SUMMER -> "☀️"
            Season.AUTUMN -> "🍂"
        }
    }

    /**
     * Cleanup - gọi khi Activity/Fragment bị destroy
     */
    fun cleanup() {
        snowfallView?.stopSnowing()
        confettiView?.stopConfetti()
        snowfallView = null
        confettiView = null
    }
}

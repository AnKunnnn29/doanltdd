package com.example.doan.Utils

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import java.util.Calendar

/**
 * 🎄 SeasonalEffectManager - Quản lý hiệu ứng theo mùa/lễ hội
 * 
 * Hỗ trợ 2 chế độ:
 * - 🕐 REALTIME: Tự động theo thời gian thực
 * - 🎨 CUSTOM: Tự chọn hiệu ứng yêu thích
 * 
 * Các hiệu ứng:
 * - ❄️ WINTER (Tháng 12-1): Tuyết rơi (SnowfallView)
 * - 🌸 SPRING (Tháng 2-4): Hoa đào rơi (SakuraView)
 * - ☀️ SUMMER (Tháng 5-8): Bong bóng & ánh nắng (SunshineView)
 * - 🍂 AUTUMN (Tháng 9-11): Lá rơi (FallingLeavesView)
 * - 🎄 CHRISTMAS (20-26/12): Tuyết rơi (SnowfallView)
 * - 🎆 NEW_YEAR (28/12 - 5/1): Pháo hoa (FireworksView)
 * - 💕 VALENTINE (10-15/2): Trái tim bay (HeartsView)
 * - 🧧 TET (Tết Nguyên Đán): Bao lì xì, hoa mai, hoa đào (TetView)
 */
object SeasonalEffectManager {

    enum class Season {
        WINTER,      // Tháng 12-1: Tuyết
        SPRING,      // Tháng 2-4: Hoa đào
        SUMMER,      // Tháng 5-8: Bong bóng
        AUTUMN,      // Tháng 9-11: Lá rơi
        CHRISTMAS,   // 20-26/12: Giáng sinh
        NEW_YEAR,    // 28/12 - 5/1: Năm mới
        VALENTINE,   // 10-15/2: Valentine
        TET          // Tết Nguyên Đán (tùy năm)
    }
    
    // Chế độ chọn mùa
    enum class SeasonMode {
        REALTIME,    // Theo thời gian thực
        CUSTOM       // Tự chọn
    }

    // Các view hiệu ứng
    private var snowfallView: SnowfallView? = null
    private var confettiView: ConfettiView? = null
    private var sakuraView: SakuraView? = null
    private var fallingLeavesView: FallingLeavesView? = null
    private var heartsView: HeartsView? = null
    private var sunshineView: SunshineView? = null
    private var fireworksView: FireworksView? = null
    private var tetView: TetView? = null
    
    // View hiệu ứng hiện tại đang active
    private var currentEffectView: View? = null
    
    // Preference keys
    private const val PREF_SEASON_MODE = "season_mode"
    private const val PREF_CUSTOM_SEASON = "custom_season"
    
    /**
     * Lấy chế độ hiệu ứng hiện tại (realtime hoặc custom)
     */
    fun getSeasonMode(context: Context): SeasonMode {
        val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val mode = prefs.getString(PREF_SEASON_MODE, "REALTIME")
        return try {
            SeasonMode.valueOf(mode ?: "REALTIME")
        } catch (e: Exception) {
            SeasonMode.REALTIME
        }
    }
    
    /**
     * Đặt chế độ hiệu ứng
     */
    fun setSeasonMode(context: Context, mode: SeasonMode) {
        val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        prefs.edit().putString(PREF_SEASON_MODE, mode.name).apply()
    }
    
    /**
     * Lấy mùa custom đã chọn
     */
    fun getCustomSeason(context: Context): Season {
        val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val season = prefs.getString(PREF_CUSTOM_SEASON, "WINTER")
        return try {
            Season.valueOf(season ?: "WINTER")
        } catch (e: Exception) {
            Season.WINTER
        }
    }
    
    /**
     * Đặt mùa custom
     */
    fun setCustomSeason(context: Context, season: Season) {
        val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        prefs.edit().putString(PREF_CUSTOM_SEASON, season.name).apply()
    }
    
    /**
     * Lấy mùa hiệu ứng hiện tại (dựa trên mode)
     * - REALTIME: Trả về mùa theo thời gian thực
     * - CUSTOM: Trả về mùa user đã chọn
     */
    fun getActiveSeason(context: Context): Season {
        return when (getSeasonMode(context)) {
            SeasonMode.REALTIME -> getRealTimeSeason()
            SeasonMode.CUSTOM -> getCustomSeason(context)
        }
    }

    /**
     * Lấy mùa/lễ hội theo thời gian thực
     */
    fun getRealTimeSeason(): Season {
        val calendar = Calendar.getInstance()
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val year = calendar.get(Calendar.YEAR)

        return when {
            month == 12 && day in 20..26 -> Season.CHRISTMAS
            (month == 12 && day >= 28) || (month == 1 && day <= 5) -> Season.NEW_YEAR
            month == 2 && day in 10..15 -> Season.VALENTINE
            isTetPeriod(year, month, day) -> Season.TET
            month == 12 || month == 1 -> Season.WINTER
            month in 2..4 -> Season.SPRING
            month in 5..8 -> Season.SUMMER
            else -> Season.AUTUMN
        }
    }
    
    /**
     * Lấy mùa/lễ hội hiện tại (legacy - để tương thích ngược)
     */
    fun getCurrentSeason(): Season = getRealTimeSeason()

    
    /**
     * Kiểm tra có phải dịp Tết không
     */
    private fun isTetPeriod(year: Int, month: Int, day: Int): Boolean {
        val tetDates = mapOf(
            2025 to Pair(1, 29),
            2026 to Pair(2, 17),
            2027 to Pair(2, 6),
            2028 to Pair(1, 26),
            2029 to Pair(2, 13),
            2030 to Pair(2, 3)
        )
        
        val tetDate = tetDates[year] ?: return false
        val tetMonth = tetDate.first
        val tetDay = tetDate.second
        
        val calendar = Calendar.getInstance()
        calendar.set(year, tetMonth - 1, tetDay)
        val tetTime = calendar.timeInMillis
        
        calendar.set(year, month - 1, day)
        val currentTime = calendar.timeInMillis
        
        val daysDiff = (currentTime - tetTime) / (1000 * 60 * 60 * 24)
        return daysDiff in -5..7
    }

    /**
     * Kiểm tra có nên hiển thị hiệu ứng tuyết không
     */
    fun shouldShowSnowfall(): Boolean {
        val season = getCurrentSeason()
        return season in listOf(Season.WINTER, Season.CHRISTMAS, Season.NEW_YEAR)
    }
    
    fun isSeasonalEffectsEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        return prefs.getBoolean("seasonal_effects_enabled", true)
    }
    
    fun isConfettiEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        return prefs.getBoolean("confetti_enabled", true)
    }
    
    fun isCartAnimationEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        return prefs.getBoolean("cart_animation_enabled", true)
    }

    /**
     * 🎯 Thêm hiệu ứng theo mùa vào container
     * Sử dụng getActiveSeason() để lấy mùa (realtime hoặc custom)
     */
    fun addSeasonalEffect(container: ViewGroup, autoStart: Boolean = true): View? {
        val context = container.context
        
        if (!isSeasonalEffectsEnabled(context)) {
            return null
        }
        
        removeCurrentEffect(container)
        
        // Sử dụng getActiveSeason thay vì getCurrentSeason
        val season = getActiveSeason(context)
        
        val effectView: View? = when (season) {
            Season.WINTER, Season.CHRISTMAS -> {
                createSnowfallView(context).also { snowfallView = it }
            }
            Season.NEW_YEAR -> {
                createFireworksView(context).also { fireworksView = it }
            }
            Season.SPRING -> {
                createSakuraView(context).also { sakuraView = it }
            }
            Season.TET -> {
                createTetView(context).also { tetView = it }
            }
            Season.SUMMER -> {
                createSunshineView(context).also { sunshineView = it }
            }
            Season.AUTUMN -> {
                createFallingLeavesView(context).also { fallingLeavesView = it }
            }
            Season.VALENTINE -> {
                createHeartsView(context).also { heartsView = it }
            }
        }
        
        effectView?.let { view ->
            view.layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            view.elevation = 100f
            container.addView(view)
            currentEffectView = view
            
            if (autoStart) {
                startEffectForSeason(season)
            }
        }
        
        return effectView
    }
    
    private fun createSnowfallView(context: Context) = SnowfallView(context)
    private fun createSakuraView(context: Context) = SakuraView(context)
    private fun createSunshineView(context: Context) = SunshineView(context)
    private fun createFallingLeavesView(context: Context) = FallingLeavesView(context)
    private fun createHeartsView(context: Context) = HeartsView(context)
    private fun createFireworksView(context: Context) = FireworksView(context)
    private fun createTetView(context: Context) = TetView(context)
    
    private fun removeCurrentEffect(container: ViewGroup) {
        currentEffectView?.let { container.removeView(it) }
        snowfallView?.let { container.removeView(it) }
        sakuraView?.let { container.removeView(it) }
        sunshineView?.let { container.removeView(it) }
        fallingLeavesView?.let { container.removeView(it) }
        heartsView?.let { container.removeView(it) }
        fireworksView?.let { container.removeView(it) }
        tetView?.let { container.removeView(it) }
    }
    
    /**
     * Bắt đầu hiệu ứng cho mùa cụ thể
     */
    private fun startEffectForSeason(season: Season) {
        when (season) {
            Season.WINTER, Season.CHRISTMAS -> snowfallView?.startSnowing()
            Season.NEW_YEAR -> fireworksView?.startFireworks()
            Season.SPRING -> sakuraView?.startFalling()
            Season.TET -> tetView?.startFalling()
            Season.SUMMER -> sunshineView?.startShining()
            Season.AUTUMN -> fallingLeavesView?.startFalling()
            Season.VALENTINE -> heartsView?.startFloating()
        }
    }
    
    fun startCurrentEffect() {
        startEffectForSeason(getCurrentSeason())
    }
    
    fun stopCurrentEffect() {
        snowfallView?.stopSnowing()
        sakuraView?.stopFalling()
        sunshineView?.stopShining()
        fallingLeavesView?.stopFalling()
        heartsView?.stopFloating()
        fireworksView?.stopFireworks()
        tetView?.stopFalling()
    }

    fun addSnowfallEffect(container: ViewGroup, autoStart: Boolean = true): SnowfallView {
        snowfallView?.let { container.removeView(it) }
        val context = container.context
        snowfallView = SnowfallView(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            elevation = 100f
        }
        container.addView(snowfallView)
        if (autoStart && shouldShowSnowfall() && isSeasonalEffectsEnabled(context)) {
            snowfallView?.startSnowing()
        }
        return snowfallView!!
    }

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

    fun startSnowfall() { snowfallView?.startSnowing() }
    fun stopSnowfall() { snowfallView?.stopSnowing() }
    
    fun toggleSnowfall(): Boolean {
        return if (snowfallView?.isSnowing() == true) {
            stopSnowfall()
            false
        } else {
            startSnowfall()
            true
        }
    }

    fun showConfetti(duration: Long = 3000L, pieceCount: Int = 150) {
        confettiView?.let { view ->
            if (isConfettiEnabled(view.context)) {
                view.startConfetti(duration, pieceCount)
            }
        }
    }


    /**
     * Lấy thông điệp chúc mừng theo mùa
     */
    fun getSeasonalGreeting(season: Season = getCurrentSeason()): String {
        return when (season) {
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
    fun getSeasonalEmoji(season: Season = getCurrentSeason()): String {
        return when (season) {
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
     * Lấy tên mùa bằng tiếng Việt
     */
    fun getSeasonNameVi(season: Season = getCurrentSeason()): String {
        return when (season) {
            Season.CHRISTMAS -> "Giáng sinh"
            Season.NEW_YEAR -> "Năm mới"
            Season.VALENTINE -> "Valentine"
            Season.TET -> "Tết Nguyên Đán"
            Season.WINTER -> "Mùa đông"
            Season.SPRING -> "Mùa xuân"
            Season.SUMMER -> "Mùa hè"
            Season.AUTUMN -> "Mùa thu"
        }
    }
    
    /**
     * Lấy danh sách tất cả các mùa với emoji và tên
     */
    fun getAllSeasonsWithNames(): List<Triple<Season, String, String>> {
        return Season.values().map { season ->
            Triple(season, getSeasonalEmoji(season), getSeasonNameVi(season))
        }
    }

    /**
     * Cleanup - gọi khi Activity/Fragment bị destroy
     */
    fun cleanup() {
        snowfallView?.stopSnowing()
        confettiView?.stopConfetti()
        sakuraView?.stopFalling()
        fallingLeavesView?.stopFalling()
        heartsView?.stopFloating()
        sunshineView?.stopShining()
        fireworksView?.stopFireworks()
        tetView?.stopFalling()
        
        snowfallView = null
        confettiView = null
        sakuraView = null
        fallingLeavesView = null
        heartsView = null
        sunshineView = null
        fireworksView = null
        tetView = null
        currentEffectView = null
    }
}

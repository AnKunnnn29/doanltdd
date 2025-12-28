package com.example.doan.Activities

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import com.example.doan.Models.*
import com.example.doan.Network.RetrofitClient
import com.example.doan.R
import com.google.android.material.progressindicator.LinearProgressIndicator
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MemberTierActivity : AppCompatActivity() {

    private lateinit var tvTierName: TextView
    private lateinit var tvTierDescription: TextView
    private lateinit var tvCurrentPoints: TextView
    private lateinit var tvPointsToNext: TextView
    private lateinit var progressTier: LinearProgressIndicator
    private lateinit var llBenefitsList: LinearLayout
    private lateinit var llTierProgress: LinearLayout
    private lateinit var cardCurrentTier: CardView
    private lateinit var ivTierBadge: ImageView
    private lateinit var loadingView: View
    private lateinit var contentView: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_member_tier)

        initViews()
        loadTierBenefits()
    }

    private fun initViews() {
        findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar).apply {
            setNavigationOnClickListener { finish() }
        }

        tvTierName = findViewById(R.id.tvTierName)
        tvTierDescription = findViewById(R.id.tvTierDescription)
        tvCurrentPoints = findViewById(R.id.tvCurrentPoints)
        tvPointsToNext = findViewById(R.id.tvPointsToNext)
        progressTier = findViewById(R.id.progressTier)
        llBenefitsList = findViewById(R.id.llBenefitsList)
        llTierProgress = findViewById(R.id.llTierProgress)
        cardCurrentTier = findViewById(R.id.cardCurrentTier)
        ivTierBadge = findViewById(R.id.ivTierBadge)
        loadingView = findViewById(R.id.loadingView)
        contentView = findViewById(R.id.contentView)
    }

    private fun loadTierBenefits() {
        showLoading(true)
        
        RetrofitClient.getInstance(this).apiService.getTierBenefits()
            .enqueue(object : Callback<ApiResponse<MemberTierBenefitsDto>> {
                override fun onResponse(
                    call: Call<ApiResponse<MemberTierBenefitsDto>>,
                    response: Response<ApiResponse<MemberTierBenefitsDto>>
                ) {
                    showLoading(false)
                    if (response.isSuccessful && response.body()?.success == true) {
                        response.body()?.data?.let { updateUI(it) }
                    } else {
                        Toast.makeText(this@MemberTierActivity, 
                            "Không thể tải thông tin hạng thành viên", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse<MemberTierBenefitsDto>>, t: Throwable) {
                    showLoading(false)
                    Toast.makeText(this@MemberTierActivity, 
                        "Lỗi kết nối: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun showLoading(show: Boolean) {
        loadingView.visibility = if (show) View.VISIBLE else View.GONE
        contentView.visibility = if (show) View.GONE else View.VISIBLE
    }

    private fun updateUI(data: MemberTierBenefitsDto) {
        // Tier name và màu
        tvTierName.text = data.tierName
        try {
            val tierColor = Color.parseColor(data.tierColor)
            tvTierName.setTextColor(tierColor)
            cardCurrentTier.setCardBackgroundColor(adjustAlpha(tierColor, 0.1f))
        } catch (e: Exception) {
            // Fallback color
        }

        // Tier badge icon
        ivTierBadge.setImageResource(getTierBadgeIcon(data.currentTier))

        // Description
        tvTierDescription.text = data.description ?: "Hạng thành viên của bạn"

        // Points
        tvCurrentPoints.text = "${data.currentPoints} điểm"
        
        if (data.nextTierName != null && data.pointsToNextTier > 0) {
            tvPointsToNext.visibility = View.VISIBLE
            tvPointsToNext.text = "Còn ${data.pointsToNextTier} điểm để lên hạng ${data.nextTierName}"
            progressTier.visibility = View.VISIBLE
            progressTier.progress = data.progressPercent.toInt()
        } else {
            tvPointsToNext.visibility = View.GONE
            progressTier.visibility = View.GONE
        }

        // Benefits list
        llBenefitsList.removeAllViews()
        data.benefitsList?.forEach { benefit ->
            addBenefitItem(benefit, true)
        }

        // Tier progress (all tiers)
        llTierProgress.removeAllViews()
        data.allTiers?.forEach { tier ->
            addTierProgressItem(tier, data.currentPoints)
        }
    }

    private fun addBenefitItem(benefit: String, isActive: Boolean) {
        val itemView = LayoutInflater.from(this)
            .inflate(R.layout.item_tier_benefit, llBenefitsList, false)
        
        val ivIcon = itemView.findViewById<ImageView>(R.id.ivBenefitIcon)
        val tvBenefit = itemView.findViewById<TextView>(R.id.tvBenefitText)
        
        tvBenefit.text = benefit
        ivIcon.setImageResource(if (isActive) R.drawable.ic_check_circle else R.drawable.ic_lock)
        ivIcon.setColorFilter(
            ContextCompat.getColor(this, 
                if (isActive) R.color.green else R.color.gray)
        )
        
        llBenefitsList.addView(itemView)
    }

    private fun addTierProgressItem(tier: TierInfoDto, currentPoints: Int) {
        val itemView = LayoutInflater.from(this)
            .inflate(R.layout.item_tier_progress, llTierProgress, false)
        
        val ivTierIcon = itemView.findViewById<ImageView>(R.id.ivTierIcon)
        val tvTierName = itemView.findViewById<TextView>(R.id.tvTierProgressName)
        val tvTierPoints = itemView.findViewById<TextView>(R.id.tvTierPoints)
        val tvTierDiscount = itemView.findViewById<TextView>(R.id.tvTierDiscount)
        val viewConnector = itemView.findViewById<View>(R.id.viewConnector)
        
        tvTierName.text = tier.tierName
        tvTierPoints.text = "${tier.minPoints} điểm"
        tvTierDiscount.text = if (tier.discountPercent > 0) 
            "Giảm ${tier.discountPercent.toInt()}%" else "Hạng khởi đầu"
        
        ivTierIcon.setImageResource(getTierBadgeIcon(tier.tier))
        
        // Highlight current tier
        if (tier.isCurrentTier) {
            itemView.setBackgroundResource(R.drawable.bg_tier_current)
            tvTierName.setTextColor(ContextCompat.getColor(this, R.color.primary))
        }
        
        // Dim locked tiers
        if (!tier.isUnlocked) {
            itemView.alpha = 0.5f
        }
        
        // Hide connector for last item
        if (tier.tier == "PLATINUM") {
            viewConnector.visibility = View.GONE
        }
        
        llTierProgress.addView(itemView)
    }

    private fun getTierBadgeIcon(tier: String): Int {
        return when (tier) {
            "BRONZE" -> R.drawable.ic_tier_bronze
            "SILVER" -> R.drawable.ic_tier_silver
            "GOLD" -> R.drawable.ic_tier_gold
            "PLATINUM" -> R.drawable.ic_tier_platinum
            else -> R.drawable.ic_tier_bronze
        }
    }

    private fun adjustAlpha(color: Int, factor: Float): Int {
        val alpha = (Color.alpha(color) * factor).toInt()
        val red = Color.red(color)
        val green = Color.green(color)
        val blue = Color.blue(color)
        return Color.argb(alpha, red, green, blue)
    }
}

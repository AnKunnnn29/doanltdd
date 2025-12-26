package com.example.doan.Utils

import android.Manifest
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.Dialog
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.example.doan.Models.Product
import com.example.doan.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

/**
 * 🎤 Voice Order Dialog
 * Dialog đẹp để đặt hàng bằng giọng nói
 */
class VoiceOrderDialog(
    private val context: Context,
    private val onOrderConfirmed: (Product, Int, String) -> Unit
) : VoiceOrderHelper.VoiceOrderListener {
    
    private var dialog: Dialog? = null
    private var voiceHelper: VoiceOrderHelper? = null
    
    // Views
    private lateinit var ivMic: ImageView
    private lateinit var tvStatus: TextView
    private lateinit var tvSpeechText: TextView
    private lateinit var cardResult: MaterialCardView
    private lateinit var tvProductName: TextView
    private lateinit var tvQuantity: TextView
    private lateinit var tvSize: TextView
    private lateinit var btnConfirm: MaterialButton
    private lateinit var btnRetry: MaterialButton
    private lateinit var pulseView: View
    private lateinit var tvProductSuggestions: TextView
    private lateinit var cardSuggestions: MaterialCardView
    private lateinit var dividerSimilar: View
    private lateinit var tvSimilarTitle: TextView
    private lateinit var layoutSimilarProducts: android.widget.LinearLayout
    private lateinit var layoutQuantitySize: android.widget.LinearLayout
    
    private var pulseAnimator: ObjectAnimator? = null
    private var currentResult: VoiceOrderHelper.VoiceOrderResult? = null
    
    fun show() {
        // Check permission
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) 
            != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(context, "Vui lòng cấp quyền microphone trong Settings", Toast.LENGTH_LONG).show()
            return
        }
        
        dialog = Dialog(context).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            
            val view = LayoutInflater.from(context).inflate(R.layout.dialog_voice_order, null)
            setContentView(view)
            
            initViews(view)
            setupListeners()
            
            setCancelable(true)
            setOnDismissListener { cleanup() }
            
            show()
        }
        
        // Start listening after dialog shown
        Handler(Looper.getMainLooper()).postDelayed({
            startVoiceRecognition()
        }, 500)
    }

    
    private fun initViews(view: View) {
        ivMic = view.findViewById(R.id.iv_mic)
        tvStatus = view.findViewById(R.id.tv_status)
        tvSpeechText = view.findViewById(R.id.tv_speech_text)
        cardResult = view.findViewById(R.id.card_result)
        tvProductName = view.findViewById(R.id.tv_product_name)
        tvQuantity = view.findViewById(R.id.tv_quantity)
        tvSize = view.findViewById(R.id.tv_size)
        btnConfirm = view.findViewById(R.id.btn_confirm)
        btnRetry = view.findViewById(R.id.btn_retry)
        pulseView = view.findViewById(R.id.pulse_view)
        tvProductSuggestions = view.findViewById(R.id.tv_product_suggestions)
        cardSuggestions = view.findViewById(R.id.card_suggestions)
        dividerSimilar = view.findViewById(R.id.divider_similar)
        tvSimilarTitle = view.findViewById(R.id.tv_similar_title)
        layoutSimilarProducts = view.findViewById(R.id.layout_similar_products)
        layoutQuantitySize = view.findViewById(R.id.layout_quantity_size)
        
        // Initial state
        cardResult.visibility = View.GONE
        tvSpeechText.text = ""
        
        // Load top 3 best sellers
        loadTop3BestSellers()
    }
    
    private fun loadTop3BestSellers() {
        val products = DataCache.products ?: return
        
        // Lấy top 3 sản phẩm (giả sử đã được sắp xếp theo bán chạy)
        val top3 = products.take(3).mapNotNull { it.name }
        
        if (top3.isNotEmpty()) {
            val suggestionText = top3.mapIndexed { index, name ->
                "${index + 1}. $name"
            }.joinToString("\n")
            
            tvProductSuggestions.text = suggestionText
        } else {
            cardSuggestions.visibility = View.GONE
        }
    }
    
    private fun setupListeners() {
        ivMic.setOnClickListener {
            startVoiceRecognition()
        }
        
        btnRetry.setOnClickListener {
            cardResult.visibility = View.GONE
            cardSuggestions.visibility = View.VISIBLE
            tvSpeechText.text = ""
            startVoiceRecognition()
        }
        
        btnConfirm.setOnClickListener {
            currentResult?.let { result ->
                if (result.product != null) {
                    onOrderConfirmed(result.product, result.quantity, result.sizeName)
                    dialog?.dismiss()
                    Toast.makeText(context, "Da them ${result.quantity} ${result.product.name} vao gio hang!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun startVoiceRecognition() {
        voiceHelper?.destroy()
        voiceHelper = VoiceOrderHelper(context, this)
        voiceHelper?.startListening()
    }
    
    private fun startPulseAnimation() {
        pulseView.visibility = View.VISIBLE
        
        pulseAnimator = ObjectAnimator.ofFloat(pulseView, "scaleX", 1f, 1.5f).apply {
            duration = 1000
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = AccelerateDecelerateInterpolator()
            
            addUpdateListener {
                pulseView.scaleY = pulseView.scaleX
                pulseView.alpha = 1.5f - pulseView.scaleX
            }
            
            start()
        }
    }
    
    private fun stopPulseAnimation() {
        pulseAnimator?.cancel()
        pulseView.visibility = View.GONE
    }
    
    // VoiceOrderListener callbacks
    override fun onListeningStarted() {
        tvStatus.text = "Dang nghe..."
        tvStatus.setTextColor(ContextCompat.getColor(context, R.color.primary))
        ivMic.setColorFilter(ContextCompat.getColor(context, R.color.primary))
        startPulseAnimation()
    }
    
    override fun onListeningEnded() {
        tvStatus.text = "Nhan mic de noi lai"
        ivMic.setColorFilter(ContextCompat.getColor(context, R.color.text_secondary))
        stopPulseAnimation()
    }
    
    override fun onSpeechResult(text: String) {
        tvSpeechText.text = "\"$text\""
    }
    
    override fun onOrderParsed(result: VoiceOrderHelper.VoiceOrderResult) {
        currentResult = result
        cardResult.visibility = View.VISIBLE
        
        // Ẩn card gợi ý khi có kết quả
        cardSuggestions.visibility = View.GONE
        
        // Clear previous similar products
        layoutSimilarProducts.removeAllViews()
        
        if (result.product != null) {
            // Found product
            tvProductName.text = "✓ ${result.product.name}"
            tvProductName.setTextColor(ContextCompat.getColor(context, R.color.primary))
            tvQuantity.text = "Số lượng: ${result.quantity}"
            tvSize.text = "Size: ${result.sizeName}"
            layoutQuantitySize.visibility = View.VISIBLE
            btnConfirm.isEnabled = true
            
            tvStatus.text = "Đã nhận diện!"
            tvStatus.setTextColor(ContextCompat.getColor(context, R.color.success))
            
            // Show similar products if any
            if (result.similarProducts.isNotEmpty()) {
                showSimilarProducts(result.similarProducts, result.quantity, result.sizeName)
            } else {
                dividerSimilar.visibility = View.GONE
                tvSimilarTitle.visibility = View.GONE
                layoutSimilarProducts.visibility = View.GONE
            }
        } else {
            // Not found - show similar products to choose
            tvProductName.text = "Không tìm thấy chính xác"
            tvProductName.setTextColor(ContextCompat.getColor(context, R.color.error))
            layoutQuantitySize.visibility = View.GONE
            btnConfirm.isEnabled = false
            
            tvStatus.text = "Chọn sản phẩm bên dưới"
            tvStatus.setTextColor(ContextCompat.getColor(context, R.color.warning))
            
            // Show similar products to choose
            if (result.similarProducts.isNotEmpty()) {
                showSimilarProducts(result.similarProducts, result.quantity, result.sizeName)
            } else {
                dividerSimilar.visibility = View.GONE
                tvSimilarTitle.visibility = View.GONE
                layoutSimilarProducts.visibility = View.GONE
            }
        }
    }
    
    private fun showSimilarProducts(products: List<Product>, quantity: Int, sizeName: String) {
        dividerSimilar.visibility = View.VISIBLE
        tvSimilarTitle.visibility = View.VISIBLE
        layoutSimilarProducts.visibility = View.VISIBLE
        
        for (product in products) {
            val itemView = TextView(context).apply {
                text = "• ${product.name}"
                textSize = 14f
                setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                setPadding(0, 8, 0, 8)
                setOnClickListener {
                    // Select this product
                    selectProduct(product, quantity, sizeName)
                }
            }
            layoutSimilarProducts.addView(itemView)
        }
    }
    
    private fun selectProduct(product: Product, quantity: Int, sizeName: String) {
        currentResult = VoiceOrderHelper.VoiceOrderResult(
            product = product,
            similarProducts = emptyList(),
            quantity = quantity,
            sizeName = sizeName,
            originalText = currentResult?.originalText ?: "",
            confidence = 1.0f
        )
        
        // Update UI
        tvProductName.text = "✓ ${product.name}"
        tvProductName.setTextColor(ContextCompat.getColor(context, R.color.primary))
        tvQuantity.text = "Số lượng: $quantity"
        tvSize.text = "Size: $sizeName"
        layoutQuantitySize.visibility = View.VISIBLE
        btnConfirm.isEnabled = true
        
        tvStatus.text = "Đã chọn!"
        tvStatus.setTextColor(ContextCompat.getColor(context, R.color.success))
        
        // Hide similar products
        dividerSimilar.visibility = View.GONE
        tvSimilarTitle.visibility = View.GONE
        layoutSimilarProducts.visibility = View.GONE
    }
    
    override fun onError(message: String) {
        tvStatus.text = message
        tvStatus.setTextColor(ContextCompat.getColor(context, R.color.error))
        stopPulseAnimation()
        
        // Show retry hint
        cardResult.visibility = View.VISIBLE
        tvProductName.text = "Có lỗi xảy ra"
        tvProductName.setTextColor(ContextCompat.getColor(context, R.color.error))
        tvQuantity.text = message
        tvSize.text = ""
        layoutQuantitySize.visibility = View.VISIBLE
        btnConfirm.isEnabled = false
        
        // Hide similar products
        dividerSimilar.visibility = View.GONE
        tvSimilarTitle.visibility = View.GONE
        layoutSimilarProducts.visibility = View.GONE
    }
    
    private fun cleanup() {
        voiceHelper?.destroy()
        voiceHelper = null
        stopPulseAnimation()
    }
    
    fun dismiss() {
        dialog?.dismiss()
    }
}

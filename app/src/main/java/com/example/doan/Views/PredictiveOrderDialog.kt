package com.example.doan.Views

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.view.Window
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.bumptech.glide.Glide
import com.example.doan.Models.PredictedDrink
import com.example.doan.Models.PredictiveOrderResponse
import com.example.doan.R
import com.google.android.material.button.MaterialButton
import java.text.NumberFormat
import java.util.Locale

/**
 * Dialog hiển thị gợi ý món dự đoán cho user
 * Hiển thị khi mở app nếu có prediction phù hợp
 */
class PredictiveOrderDialog(
    context: Context,
    private val prediction: PredictiveOrderResponse,
    private val onAddToCart: (PredictedDrink) -> Unit,
    private val onDismiss: () -> Unit
) : Dialog(context) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_predictive_order)
        
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        setCancelable(true)
        
        setupViews()
    }
    
    private fun setupViews() {
        val drink = prediction.predictedDrink ?: return
        
        // Message
        findViewById<TextView>(R.id.tvPredictionMessage).text = prediction.message
        
        // Drink info
        findViewById<TextView>(R.id.tvDrinkName).text = drink.drinkName
        findViewById<TextView>(R.id.tvDrinkSize).text = drink.sizeName?.let { "Size $it" } ?: ""
        
        // Price
        val priceFormat = NumberFormat.getInstance(Locale("vi", "VN"))
        val priceText = drink.price?.let { "${priceFormat.format(it)}đ" } ?: ""
        findViewById<TextView>(R.id.tvDrinkPrice).text = priceText
        
        // Order count
        findViewById<TextView>(R.id.tvOrderCount).text = "Đã đặt ${drink.orderCount} lần"
        
        // Image
        val ivDrink = findViewById<ImageView>(R.id.ivDrinkImage)
        if (!drink.drinkImage.isNullOrEmpty()) {
            Glide.with(context)
                .load(drink.drinkImage)
                .placeholder(R.drawable.ic_launcher_background)
                .error(R.drawable.ic_launcher_background)
                .centerCrop()
                .into(ivDrink)
        }
        
        // Reasons
        val reasons = prediction.triggerReasons
        if (!reasons.isNullOrEmpty()) {
            findViewById<LinearLayout>(R.id.llReasons).visibility = View.VISIBLE
            val reasonsText = reasons.joinToString("\n") { "• $it" }
            findViewById<TextView>(R.id.tvReasons).text = reasonsText
        }
        
        // Buttons
        findViewById<MaterialButton>(R.id.btnDismiss).setOnClickListener {
            onDismiss()
            dismiss()
        }
        
        findViewById<MaterialButton>(R.id.btnAddToCart).setOnClickListener {
            onAddToCart(drink)
            dismiss()
        }
    }
}

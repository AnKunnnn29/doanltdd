package com.example.doan.Utils

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.Window
import android.widget.TextView
import com.example.doan.R

/**
 * Loading Dialog với animation Lottie cute doggie
 * Sử dụng:
 * val loading = LoadingDialog(context)
 * loading.show() hoặc loading.show("Đang xử lý...")
 * loading.dismiss()
 */
class LoadingDialog(context: Context) {
    
    private val dialog: Dialog = Dialog(context)
    private var tvMessage: TextView
    
    init {
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_loading, null)
        dialog.setContentView(view)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)
        
        tvMessage = view.findViewById(R.id.tv_loading_message)
    }
    
    fun show(message: String = "Đang tải...") {
        tvMessage.text = message
        if (!dialog.isShowing) {
            dialog.show()
        }
    }
    
    fun dismiss() {
        if (dialog.isShowing) {
            dialog.dismiss()
        }
    }
    
    fun setMessage(message: String) {
        tvMessage.text = message
    }
    
    fun isShowing(): Boolean = dialog.isShowing
}

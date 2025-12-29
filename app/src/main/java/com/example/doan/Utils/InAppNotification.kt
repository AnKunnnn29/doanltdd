package com.example.doan.Utils

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView

/**
 * 🔔 InAppNotification - Banner thông báo đẹp trong app
 */
object InAppNotification {

    enum class Type {
        SUCCESS, ERROR, WARNING, INFO, CART, ORDER
    }

    private var currentNotification: View? = null
    private val handler = Handler(Looper.getMainLooper())

    fun show(
        activity: Activity,
        title: String,
        message: String? = null,
        type: Type = Type.INFO,
        duration: Long = 3000L,
        onClick: (() -> Unit)? = null
    ) {
        // Dismiss old notification
        dismissInternal()

        val rootView = activity.window.decorView.findViewById<ViewGroup>(android.R.id.content)
        val density = activity.resources.displayMetrics.density

        // Container
        val container = FrameLayout(activity).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.TOP
                val margin = (16 * density).toInt()
                setMargins(margin, getStatusBarHeight(activity) + margin, margin, 0)
            }
            elevation = 24f
        }

        // Card
        val card = CardView(activity).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
            radius = 20 * density
            cardElevation = 16 * density
            setCardBackgroundColor(Color.TRANSPARENT)
            setOnClickListener {
                onClick?.invoke()
                dismiss()
            }
        }

        // Gradient background
        val gradientBg = GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            getGradientColors(type)
        ).apply {
            cornerRadius = 20 * density
        }

        // Inner container
        val innerContainer = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = gradientBg
            val padding = (16 * density).toInt()
            setPadding(padding, padding, padding, padding)
        }

        // Icon container
        val iconContainer = FrameLayout(activity).apply {
            val size = (48 * density).toInt()
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                marginEnd = (14 * density).toInt()
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#33FFFFFF"))
            }
        }

        // Icon emoji
        val iconText = TextView(activity).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            gravity = Gravity.CENTER
            text = getEmojiForType(type)
            textSize = 22f
        }
        iconContainer.addView(iconText)

        // Text container
        val textContainer = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        // Title
        val titleView = TextView(activity).apply {
            text = title
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            setShadowLayer(2f, 0f, 1f, Color.parseColor("#40000000"))
        }
        textContainer.addView(titleView)

        // Message
        if (!message.isNullOrEmpty()) {
            val messageView = TextView(activity).apply {
                text = message
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                setTextColor(Color.parseColor("#E0FFFFFF"))
                setPadding(0, (4 * density).toInt(), 0, 0)
            }
            textContainer.addView(messageView)
        }

        // Close button
        val closeBtn = TextView(activity).apply {
            val size = (32 * density).toInt()
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                marginStart = (8 * density).toInt()
            }
            gravity = Gravity.CENTER
            text = "✕"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setTextColor(Color.parseColor("#B0FFFFFF"))
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#20FFFFFF"))
            }
            setOnClickListener { dismiss() }
        }

        // Assemble views
        innerContainer.addView(iconContainer)
        innerContainer.addView(textContainer)
        innerContainer.addView(closeBtn)
        card.addView(innerContainer)
        container.addView(card)
        rootView.addView(container)
        
        currentNotification = container

        // Animate in
        container.translationY = -300f
        container.alpha = 0f
        container.scaleX = 0.8f
        container.scaleY = 0.8f

        AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(container, "translationY", -300f, 0f),
                ObjectAnimator.ofFloat(container, "alpha", 0f, 1f),
                ObjectAnimator.ofFloat(container, "scaleX", 0.8f, 1f),
                ObjectAnimator.ofFloat(container, "scaleY", 0.8f, 1f)
            )
            this.duration = 400L
            interpolator = OvershootInterpolator(1.2f)
            start()
        }

        handler.postDelayed({ dismiss() }, duration)
    }

    private fun dismissInternal() {
        val view = currentNotification ?: return
        currentNotification = null
        handler.removeCallbacksAndMessages(null)
        
        AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(view, "translationY", 0f, -200f),
                ObjectAnimator.ofFloat(view, "alpha", 1f, 0f),
                ObjectAnimator.ofFloat(view, "scaleX", 1f, 0.8f),
                ObjectAnimator.ofFloat(view, "scaleY", 1f, 0.8f)
            )
            this.duration = 250L
            start()
        }
        handler.postDelayed({
            (view.parent as? ViewGroup)?.removeView(view)
        }, 250)
    }

    fun dismiss() {
        dismissInternal()
    }

    fun success(activity: Activity, title: String, message: String? = null) {
        show(activity, title, message, Type.SUCCESS, 3000L)
    }

    fun error(activity: Activity, title: String, message: String? = null) {
        show(activity, title, message, Type.ERROR, 4000L)
    }

    fun warning(activity: Activity, title: String, message: String? = null) {
        show(activity, title, message, Type.WARNING, 3500L)
    }

    fun cartAdded(activity: Activity, productName: String) {
        show(activity, "Đã thêm vào giỏ hàng", productName, Type.CART, 2500L)
    }

    fun orderSuccess(activity: Activity, orderId: String) {
        show(
            activity = activity,
            title = "Đặt hàng thành công!",
            message = "Đơn hàng #$orderId đang được xử lý",
            type = Type.ORDER,
            duration = 4000L
        )
    }

    private fun getGradientColors(type: Type): IntArray {
        return when (type) {
            Type.SUCCESS -> intArrayOf(Color.parseColor("#43A047"), Color.parseColor("#66BB6A"))
            Type.ERROR -> intArrayOf(Color.parseColor("#E53935"), Color.parseColor("#EF5350"))
            Type.WARNING -> intArrayOf(Color.parseColor("#FB8C00"), Color.parseColor("#FFA726"))
            Type.INFO -> intArrayOf(Color.parseColor("#1E88E5"), Color.parseColor("#42A5F5"))
            Type.CART -> intArrayOf(Color.parseColor("#931923"), Color.parseColor("#B8293A"))
            Type.ORDER -> intArrayOf(Color.parseColor("#7B1FA2"), Color.parseColor("#AB47BC"))
        }
    }

    private fun getEmojiForType(type: Type): String {
        return when (type) {
            Type.SUCCESS -> "✓"
            Type.ERROR -> "✕"
            Type.WARNING -> "⚠"
            Type.INFO -> "ℹ"
            Type.CART -> "🛒"
            Type.ORDER -> "🎉"
        }
    }

    private fun getStatusBarHeight(activity: Activity): Int {
        val resourceId = activity.resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (resourceId > 0) activity.resources.getDimensionPixelSize(resourceId) else 0
    }
}

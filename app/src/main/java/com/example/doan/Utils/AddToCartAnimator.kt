package com.example.doan.Utils

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AccelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.view.drawToBitmap


object AddToCartAnimator {
    fun animate(
        activity: Activity,
        sourceView: View,
        targetView: View,
        onComplete: (() -> Unit)? = null
    ) {
        val rootView = activity.window.decorView.findViewById<ViewGroup>(android.R.id.content)
        
        // Tạo bitmap từ source view
        val bitmap = try {
            sourceView.drawToBitmap()
        } catch (e: Exception) {
            onComplete?.invoke()
            return
        }

        // Tạo ImageView tạm để animate
        val flyingView = ImageView(activity).apply {
            setImageBitmap(bitmap)
            layoutParams = FrameLayout.LayoutParams(
                sourceView.width,
                sourceView.height
            )
            scaleType = ImageView.ScaleType.CENTER_CROP
        }

        // Lấy vị trí trên màn hình
        val sourceLocation = IntArray(2)
        val targetLocation = IntArray(2)
        sourceView.getLocationOnScreen(sourceLocation)
        targetView.getLocationOnScreen(targetLocation)

        // Đặt vị trí ban đầu
        flyingView.x = sourceLocation[0].toFloat()
        flyingView.y = sourceLocation[1].toFloat()

        // Thêm vào root view
        rootView.addView(flyingView)

        // Tính toán điểm đích
        val targetX = targetLocation[0].toFloat() + targetView.width / 2 - sourceView.width / 2
        val targetY = targetLocation[1].toFloat() + targetView.height / 2 - sourceView.height / 2

        // Animation di chuyển theo đường cong Bezier
        val duration = 600L

        // Scale animation
        val scaleXAnim = ObjectAnimator.ofFloat(flyingView, "scaleX", 1f, 0.3f).apply {
            this.duration = duration
            interpolator = AccelerateInterpolator()
        }
        val scaleYAnim = ObjectAnimator.ofFloat(flyingView, "scaleY", 1f, 0.3f).apply {
            this.duration = duration
            interpolator = AccelerateInterpolator()
        }

        // Alpha animation
        val alphaAnim = ObjectAnimator.ofFloat(flyingView, "alpha", 1f, 0.7f).apply {
            this.duration = duration
        }

        // Path animation (đường cong)
        val controlPointY = minOf(sourceLocation[1], targetLocation[1]) - 200f
        
        val pathAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            this.duration = duration
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { animator ->
                val t = animator.animatedValue as Float
                
                // Quadratic Bezier curve
                val startX = sourceLocation[0].toFloat()
                val startY = sourceLocation[1].toFloat()
                val controlX = (startX + targetX) / 2
                
                flyingView.x = (1 - t) * (1 - t) * startX + 
                              2 * (1 - t) * t * controlX + 
                              t * t * targetX
                flyingView.y = (1 - t) * (1 - t) * startY + 
                              2 * (1 - t) * t * controlPointY + 
                              t * t * targetY
            }
        }

        // Rotation animation
        val rotationAnim = ObjectAnimator.ofFloat(flyingView, "rotation", 0f, 360f).apply {
            this.duration = duration
        }

        // Chạy tất cả animation cùng lúc
        AnimatorSet().apply {
            playTogether(scaleXAnim, scaleYAnim, alphaAnim, pathAnimator, rotationAnim)
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    // Xóa flying view
                    rootView.removeView(flyingView)
                    
                    // Bounce effect cho target view
                    bounceView(targetView)
                    
                    // Callback
                    onComplete?.invoke()
                }
            })
            start()
        }

        // Pulse effect cho source view
        pulseView(sourceView)
    }

    /**
     * Bounce effect cho view
     */
    fun bounceView(view: View) {
        val scaleX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.3f, 1f)
        val scaleY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.3f, 1f)
        
        AnimatorSet().apply {
            playTogether(scaleX, scaleY)
            duration = 300
            interpolator = OvershootInterpolator()
            start()
        }
    }


    fun pulseView(view: View) {
        val scaleX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 0.9f, 1f)
        val scaleY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 0.9f, 1f)
        
        AnimatorSet().apply {
            playTogether(scaleX, scaleY)
            duration = 200
            start()
        }
    }

    fun shakeView(view: View) {
        ObjectAnimator.ofFloat(view, "translationX", 0f, 25f, -25f, 25f, -25f, 15f, -15f, 6f, -6f, 0f).apply {
            duration = 500
            start()
        }
    }

    fun successAnimation(view: View, onComplete: (() -> Unit)? = null) {
        val scaleX = ObjectAnimator.ofFloat(view, "scaleX", 0f, 1.2f, 1f)
        val scaleY = ObjectAnimator.ofFloat(view, "scaleY", 0f, 1.2f, 1f)
        val alpha = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f)
        
        AnimatorSet().apply {
            playTogether(scaleX, scaleY, alpha)
            duration = 400
            interpolator = OvershootInterpolator()
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    onComplete?.invoke()
                }
            })
            start()
        }
    }
}

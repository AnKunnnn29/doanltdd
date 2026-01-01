package com.example.doan.Utils

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup

/**
 * Helper class để làm cho một View có thể kéo thả được
 * Sử dụng cho Quick Actions Card trong HomeFragment
 */
object DraggableViewHelper {
    
    /**
     * Thiết lập khả năng kéo thả cho một View
     * @param view View cần kéo thả
     * @param parentView ViewGroup chứa view (để giới hạn phạm vi kéo)
     */
    @SuppressLint("ClickableViewAccessibility")
    fun makeDraggable(view: View, parentView: ViewGroup? = null) {
        var dX = 0f
        var dY = 0f
        var lastAction = 0
        
        view.setOnTouchListener { v, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    dX = v.x - event.rawX
                    dY = v.y - event.rawY
                    lastAction = MotionEvent.ACTION_DOWN
                    true
                }
                
                MotionEvent.ACTION_MOVE -> {
                    var newX = event.rawX + dX
                    var newY = event.rawY + dY
                    
                    // Giới hạn trong phạm vi parent nếu có
                    parentView?.let { parent ->
                        val maxX = parent.width - v.width
                        val maxY = parent.height - v.height
                        
                        newX = newX.coerceIn(0f, maxX.toFloat())
                        newY = newY.coerceIn(0f, maxY.toFloat())
                    }
                    
                    v.x = newX
                    v.y = newY
                    lastAction = MotionEvent.ACTION_MOVE
                    true
                }
                
                MotionEvent.ACTION_UP -> {
                    // Nếu chỉ là click (không di chuyển nhiều), cho phép click event
                    if (lastAction == MotionEvent.ACTION_DOWN) {
                        v.performClick()
                    }
                    true
                }
                
                else -> false
            }
        }
    }
    
    /**
     * Thiết lập khả năng kéo thả với animation snap to edge
     * View sẽ tự động dính vào cạnh gần nhất khi thả
     */
    @SuppressLint("ClickableViewAccessibility")
    fun makeDraggableWithSnapToEdge(view: View, parentView: ViewGroup) {
        var dX = 0f
        var dY = 0f
        var lastAction = 0
        val margin = 12 // dp margin từ edge
        
        view.setOnTouchListener { v, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    dX = v.x - event.rawX
                    dY = v.y - event.rawY
                    lastAction = MotionEvent.ACTION_DOWN
                    // Scale up khi bắt đầu kéo
                    v.animate().scaleX(1.05f).scaleY(1.05f).setDuration(100).start()
                    true
                }
                
                MotionEvent.ACTION_MOVE -> {
                    var newX = event.rawX + dX
                    var newY = event.rawY + dY
                    
                    // Giới hạn trong phạm vi parent
                    val maxX = parentView.width - v.width - margin
                    val maxY = parentView.height - v.height - margin
                    
                    newX = newX.coerceIn(margin.toFloat(), maxX.toFloat())
                    newY = newY.coerceIn(margin.toFloat(), maxY.toFloat())
                    
                    v.x = newX
                    v.y = newY
                    lastAction = MotionEvent.ACTION_MOVE
                    true
                }
                
                MotionEvent.ACTION_UP -> {
                    // Scale về bình thường
                    v.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                    
                    // Snap to nearest edge (left or right)
                    val parentWidth = parentView.width
                    val viewCenterX = v.x + v.width / 2
                    
                    val targetX = if (viewCenterX < parentWidth / 2) {
                        margin.toFloat() // Snap to left
                    } else {
                        (parentWidth - v.width - margin).toFloat() // Snap to right
                    }
                    
                    // Animate to target position
                    v.animate()
                        .x(targetX)
                        .setDuration(200)
                        .start()
                    
                    // Nếu chỉ là click (không di chuyển nhiều), cho phép click event
                    if (lastAction == MotionEvent.ACTION_DOWN) {
                        v.performClick()
                    }
                    true
                }
                
                else -> false
            }
        }
    }
}

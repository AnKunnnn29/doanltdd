package com.example.doan.Utils

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.google.android.gms.maps.SupportMapFragment


class TouchableMapFragment : SupportMapFragment() {
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val originalView = super.onCreateView(inflater, container, savedInstanceState)
        
        val frameLayout = TouchableWrapper(requireContext())
        frameLayout.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        
        (originalView.parent as? ViewGroup)?.removeView(originalView)
        frameLayout.addView(originalView)
        
        return frameLayout
    }
    
    class TouchableWrapper(context: Context) : FrameLayout(context) {
        
        override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
            when (ev.action) {
                MotionEvent.ACTION_DOWN -> {
                    parent?.requestDisallowInterceptTouchEvent(true)
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    parent?.requestDisallowInterceptTouchEvent(false)
                }
            }
            return super.dispatchTouchEvent(ev)
        }
    }
}

package com.example.doan.Utils

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View

/**
 * Helper class for haptic feedback
 * Provides tactile feedback for user interactions
 */
object HapticFeedbackHelper {

    /**
     * Perform click haptic feedback
     */
    fun performClick(view: View) {
        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
    }

    /**
     * Perform long press haptic feedback
     */
    fun performLongPress(view: View) {
        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
    }

    /**
     * Perform keyboard tap haptic feedback
     */
    fun performKeyboardTap(view: View) {
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }

    /**
     * Perform context click haptic feedback (API 23+)
     */
    fun performContextClick(view: View) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
        } else {
            performClick(view)
        }
    }

    /**
     * Perform confirm haptic feedback (API 30+)
     */
    fun performConfirm(view: View) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        } else {
            performClick(view)
        }
    }

    /**
     * Perform reject haptic feedback (API 30+)
     */
    fun performReject(view: View) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.performHapticFeedback(HapticFeedbackConstants.REJECT)
        } else {
            performClick(view)
        }
    }
}

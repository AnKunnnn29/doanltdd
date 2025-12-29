package com.example.doan.Utils

import android.app.Activity
import android.view.View
import android.view.animation.AnimationUtils
import com.example.doan.R

/**
 * Helper class for animations
 * Provides easy-to-use animation methods
 */
object AnimationHelper {

    /**
     * Apply fade in animation to a view
     */
    fun fadeIn(view: View) {
        val animation = AnimationUtils.loadAnimation(view.context, R.anim.fade_in)
        view.startAnimation(animation)
        view.visibility = View.VISIBLE
    }

    /**
     * Apply fade out animation to a view
     */
    fun fadeOut(view: View) {
        val animation = AnimationUtils.loadAnimation(view.context, R.anim.fade_out)
        view.startAnimation(animation)
        view.visibility = View.GONE
    }

    /**
     * Apply slide up animation to a view
     */
    fun slideUp(view: View) {
        val animation = AnimationUtils.loadAnimation(view.context, R.anim.slide_up)
        view.startAnimation(animation)
        view.visibility = View.VISIBLE
    }

    /**
     * Apply slide down animation to a view
     */
    fun slideDown(view: View) {
        val animation = AnimationUtils.loadAnimation(view.context, R.anim.slide_down)
        view.startAnimation(animation)
        view.visibility = View.GONE
    }

    /**
     * Apply slide in from right animation to a view
     */
    fun slideInRight(view: View) {
        val animation = AnimationUtils.loadAnimation(view.context, R.anim.slide_in_right)
        view.startAnimation(animation)
        view.visibility = View.VISIBLE
    }

    /**
     * Apply slide in from left animation to a view
     */
    fun slideInLeft(view: View) {
        val animation = AnimationUtils.loadAnimation(view.context, R.anim.slide_in_left)
        view.startAnimation(animation)
        view.visibility = View.VISIBLE
    }

    /**
     * Apply scale in animation to a view
     */
    fun scaleIn(view: View) {
        val animation = AnimationUtils.loadAnimation(view.context, R.anim.scale_in)
        view.startAnimation(animation)
        view.visibility = View.VISIBLE
    }

    /**
     * Apply scale out animation to a view
     */
    fun scaleOut(view: View) {
        val animation = AnimationUtils.loadAnimation(view.context, R.anim.scale_out)
        view.startAnimation(animation)
        view.visibility = View.GONE
    }

    /**
     * Apply bounce animation to a view
     */
    fun bounce(view: View) {
        val animation = AnimationUtils.loadAnimation(view.context, R.anim.bounce)
        view.startAnimation(animation)
    }

    /**
     * Apply pulse animation to a view
     */
    fun pulse(view: View) {
        val animation = AnimationUtils.loadAnimation(view.context, R.anim.pulse)
        view.startAnimation(animation)
    }

    /**
     * Apply activity transition - slide right
     */
    fun applySlideRightTransition(activity: Activity) {
        activity.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    /**
     * Apply activity transition - slide left
     */
    fun applySlideLeftTransition(activity: Activity) {
        activity.overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }

    /**
     * Apply activity transition - fade
     */
    fun applyFadeTransition(activity: Activity) {
        activity.overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
    }

    /**
     * Apply activity transition - slide up
     */
    fun applySlideUpTransition(activity: Activity) {
        activity.overridePendingTransition(R.anim.slide_up, R.anim.fade_out)
    }
}

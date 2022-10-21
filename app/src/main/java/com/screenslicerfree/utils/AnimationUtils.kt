package com.screenslicerfree.utils

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.view.View
import android.widget.Button
import android.widget.ImageView

fun rotateImage(view: ImageView?, trigger: Button?) {
    val animator = ObjectAnimator.ofFloat(view, View.ROTATION,-360f*3,0f)
    animator.duration= 500
    trigger?.let {
        animator.disableDuringAnimation(trigger)
    }

    animator.start()
}

fun rotateImage(view: ImageView?, trigger: ImageView?, rounds: Float = -360f*3) {
    val animator = ObjectAnimator.ofFloat(view, View.ROTATION,rounds,0f)
    animator.duration= 500
    trigger?.let {
        animator.disableDuringAnimation(trigger)
    }

    animator.start()
}

private fun ObjectAnimator.disableDuringAnimation(view: View){
    addListener(object : AnimatorListenerAdapter(){
        override fun onAnimationStart(animation: Animator) {
            super.onAnimationStart(animation)
            view.isEnabled = false
        }

        override fun onAnimationEnd(animation: Animator) {
            super.onAnimationEnd(animation)
            view.isEnabled = true
        }
    })
}
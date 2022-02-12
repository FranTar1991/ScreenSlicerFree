package com.screenslicerfree.gestures.action

import android.content.Context
import android.util.Log
import android.view.MotionEvent
import com.screenslicerfree.R


import kotlin.math.abs

enum class AllModes {SWIPE,NONE}

abstract class SimpleTwoFingerSwipeDetector(val context: Context) {

    private var p2StopY: Float = 0f
    private var p1StopY: Float = 0f
    private var p2StartY: Float = 0f
    private var p1StartY: Float = 0f
    private val DOUBLE_SWIPE_THRESHOLD: Long = context.resources.getDimensionPixelSize(R.dimen.another_finger_font_size).toLong()
    private lateinit var mode: AllModes
    fun onTouchEvent(event: MotionEvent): Boolean {

        //with the getPointerCount() i'm able to get the count of fingures
        if (event.pointerCount > 1) {
            when (event.action and MotionEvent.ACTION_MASK) {
                MotionEvent.ACTION_POINTER_DOWN -> {
                    // This happens when you touch the screen with two fingers
                    mode = AllModes.SWIPE
                    // event.getY(1) is for the second finger
                    p1StartY = event.getY(0)
                    p2StartY = event.getY(1)
                }
                MotionEvent.ACTION_POINTER_UP -> {
                    // This happens when you release the second finger
                    mode = AllModes.NONE
                    val p1Diff: Float = p1StartY - p1StopY
                    val p2Diff: Float = p2StartY - p2StopY

                    //this is to make sure that fingers go in same direction and
                    // swipe have certain length to consider it a swipe
                    if (abs(p1Diff) > DOUBLE_SWIPE_THRESHOLD && abs(p2Diff) > DOUBLE_SWIPE_THRESHOLD &&
                        (p1Diff > 0 && p2Diff > 0 || p1Diff < 0 && p2Diff < 0)
                    ) {
                        if (p1StartY > p1StopY) {

                            onTwoFingerSwipeDetector(event)
                        } else {

                            onTwoFingerSwipeDetector(event)
                        }
                    }
                    mode = AllModes.NONE
                }
                MotionEvent.ACTION_MOVE -> if (mode == AllModes.SWIPE) {
                    p1StopY = event.getY(0)
                    p2StopY = event.getY(1)
                }
            }
        } else if (event.pointerCount == 1) {
            //this is single swipe, I have implemented onFling() here
        }

        return false
     }

    abstract fun onTwoFingerSwipeDetector(event: MotionEvent?)
    }


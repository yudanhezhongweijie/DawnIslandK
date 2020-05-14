package com.laotoua.dawnislandk.ui.widget

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.widget.FrameLayout
import androidx.core.view.GestureDetectorCompat

class GestureInterceptorFrameLayout : FrameLayout {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    private var mDetector: GestureDetectorCompat? = null

    fun bindGestureDetector(simpleOnGestureListener: GestureDetector.SimpleOnGestureListener) {
        mDetector = GestureDetectorCompat(context, simpleOnGestureListener)
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        /**
         * This method determines whether we have intercepted the motion by using a GestureListener
         * GestureListener will detect and *CONSUME* the event if matched
         */
        return mDetector?.onTouchEvent(ev) ?: false
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        throw Exception("MotionEvent should be intercepted and handled in onInterceptTouchEvent")
    }

}
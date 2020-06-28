package com.laotoua.dawnislandk.screens.widgets

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.FrameLayout
import timber.log.Timber
import kotlin.math.abs

/**
 *  Intercept fling swipes gesture before other listener handles it
 *  i.e. VP2 consumes events which has the same scroll direction https://issuetracker.google.com/issues/154751401
 */
class FlingInterceptor : FrameLayout {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    private var swipeLastX: Float = 0f
    private var swipeStartX: Float = 0f
    private var swipeCancel = false

    private var xDistance: Float = 0f
    private var yDistance: Float = 0f
    private var xLast: Float = 0f
    private var yLast: Float = 0f
    private val touchSlop: Int = android.view.ViewConfiguration.get(context).scaledTouchSlop
    private val swipeSlop: Int = 48
    private var isTouching = false

    private var listener: (() -> Unit)? = null

    fun bindListener(func: () -> Unit) {
        listener = func
    }

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        var handled = false
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                swipeStartX = event.x
                swipeLastX = 0f
                swipeCancel = false
                yDistance = 0f
                xDistance = yDistance
                xLast = event.x
                yLast = event.y
            }
            MotionEvent.ACTION_MOVE -> {
                val curX = event.x
                val curY = event.y
                xDistance = curX - xLast
                yDistance = abs(curY - yLast)
                xLast = curX
                yLast = curY
                handled = xDistance >= touchSlop && yDistance * 4 < xDistance * 3
            }
        }


        // Fixed: IllegalArgumentException: pointerIndex out of range
        return try {
            handled || super.onInterceptTouchEvent(event)
        } catch (e: Throwable) {
            Timber.e(e)
            return handled
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(ev: MotionEvent): Boolean {
        val action = ev.action
        val x = ev.x
        when (action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_CANCEL -> isTouching = true
            MotionEvent.ACTION_MOVE -> {
                isTouching = true
                // 如果手指往回收，则取消开启侧栏
                swipeCancel = x < swipeLastX
                swipeLastX = x
            }
            MotionEvent.ACTION_UP -> {
                if (!swipeCancel && (x - swipeStartX) > swipeSlop) {
                    listener?.invoke()
                }
                isTouching = false
            }
        }
        return super.onTouchEvent(ev)
    }
}
package com.laotoua.dawnislandk.screens.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.laotoua.dawnislandk.screens.util.Layout
import kotlin.math.abs

/*
 * Copyright 2015 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

class ThicknessPreviewView : View {
    private var mInitPath = false
    private var mPath: Path? = null
    private var mPaint: Paint? = null
    private var mX = 0f
    private var mY = 0f
    private var mIsDot = false

    constructor(context: Context?) : super(context) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(
        context,
        attrs
    ) {
        init()
    }

    constructor(
        context: Context?,
        attrs: AttributeSet?,
        defStyleAttr: Int
    ) : super(context, attrs, defStyleAttr) {
        init()
    }

    private fun init() {
        mPath = Path()
        mPaint = Paint()
        mPaint!!.isAntiAlias = true
        mPaint!!.isDither = true
        mPaint!!.style = Paint.Style.STROKE
        mPaint!!.strokeJoin = Paint.Join.ROUND
        mPaint!!.strokeCap = Paint.Cap.ROUND
    }

    fun setColor(color: Int) {
        mPaint!!.color = color
        invalidate()
    }

    fun setThickness(thickness: Int) {
        mPaint!!.strokeWidth = thickness.toFloat()
        invalidate()
    }

    private fun touchStart(x: Float, y: Float) {
        mPath!!.reset()
        mPath!!.moveTo(x, y)
        mX = x
        mY = y
        mIsDot = true
    }

    private fun touchMove(x: Float, y: Float) {
        val dx = abs(x - mX)
        val dy = abs(y - mY)
        if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
            mIsDot = false
            mPath!!.quadTo(mX, mY, (x + mX) / 2, (y + mY) / 2)
            mX = x
            mY = y
        }
    }

    private fun touchUp() {
        mPath!!.lineTo(mX, mY)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                touchStart(x, y)
                invalidate()
            }
            MotionEvent.ACTION_MOVE -> {
                touchMove(x, y)
                invalidate()
            }
            MotionEvent.ACTION_UP -> {
                touchUp()
                invalidate()
            }
        }
        return true
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (!mInitPath) {
            mInitPath = true
            val padding: Int = Layout.dip2px(context, 16f)
            val path = mPath
            path!!.reset()
            path.moveTo(padding.toFloat(), h - padding.toFloat())
            path.cubicTo(
                padding.toFloat(),
                h / 2.toFloat(),
                w - padding.toFloat(),
                h / 2.toFloat(),
                w - padding.toFloat(),
                padding.toFloat()
            )
            mIsDot = false
            invalidate()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (mIsDot) {
            canvas.drawPoint(mX, mY, mPaint!!)
        } else {
            canvas.drawPath(mPath!!, mPaint!!)
        }
    }

    companion object {
        private const val TOUCH_TOLERANCE = 4f
    }
}
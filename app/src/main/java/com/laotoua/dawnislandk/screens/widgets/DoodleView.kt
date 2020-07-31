/*
 *  Copyright 2020 Fishballzzz
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package com.laotoua.dawnislandk.screens.widgets

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.net.Uri
import android.os.Environment
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.annotation.Nullable
import androidx.appcompat.app.AppCompatActivity
import com.laotoua.dawnislandk.R
import com.laotoua.dawnislandk.screens.util.Layout
import com.laotoua.dawnislandk.util.ImageUtil
import com.laotoua.dawnislandk.util.ReadableTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.util.*
import kotlin.math.abs

class DoodleView : View {

    private var mBitmap: Bitmap? = null

    private var mCanvas: Canvas? = null
    private var mBitmapPaint: Paint? = null
    private var mPath: Path? = null
    private var mPaint: Paint? = null

    private var mInsertBitmap: Bitmap? = null
    private var mOffsetX = 0
    private var mOffsetY = 0
    private var mBgColor = 0
    var paintColor = 0
    var paintThickness = 0
    private var mIsDot = false
    private var mPathDone = false
    private var mPointCount = 0
    private var mX = 0f
    private var mY = 0f
    private var mEraser = false
    private val mDst = Rect()
    private var mRecycler: Recycler? = null

    private var mHelper: Helper? = null


    private var isLocked: Boolean = false

    constructor(context: Context) : super(context) {
        init(context)
    }

    constructor(
        context: Context,
        attrs: AttributeSet?
    ) : super(context, attrs) {
        init(context)
    }

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int
    ) : super(context, attrs, defStyleAttr) {
        init(context)
    }

    private fun init(context: Context) {
        mBitmapPaint =
            Paint(Paint.DITHER_FLAG or Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
//        mBgColor = ResourcesUtils.getAttrColor(context, R.attr.colorPure)
//        paintColor = ResourcesUtils.getAttrColor(context, R.attr.colorPureInverse)

        mBgColor = resources.getColor(R.color.pure_light, null)
        paintColor = resources.getColor(R.color.pure_dark, null)
        paintThickness = Layout.dip2px(context, 4.5f)
        mPath = Path()
        mPaint = Paint()
        mPaint!!.isAntiAlias = true
        mPaint!!.isDither = true
        mPaint!!.style = Paint.Style.STROKE
        mPaint!!.strokeJoin = Paint.Join.ROUND
        mPaint!!.strokeCap = Paint.Cap.ROUND
        mRecycler = Recycler()
    }

    fun setEraser(eraser: Boolean) {
        mEraser = eraser
    }

    fun setHelper(helper: Helper?) {
        mHelper = helper
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        onResize(w, h)
    }

    private fun onResize(width: Int, height: Int) {
        clearStore()
        if (mBitmap != null) {
            mBitmap!!.recycle()
        }
        val bitmapWidth: Int
        val bitmapHeight: Int
        if (mInsertBitmap == null) {
            bitmapWidth = width
            bitmapHeight = height
            mOffsetX = 0
            mOffsetY = 0
        } else {
            val insertWidth = mInsertBitmap!!.width
            val insertHeight = mInsertBitmap!!.height
            val insertScale =
                insertWidth.toFloat() / insertHeight.toFloat()
            val scale = width.toFloat() / height.toFloat()
            if (insertScale > scale) {
                bitmapWidth = width
                bitmapHeight = (bitmapWidth / insertScale).toInt()
            } else {
                bitmapHeight = height
                bitmapWidth = (bitmapHeight * insertScale).toInt()
            }
            mOffsetX = (width - bitmapWidth) / 2
            mOffsetY = (height - bitmapHeight) / 2
        }
        mBitmap = Bitmap.createBitmap(
            bitmapWidth,
            bitmapHeight,
            Bitmap.Config.ARGB_8888
        )
        mCanvas = Canvas(mBitmap as Bitmap)
        if (mInsertBitmap == null) {
            mCanvas!!.drawColor(mBgColor)
        } else {
            mDst[0, 0, bitmapWidth] = bitmapHeight
            mCanvas!!.drawBitmap(mInsertBitmap!!, null, mDst, mBitmapPaint)
        }
    }

    override fun onDraw(canvas: Canvas) {
        if (mBitmap == null) {
            return
        }
        canvas.drawBitmap(mBitmap!!, mOffsetX.toFloat(), mOffsetY.toFloat(), mBitmapPaint)
        val saved = canvas.save()
        canvas.translate(mOffsetX.toFloat(), mOffsetY.toFloat())
        canvas.clipRect(0, 0, mBitmap!!.width, mBitmap!!.height)
        drawStore(canvas, mPaint)
        mPaint!!.color = if (mEraser) mBgColor else paintColor
        mPaint!!.strokeWidth = paintThickness.toFloat()
        if (mIsDot) {
            canvas.drawPoint(mX, mY, mPaint!!)
        } else {
            canvas.drawPath(mPath!!, mPaint!!)
        }
        canvas.restoreToCount(saved)
    }

    private fun motionToPath(
        event: MotionEvent,
        path: Path?
    ) {
        when (event.pointerCount) {
            2 -> {
                val x0 = event.getX(0) - mOffsetX
                val y0 = event.getY(0) - mOffsetY
                val x1 = event.getX(1) - mOffsetX
                val y1 = event.getY(1) - mOffsetY
                path!!.reset()
                path.moveTo(x0, y0)
                path.lineTo(x1, y1)
            }
            3 -> {
                val x0 = event.getX(0) - mOffsetX
                val y0 = event.getY(0) - mOffsetY
                val x1 = event.getX(1) - mOffsetX
                val y1 = event.getY(1) - mOffsetY
                val x2 = event.getX(2) - mOffsetX
                val y2 = event.getY(2) - mOffsetY
                path!!.reset()
                path.moveTo(x0, y0)
                path.quadTo(x2, y2, x1, y1)
            }
            4 -> {
                val x0 = event.getX(0) - mOffsetX
                val y0 = event.getY(0) - mOffsetY
                val x1 = event.getX(1) - mOffsetX
                val y1 = event.getY(1) - mOffsetY
                val x2 = event.getX(2) - mOffsetX
                val y2 = event.getY(2) - mOffsetY
                val x3 = event.getX(3) - mOffsetX
                val y3 = event.getY(3) - mOffsetY
                path!!.reset()
                path.moveTo(x0, y0)
                path.cubicTo(x2, y2, x3, y3, x1, y1)
            }
        }
    }

    private fun touchDown(event: MotionEvent) {
        if (event.pointerCount == 1) {
            mIsDot = true
            val x = event.x - mOffsetX
            val y = event.y - mOffsetY
            mPath!!.reset()
            mPath!!.moveTo(x, y)
            mX = x
            mY = y
        } else {
            mIsDot = false
            motionToPath(event, mPath)
        }
    }

    private fun touchMove(event: MotionEvent) {
        if (event.pointerCount == 1) {
            val x = event.x - mOffsetX
            val y = event.y - mOffsetY
            // Check mIsDot
            if (mIsDot) {
                val dx = abs(x - mX)
                val dy = abs(y - mY)
                mIsDot =
                    dx < TOUCH_TOLERANCE && dy < TOUCH_TOLERANCE
            }
            if (!mIsDot) {
                mPath!!.quadTo(mX, mY, (x + mX) / 2, (y + mY) / 2)
                mX = x
                mY = y
            }
        } else {
            mIsDot = false
            motionToPath(event, mPath)
        }
    }

    private fun touchUp(pointCount: Int) {
        // Skip empty path
        if (mPath!!.isEmpty) {
            return
        }

        // End mPath for single finger
        if (pointCount == 1) {
            mPath!!.lineTo(mX, mY)
        }
        var drawInfo: DrawInfo? = mRecycler!!.obtain()
        if (drawInfo == null) {
            drawInfo = DrawInfo()
        }
        drawInfo[if (mEraser) mBgColor else paintColor, paintThickness.toFloat(), mPath, mX, mY] =
            mIsDot
        val legacy: DrawInfo? = push(drawInfo)

        // Draw legacy
        if (legacy != null) {
            legacy.draw(mCanvas!!, mPaint!!)
            mRecycler!!.release(legacy)
        }

        // Rest path
        mIsDot = false
        mPath!!.reset()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (isLocked) {
            return true
        }
        var pointCount = event.pointerCount
        val actionMasked = event.actionMasked
        if (actionMasked == MotionEvent.ACTION_UP || actionMasked == MotionEvent.ACTION_CANCEL || actionMasked == MotionEvent.ACTION_POINTER_UP
        ) {
            --pointCount
        }
        val oldPointCount = mPointCount
        if (pointCount > oldPointCount ||
            event.actionMasked == MotionEvent.ACTION_DOWN
        ) {
            mPathDone = false
        }
        mPointCount = pointCount
        if (mPathDone) {
            return true
        }

        // If the user has drawn with finger before, not dot, save it now
        if (oldPointCount == 1 && pointCount > 1 && !mIsDot) {
            touchUp(1)
        }
        when (actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                touchDown(event)
                invalidate()
            }
            MotionEvent.ACTION_MOVE -> {
                touchMove(event)
                invalidate()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_POINTER_UP -> {
                mPathDone = true
                touchUp(event.pointerCount)
                invalidate()
            }
        }
        return true
    }

    fun clear() {
        if (isLocked || mCanvas == null || mBitmap == null) {
            return
        }
        if (mInsertBitmap == null) {
            mCanvas!!.drawColor(mBgColor)
        } else {
            mDst[0, 0, mBitmap!!.width] = mBitmap!!.height
            mCanvas!!.drawBitmap(mInsertBitmap!!, null, mDst, mBitmapPaint)
        }
        mPath!!.reset()
        clearStore()
        invalidate()
    }

    fun hasInsertBitmap(): Boolean {
        return mInsertBitmap != null
    }

    fun insertBitmap(@Nullable bitmap: Bitmap?) {
        if (mInsertBitmap != null) {
            mInsertBitmap!!.recycle()
        }
        mInsertBitmap = bitmap
        onResize(width, height)
        invalidate()
    }

    suspend fun save(callerActivity: AppCompatActivity) {
        if (isLocked) {
            return
        }
        isLocked = true
        val res = withContext(Dispatchers.IO) {
            drawStore(mCanvas, mPaint)
            Timber.i("Saving Doodle to Gallery... ")
            val timeStamp: String = ReadableTime.getCurrentTimeFileName()
            val relativeLocation =
                Environment.DIRECTORY_PICTURES + File.separator + "Dawn"
            val fileName = "Doodle_$timeStamp.png"
            ImageUtil.writeBitmapToGallery(
                callerActivity, fileName, relativeLocation,
                mBitmap!!
            )
        }
        isLocked = false
        mHelper?.onSavingFinished(res)
    }


    private var mStop = 0
    private var mSize = 0
    private val mData = arrayOfNulls<DrawInfo>(CAPACITY)

    fun canUndo(): Boolean {
        return mStop > 0
    }

    fun canRedo(): Boolean {
        return mStop < mSize
    }

    fun undo() {
        if (isLocked) {
            return
        }
        if (mStop > 0) {
            mStop--
            invalidate()
            mHelper?.onStoreChange(this)

        }
    }

    fun redo() {
        if (isLocked) {
            return
        }
        if (mStop < mSize) {
            mStop++
            invalidate()
            mHelper?.onStoreChange(this)

        }
    }

    private fun drawStore(
        canvas: Canvas?,
        paint: Paint?
    ) {
        for (i in 0 until mStop) {
            mData[i]?.draw(canvas!!, paint!!)
        }
    }

    private fun push(drawInfo: DrawInfo?): DrawInfo? {
        val data: Array<DrawInfo?> = mData
        return when {
            mStop != mSize -> {
                // Release from mStop to mSize
                for (i in mStop until mSize) {
                    mRecycler!!.release(data[i])
                    data[i] = null
                }
                data[mStop] = drawInfo
                mStop++
                mSize = mStop
                mHelper?.onStoreChange(this)

                null
            }
            mSize == CAPACITY -> {
                // It is Full
                val legacy: DrawInfo? = data[0]
                System.arraycopy(data, 1, data, 0, CAPACITY - 1)
                data[CAPACITY - 1] = drawInfo
                legacy
            }
            else -> {
                data[mStop] = drawInfo
                mStop++
                mSize++
                mHelper?.onStoreChange(this)
                null
            }
        }
    }

    private fun clearStore() {
        val data: Array<DrawInfo?> = mData
        for (i in 0 until mSize) {
            mRecycler!!.release(data[i])
            data[i] = null
        }
        mStop = 0
        mSize = 0
        mHelper?.onStoreChange(this)
    }

    private class DrawInfo {
        private var mColor = 0
        private var mWidth = 0f
        private val mPath: Path = Path()
        private var mStartX = 0f
        private var mStartY = 0f
        private var mIsDot = false
        operator fun set(
            color: Int,
            width: Float,
            path: Path?,
            startX: Float,
            startY: Float,
            isDot: Boolean
        ) {
            mColor = color
            mWidth = width
            mPath.set(path!!)
            mStartX = startX
            mStartY = startY
            mIsDot = isDot
        }

        fun draw(canvas: Canvas, paint: Paint) {
            paint.color = mColor
            paint.strokeWidth = mWidth
            if (mIsDot) {
                canvas.drawPoint(mStartX, mStartY, paint)
            } else {
                canvas.drawPath(mPath, paint)
            }
        }

    }

    private class Recycler {
        private var mSize = 0
        private val mStack: Stack<DrawInfo> =
            Stack()

        @Nullable
        fun obtain(): DrawInfo? {
            return if (mSize != 0) {
                mSize--
                mStack.pop()
            } else {
                null
            }
        }

        fun release(@Nullable item: DrawInfo?) {
            if (item == null) {
                return
            }
            if (mSize < CAPACITY) {
                mSize++
                mStack.push(item)
            }
        }
    }

    interface Helper {
        fun onStoreChange(view: DoodleView?)
        fun onSavingFinished(savedUri: Uri?)
    }

    companion object {
        private const val TOUCH_TOLERANCE = 4f
        private const val CAPACITY = 20
    }
}
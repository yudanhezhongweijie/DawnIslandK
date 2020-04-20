package com.laotoua.dawnislandk.ui.span

import android.graphics.*
import android.graphics.Paint.FontMetricsInt
import android.text.style.ReplacementSpan

class RoundBackgroundColorSpan(private val bgColor: Int, private val textColor: Int) :
    ReplacementSpan() {
    var radiu = 20
    var size = 0
    var top = 0
    var bottom = 0
    var first = true
    var height = 2
    override fun getSize(
        paint: Paint,
        text: CharSequence,
        start: Int,
        end: Int,
        fm: FontMetricsInt?
    ): Int {
        if (first) {
            val tempFM = paint.fontMetricsInt
            var height = tempFM.bottom - tempFM.top
            height /= 6
            top = tempFM.top - height
            bottom = tempFM.bottom + height
            radiu = (bottom - top) / 2
            first = false
        }
        if (fm != null && fm.top != 0) {
            fm.top = top
            fm.bottom = bottom
        }
        size = paint.measureText(text, start, end).toInt() + radiu * 2
        return size
    }

    override fun draw(
        canvas: Canvas,
        text: CharSequence,
        start: Int,
        end: Int,
        x: Float,
        top: Int,
        y: Int,
        bottom: Int,
        paint: Paint
    ) {
        val color1 = paint.color
        val alpht = paint.alpha
        val defaultShader = paint.shader
        paint.alpha = 255
        val linearGradient = LinearGradient(
            0f,
            top.toFloat(),
            (x + paint.measureText(text, start, end).toInt() + radiu * 2),
            bottom.toFloat(),
            Color.parseColor("#2195da"),
            Color.parseColor("#3ae4cd"),
            Shader.TileMode.CLAMP
        )
        paint.shader = linearGradient
        canvas.drawRoundRect(
            RectF(
                0f,
                top.toFloat(),
                x + (paint.measureText(text, start, end).toInt() + radiu * 2),
                bottom.toFloat()
            ), radiu.toFloat(), radiu.toFloat(), paint
        )
        paint.shader = defaultShader
        paint.color = textColor
        canvas.drawText(text, start, end, x + radiu, y.toFloat(), paint)
        paint.color = color1
        paint.alpha = alpht
    }

    companion object {
        private const val TAG = "RoundBackgroundColorSpa"
    }

}
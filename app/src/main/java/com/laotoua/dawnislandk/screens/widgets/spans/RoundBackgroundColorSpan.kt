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

package com.laotoua.dawnislandk.screens.widgets.spans

import android.graphics.*
import android.graphics.Paint.FontMetricsInt
import android.text.style.ReplacementSpan

class RoundBackgroundColorSpan(private val bgColor1Str: String = "#2195da",
                               private val bgColor2Str: String = "#3ae4cd",
                               private val textColorStr: String = "#FFFFFF") :
    ReplacementSpan() {
    private var radius = 20
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
            radius = (bottom - top) / 2
            first = false
        }
        if (fm != null && fm.top != 0) {
            fm.top = top
            fm.bottom = bottom
        }
        size = paint.measureText(text, start, end).toInt() + radius * 2
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
        val alpha = paint.alpha
        val defaultShader = paint.shader
        paint.alpha = 255
        val linearGradient = LinearGradient(
            0f,
            top.toFloat(),
            (x + paint.measureText(text, start, end).toInt() + radius * 2),
            bottom.toFloat(),
            Color.parseColor(bgColor1Str),
            Color.parseColor(bgColor2Str),
            Shader.TileMode.CLAMP
        )
        paint.shader = linearGradient
        canvas.drawRoundRect(
            RectF(
                0f,
                top.toFloat(),
                x + (paint.measureText(text, start, end).toInt() + radius * 2),
                bottom.toFloat()
            ), radius.toFloat(), radius.toFloat(), paint
        )
        paint.shader = defaultShader
        paint.color = Color.parseColor(textColorStr)
        canvas.drawText(text, start, end, x + radius, y.toFloat(), paint)
        paint.color = color1
        paint.alpha = alpha
    }

}
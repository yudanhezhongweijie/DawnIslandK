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

import android.text.Selection
import android.text.Spannable
import android.text.method.BaseMovementMethod
import android.text.style.ClickableSpan
import android.view.MotionEvent
import android.widget.TextView

/** 版权声明：本文为CSDN博主「陈蒙_」的原创文章，遵循CC 4.0 BY-SA版权协议，转载请附上原文出处链接及本声明。
 * 原文链接：https://blog.csdn.net/zhaizu/java/article/details/51038113
 */


class ClickableMovementMethod : BaseMovementMethod() {

    companion object {
        private var sInstance: ClickableMovementMethod? = null
        fun getInstance(): ClickableMovementMethod {
            if (sInstance == null) {
                sInstance = ClickableMovementMethod()
            }
            return sInstance as ClickableMovementMethod
        }
    }

    override fun onTouchEvent(widget: TextView, buffer: Spannable, event: MotionEvent): Boolean {

        val action = event.actionMasked
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_DOWN) {

            var x = event.x
            var y = event.y
            x -= widget.totalPaddingLeft
            y -= widget.totalPaddingTop
            x += widget.scrollX
            y += widget.scrollY

            val layout = widget.layout
            val line = layout.getLineForVertical(y.toInt())
            val off = layout.getOffsetForHorizontal(line, x)

            val link: Array<ClickableSpan> =
                buffer.getSpans(off, off, ClickableSpan::class.java)
            if (link.isNotEmpty()) {
                if (action == MotionEvent.ACTION_UP) {
                    link[0].onClick(widget)
                } else {
                    Selection.setSelection(
                        buffer, buffer.getSpanStart(link[0]),
                        buffer.getSpanEnd(link[0])
                    )
                }
                return true
            } else {
                Selection.removeSelection(buffer)
            }
        }

        return false
    }

    override fun initialize(widget: TextView, text: Spannable) {
        Selection.removeSelection(text)
    }
}

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
import android.text.Spanned
import android.text.style.ClickableSpan
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatTextView

class LinkifyTextView : AppCompatTextView {
    var currentSpan: ClickableSpan? = null
        private set
    var currentMotionEvent: MotionEvent? = null
        private set

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(
        context,
        attrs
    )

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int
    ) : super(context, attrs, defStyleAttr)

    fun clearCurrentSpan() {
        currentSpan = null
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        // Let the parent or grandparent of TextView to handles click action.
        // Otherwise click effect like ripple will not work, and if touch area
        // do not contain a url, the TextView will still get MotionEvent.
        // onTouchEven must be called with MotionEvent.ACTION_DOWN for each touch
        // action on it, so we analyze touched url here.
        if (event.action == MotionEvent.ACTION_DOWN) {
            currentSpan = null
            currentMotionEvent = event
            if (text is Spanned) {
                var x = event.x.toInt()
                var y = event.y.toInt()
                x -= totalPaddingLeft
                y -= totalPaddingTop
                x += scrollX
                y += scrollY
                val line = layout.getLineForVertical(y)
                val off = layout.getOffsetForHorizontal(line, x.toFloat())
                val spans = (text as Spanned).getSpans(off, off, ClickableSpan::class.java)
                for (span in spans) {
                    currentSpan = span
                    currentMotionEvent = null
                    break
                }
            }
        }
        return super.onTouchEvent(event)
    }
}
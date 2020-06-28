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

import android.text.TextPaint
import android.text.style.ClickableSpan
import android.view.View

class ReferenceSpan(val id: String, private val clickListener: ReferenceClickHandler? = null) :
    ClickableSpan() {
    override fun onClick(widget: View) {
        clickListener?.handleReference(id)
    }

    override fun updateDrawState(ds: TextPaint) {}

    interface ReferenceClickHandler{
        fun handleReference(id:String)
    }
}
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

import android.view.View

class DoubleClickListener(
    private val doubleClickTimeLimitMills: Long = 400,
    private val callback: DoubleClickCallBack
) :
    View.OnClickListener {
    private var lastClicked: Long = -1L

    override fun onClick(v: View?) {
        lastClicked = when {
            lastClicked == -1L -> {
                System.currentTimeMillis()
            }
            isDoubleClicked() -> {
                callback.doubleClicked()
                -1L
            }
            else -> {
                System.currentTimeMillis()
            }
        }
    }

    private fun getTimeDiff(from: Long, to: Long): Long = to - from

    private fun isDoubleClicked(): Boolean {
        return getTimeDiff(lastClicked, System.currentTimeMillis()) <= doubleClickTimeLimitMills
    }

    interface DoubleClickCallBack {
        fun doubleClicked()
    }
}
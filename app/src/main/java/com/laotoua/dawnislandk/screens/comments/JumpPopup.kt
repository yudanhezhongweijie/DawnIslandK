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

package com.laotoua.dawnislandk.screens.comments

import android.content.Context
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.widget.doOnTextChanged
import com.afollestad.materialdialogs.MaterialDialog
import com.laotoua.dawnislandk.DawnApp.Companion.applicationDataStore
import com.laotoua.dawnislandk.R
import com.laotoua.dawnislandk.util.lazyOnMainOnly
import com.lxj.xpopup.core.CenterPopupView

class JumpPopup(context: Context) : CenterPopupView(context) {
    private var currentPage = 1
    private var maxPage = 1
    var submit = false
    var targetPage = 1

    private val pageInput by lazyOnMainOnly { findViewById<EditText>(R.id.pageInput) }
    private val submitButton by lazyOnMainOnly { findViewById<Button>(R.id.submit) }

    override fun getImplLayoutId(): Int {
        return R.layout.popup_jump
    }

    override fun onCreate() {
        super.onCreate()

        findViewById<EditText>(R.id.pageInput).doOnTextChanged { text, _, _, _ ->
            submitButton.isEnabled =
                !(text.isNullOrBlank()
                        || text.length > maxPage.toString().length
                        || text.toString().toInt() > maxPage)
        }

        findViewById<ImageButton>(R.id.firstPage).setOnClickListener {
            targetPage = 1
            updateInput()
        }

        findViewById<ImageButton>(R.id.lastPage).setOnClickListener {
            targetPage = maxPage
            updateInput()
        }

        findViewById<Button>(R.id.cancel).setOnClickListener {
            submit = false
            dismiss()
        }

        submitButton.setOnClickListener {
            submit = true
            targetPage = pageInput.text.toString().toInt()
            if (applicationDataStore.firstCookieHash == null && targetPage > 99) {
                MaterialDialog(context).show {
                    message(R.string.need_cookie_to_read)
                    positiveButton(R.string.acknowledge)
                }
            } else {
                dismiss()
            }
        }
    }

    private fun updateInput() {
        pageInput.setText(targetPage.toString(), TextView.BufferType.EDITABLE)
    }

    fun updatePages(current: Int, max: Int) {
        currentPage = current
        targetPage = currentPage
        maxPage = max
        findViewById<ConstraintLayout>(R.id.pageCountContainer).let {
            findViewById<TextView>(R.id.currentPage).text = currentPage.toString()
            findViewById<TextView>(R.id.maxPage).text = maxPage.toString()
        }
        updateInput()
    }
}
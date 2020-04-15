package com.laotoua.dawnislandk.components

import android.content.Context
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import com.laotoua.dawnislandk.R
import com.lxj.xpopup.core.CenterPopupView

class JumpPopup(private val caller: Fragment, context: Context) : CenterPopupView(context) {
    var currentPage = 1
    var maxPage = 1
    var submit = false
    var targetPage = 1

    private val pageInput: EditText by lazy { findViewById<EditText>(R.id.pageInput) }
    private val submitButton: Button by lazy { findViewById<Button>(R.id.submit) }

    override fun getImplLayoutId(): Int {
        return R.layout.jump_popup
    }

    override fun onCreate() {
        super.onCreate()

        findViewById<EditText>(R.id.pageInput).doOnTextChanged { text, start, count, after ->
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
            dismiss()
        }
    }

    private fun updateInput() {
        pageInput.setText(targetPage.toString(), TextView.BufferType.EDITABLE);
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
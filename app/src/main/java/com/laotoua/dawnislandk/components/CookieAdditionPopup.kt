package com.laotoua.dawnislandk.components

import android.content.Context
import android.widget.Button
import android.widget.EditText
import androidx.core.widget.doOnTextChanged
import com.laotoua.dawnislandk.R
import com.lxj.xpopup.core.CenterPopupView

class CookieAdditionPopup(context: Context) : CenterPopupView(context) {

    var cookieName = ""
    var cookieHash = ""

    private val submitButton: Button by lazy { findViewById<Button>(R.id.submit) }

    override fun getImplLayoutId(): Int {
        return R.layout.cookie_addition_popup
    }

    override fun onCreate() {
        super.onCreate()
        submitButton.setOnClickListener {
            cookieName = findViewById<EditText>(R.id.cookieNameText).text.toString()
            cookieHash = findViewById<EditText>(R.id.cookieHashText).text.toString()
            dismiss()
        }

        findViewById<Button>(R.id.cancel).setOnClickListener { dismiss() }

        findViewById<EditText>(R.id.cookieHashText).doOnTextChanged { text, _, _, _ ->
            submitButton.isEnabled = !text.isNullOrBlank()
        }
    }

    fun clearEntries() {
        findViewById<EditText>(R.id.cookieNameText).text.clear()
        findViewById<EditText>(R.id.cookieHashText).text.clear()
    }
}
package com.laotoua.dawnislandk.ui.popup

import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.ImageButton
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import com.laotoua.dawnislandk.R
import com.laotoua.dawnislandk.data.entity.Cookie
import com.laotoua.dawnislandk.ui.activity.QRCookieActivity
import com.lxj.xpopup.core.CenterPopupView
import kotlinx.android.synthetic.main.list_item_cookie.view.*


class CookieManagerPopup(private val caller: Fragment, context: Context) :
    CenterPopupView(context) {

    val cookies = mutableListOf<Cookie>()

    private val cookiesView: MutableList<ConstraintLayout> = mutableListOf()

    private val addCookie: ImageButton by lazy { findViewById<ImageButton>(R.id.addCookie) }
    private val cookieAdditionPopup: CookieAdditionPopup by lazy { CookieAdditionPopup(context) }

    override fun getImplLayoutId(): Int {
        return R.layout.popup_cookie_manager
    }

    fun setCookies(cookies: List<Cookie>) {
        this.cookies.clear()
        this.cookies.addAll(cookies)
    }

    override fun onCreate() {
        super.onCreate()

        addCookie.setOnClickListener {
            caller.startActivity(Intent(caller.requireActivity(), QRCookieActivity::class.java))
//            XPopup.Builder(context)
//                .setPopupCallback(object : SimpleCallback() {
//                    override fun beforeShow() {
//                        cookieAdditionPopup.clearEntries()
//                        super.beforeShow()
//                    }
//                })
//                .asCustom(cookieAdditionPopup)
//                .show()
//                .dismissWith {
//                    if (cookieAdditionPopup.cookieHash != "") {
//                        cookies.add(
//                            Cookie(
//                                cookieAdditionPopup.cookieHash,
//                                cookieAdditionPopup.cookieName
//                            )
//                        )
//                        updateCookiesView()
//                    }
//                }
        }

        cookiesView.addAll(
            listOf(
                findViewById(R.id.cookie1),
                findViewById(R.id.cookie2),
                findViewById(R.id.cookie3),
                findViewById(R.id.cookie4),
                findViewById(R.id.cookie5)
            )
        )

        for (i in 0 until cookiesView.size) {
            cookiesView[i].remove.setOnClickListener {
                cookies.removeAt(i)
                updateCookiesView()
            }
        }

        updateCookiesView()
    }

    private fun updateCookiesView() {
        for (i in 0 until cookiesView.size) {
            if (i >= cookies.size) {
                cookiesView[i].visibility = View.GONE
            } else {
                cookiesView[i].visibility = View.VISIBLE
                cookiesView[i].cookieName.text = cookies[i].cookieName
            }
        }
        if (cookies.size >= 5) {
            addCookie.visibility = View.INVISIBLE
        } else {
            addCookie.visibility = View.VISIBLE
        }
    }

}
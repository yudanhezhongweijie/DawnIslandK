package com.laotoua.dawnislandk.components

import android.content.Context
import android.view.View
import android.widget.ImageButton
import androidx.constraintlayout.widget.ConstraintLayout
import com.laotoua.dawnislandk.R
import com.lxj.xpopup.XPopup
import com.lxj.xpopup.core.CenterPopupView
import kotlinx.android.synthetic.main.cookie_list_item.view.*
import timber.log.Timber


class CookieManagerPopup(context: Context) : CenterPopupView(context) {

    private val cookies: MutableList<String> = mutableListOf()
    private val cookiesView: MutableList<ConstraintLayout> = mutableListOf()
    private val addCookie: ImageButton by lazy { findViewById<ImageButton>(R.id.addCookie) }

    override fun getImplLayoutId(): Int {
        return R.layout.cookie_manager_popup
    }

    override fun onCreate() {
        super.onCreate()

        //TODO match cookie from DB
        cookies.addAll(listOf("C1", "C2", "C3"))

        cookiesView.addAll(
            listOf(
                findViewById(R.id.cookie1),
                findViewById(R.id.cookie2),
                findViewById(R.id.cookie3),
                findViewById(R.id.cookie4),
                findViewById(R.id.cookie5)
            )
        )
        for (i in 0 until 5) {
            if (i >= cookies.size) {
                cookiesView[i].visibility = View.GONE
                continue
            }

            cookiesView[i].cookieName.text = cookies[i]
            cookiesView[i].remove.setOnClickListener {
                //TODO
                Timber.i("need implement remove cookie ")
            }
        }

        addCookie.setOnClickListener {
            XPopup.Builder(context).asInputConfirm(
                "添加饼干", "请输入内容"
            ) {//TODO
                Timber.i("need implement add cookie ")
            }
                .show()
        }

    }

}
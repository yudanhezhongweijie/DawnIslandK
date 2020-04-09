package com.laotoua.dawnislandk.components

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.invoke
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat.requestPermissions
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.laotoua.dawnislandk.R
import com.laotoua.dawnislandk.entities.Cookie
import com.laotoua.dawnislandk.util.AppState
import com.lxj.xpopup.XPopup
import com.lxj.xpopup.core.BasePopupView
import com.lxj.xpopup.core.BottomPopupView
import com.lxj.xpopup.util.XPopupUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber


@SuppressLint("ViewConstructor")
class CreatePopup(private val caller: Fragment, context: Context) :
    BottomPopupView(context) {

    private val PERMISSIONS_STORAGE = arrayOf(
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE
    )

    private val EXTERNAL_STORAGE = 1

    private val getImage = caller.prepareCall(ActivityResultContracts.GetContent())
    { uri: Uri? ->
        // TODO Handle the returned Uri
        Timber.i("selected uri: $uri")
    }

    private var cookies = listOf<Cookie>()

    private var selectedCookie: String? = null

    override fun show(): BasePopupView {
        caller.lifecycleScope.launch {
            loadCookies()
        }
        return super.show()
    }

    private suspend fun loadCookies() {
        withContext(Dispatchers.IO) {
            AppState.loadCookies()
            cookies = AppState.cookies!!
        }
        if (selectedCookie == null || cookies.isNullOrEmpty()) {
            findViewById<TextView>(R.id.cookie)?.let {
                it.text = if (cookies.isNullOrEmpty()) {
                    "没有饼干"
                } else {
                    cookies[0].userHash
                }
            }
        }
    }

    private fun checkPermission(context: Context) {
        if (ContextCompat.checkSelfPermission(
                context, Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                caller.requireActivity(),
                PERMISSIONS_STORAGE,
                EXTERNAL_STORAGE
            )
        }
    }

    override fun getImplLayoutId(): Int {
        return R.layout.create_dialog
    }

    override fun getMaxHeight(): Int {
        return (XPopupUtils.getWindowHeight(context) * .7f).toInt()
    }

    override fun onCreate() {
        super.onCreate()

        findViewById<ImageButton>(R.id.send).setOnClickListener {
            // TODO
            Timber.i("Clicked on send")
        }

        findViewById<ImageButton>(R.id.attachImage).setOnClickListener {
            Timber.i("Clicked on attachImage")
            checkPermission(context)

            getImage("image/*")
        }

        findViewById<ImageButton>(R.id.doodle).setOnClickListener {
            // TODO
            Toast.makeText(caller.context, "还没做。。。", Toast.LENGTH_SHORT).show()
        }

        findViewById<ImageButton>(R.id.expandMore).setOnClickListener {
            findViewById<LinearLayout>(R.id.expansion).let {
                if (it.visibility == View.VISIBLE) {
                    it.visibility = View.GONE
                } else {
                    it.visibility = View.VISIBLE
                }
            }
        }

        findViewById<TextView>(R.id.cookie).setOnClickListener {

            if (!cookies.isNullOrEmpty()) {
                XPopup.Builder(context)
                    .atView(it) // 依附于所点击的View，内部会自动判断在上方或者下方显示
                    .asAttachList(
                        cookies.map { c -> c.userHash }.toTypedArray(),
                        intArrayOf(
                            R.mipmap.ic_launcher, R.mipmap.ic_launcher, R.mipmap.ic_launcher,
                            R.mipmap.ic_launcher, R.mipmap.ic_launcher
                        )
                    ) { _, text ->
                        selectedCookie = text
                        findViewById<TextView>(R.id.cookie).text = selectedCookie
                    }
                    .show()
            } else {
                Toast.makeText(caller.context, "没有饼干", Toast.LENGTH_SHORT).show()
            }
        }
    }

}
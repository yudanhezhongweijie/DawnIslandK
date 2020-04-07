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
import androidx.activity.invoke
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat.requestPermissions
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.laotoua.dawnislandk.R
import com.lxj.xpopup.core.BottomPopupView
import com.lxj.xpopup.util.XPopupUtils
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
            Timber.i("Clicked on doodle")
        }

        findViewById<ImageButton>(R.id.expandMore).setOnClickListener {
            Timber.i("Clicked on expandMore")

            findViewById<LinearLayout>(R.id.expansion).let {
                if (it.visibility == View.VISIBLE) {
                    it.visibility = View.GONE
                } else {
                    it.visibility = View.VISIBLE
                }
            }
        }

        findViewById<TextView>(R.id.cookie).setOnClickListener {
            Timber.i("Clicked on cookie")
            // TODO
        }
    }
}
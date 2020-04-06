package com.laotoua.dawnislandk.components


import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.invoke
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.laotoua.dawnislandk.R
import com.lxj.xpopup.XPopup
import com.lxj.xpopup.core.BasePopupView
import com.lxj.xpopup.core.BottomPopupView
import com.lxj.xpopup.util.XPopupUtils
import timber.log.Timber


class SendDialog : Fragment() {

    private val PERMISSIONS_STORAGE = arrayOf(
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE
    )

    private val dialog: BasePopupView by lazy { CustomPopup(this, requireContext()) }

    private val EXTERNAL_STORAGE = 1

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) {
            XPopup.Builder(context)
                .asCustom(dialog)
                .show()
        }
    }

    override fun onResume() {
        super.onResume()
        XPopup.setAnimationDuration(200)
        XPopup.Builder(context)
            .asCustom(dialog)
            .show()

    }


    private val getImage = prepareCall(ActivityResultContracts.GetContent())
    { uri: Uri? ->
        // TODO Handle the returned Uri
        Timber.i("selected uri: $uri")
    }

    private fun checkPermission(context: Context, caller: Fragment) {
        if (ContextCompat.checkSelfPermission(
                context, Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                PERMISSIONS_STORAGE,
                EXTERNAL_STORAGE
            )
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        Timber.i("send dialog destroyed")
    }

    inner class CustomPopup(private val caller: Fragment, context: Context) :
        BottomPopupView(context) {

        override fun getImplLayoutId(): Int {
            return R.layout.send_dialog
        }

        override fun getMaxHeight(): Int {
            return (XPopupUtils.getWindowHeight(context) * .6f).toInt()
        }

        override fun onCreate() {
            super.onCreate()

            findViewById<ImageButton>(R.id.send).setOnClickListener {
                Timber.i("Clicked on send")
            }

            findViewById<ImageButton>(R.id.attachImage).setOnClickListener {
                Timber.i("Clicked on attachImage")
                checkPermission(context, caller)

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
            }
        }

        override fun onDismiss() {
            super.onDismiss()

            (caller.requireActivity()
                .getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                .hideSoftInputFromWindow(windowToken, 0)

            caller.parentFragmentManager.beginTransaction().hide(caller).commit()
        }
    }
}
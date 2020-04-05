package com.laotoua.dawnislandk

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.invoke
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat.requestPermissions
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import com.afollestad.materialdialogs.LayoutMode
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.bottomsheets.BottomSheet
import com.afollestad.materialdialogs.callbacks.onDismiss
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.customview.getCustomView
import com.laotoua.dawnislandk.SendDialog.prepareCall
import timber.log.Timber


object SendDialog : DialogFragment() {

    private val PERMISSIONS_STORAGE = arrayOf(
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE
    )

    private var dialog: MaterialDialog? = null
    private const val REQUEST_EXTERNAL_STORAGE = 1

    private val getImage = prepareCall(ActivityResultContracts.GetContent())
    { uri: Uri? ->
        // Handle the returned Uri
        Timber.i("selected uri: $uri")
    }


    private fun display(titleText: String?) {
        val caller = this
        if (dialog == null) {
            dialog = MaterialDialog(requireContext(), BottomSheet(LayoutMode.WRAP_CONTENT))
//            .title(text = titleText)
                .customView(R.layout.send_dialog, scrollable = true)
                .cornerRadius(16f)
                .onDismiss {
                    // TODO: save draft
                    parentFragmentManager.beginTransaction().remove(caller).commit()
//                    parentFragmentManager.beginTransaction().hide(caller).commit()
//                    parentFragmentManager.popBackStack()
                    Timber.i("hiding dialog")
                }
                .show {

                    this.view.titleLayout.clipToPadding = true
                    val view = getCustomView()
                    view.findViewById<ImageButton>(R.id.send).setOnClickListener {
                        Timber.i("Clicked on send")
                    }

                    view.findViewById<ImageButton>(R.id.attachImage).setOnClickListener {
                        Timber.i("Clicked on attachImage")
                        checkPermission(context, caller)

                        getImage("image/*")
                        dialog!!.show()
                        Timber.i("showed???")
                    }

                    view.findViewById<ImageButton>(R.id.doodle).setOnClickListener {
                        Timber.i("Clicked on doodle")
                    }

                    view.findViewById<ImageButton>(R.id.expandMore).setOnClickListener {
                        Timber.i("Clicked on expandMore")
                    }

                    view.findViewById<TextView>(R.id.cookie).setOnClickListener {
                        Timber.i("Clicked on cookie")
                    }


                }
        }


    }

    override fun onResume() {
        super.onResume()
        display(null)
    }

    private fun checkPermission(context: Context, caller: Fragment) {
        if (ContextCompat.checkSelfPermission(
                context, Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                requireActivity(),
                PERMISSIONS_STORAGE,
                REQUEST_EXTERNAL_STORAGE
            )
        }
    }
//
//    private fun openAlbum() {
//        val intent = Intent(Intent.ACTION_GET_CONTENT)
//        intent.type = "image/*"
//        // 打开相册
//        startActivityForResult(intent, 2)
//    }

    override fun onDestroy() {
        super.onDestroy()
        dialog = null
        Timber.i("send dialog destroyed")
    }

}
package com.laotoua.dawnislandk.io

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.invoke
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.CallSuper
import androidx.fragment.app.Fragment
import com.king.zxing.Intents
import com.laotoua.dawnislandk.R
import com.laotoua.dawnislandk.ui.activity.DoodleActivity
import com.laotoua.dawnislandk.ui.activity.QRCookieActivity
import timber.log.Timber

object FragmentIntentUtil {

    private fun requestPermission(caller: Fragment, permission: String) {
        caller.registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            Timber.i("requesting $permission permission...")
        }(permission)
    }

    fun checkReadStoragePermission(caller: Fragment): Boolean {
        if (caller.requireContext()
                .checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(
                caller.requireContext(),
                R.string.need_read_storage_permission,
                Toast.LENGTH_SHORT
            ).show()
            requestPermission(
                caller,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            return false
        }
        return true
    }

    fun checkWriteStoragePermission(caller: Fragment): Boolean {
        if (caller.requireContext()
                .checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(
                caller.requireContext(),
                R.string.need_write_storage_permission,
                Toast.LENGTH_SHORT
            ).show()
            requestPermission(
                caller,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            return false
        }
        return true
    }

    private fun checkTakePicturePermission(caller: Fragment): Boolean {
        if (caller.requireContext()
                .checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(
                caller.requireContext(),
                R.string.need_take_picture_permission,
                Toast.LENGTH_SHORT
            ).show()
            requestPermission(
                caller,
                Manifest.permission.CAMERA
            )
            return false
        }
        return true
    }

    fun checkPermissions(caller: Fragment): Boolean {
        var result = checkTakePicturePermission(caller)
        result = checkReadStoragePermission(caller) && result
        result = checkWriteStoragePermission(caller) && result
        return result
    }

    fun getImageFromGallery(caller: Fragment, type: String, callback: (Uri?) -> Unit) {
        caller.registerForActivityResult(ActivityResultContracts.GetContent(), callback)(type)
    }

    fun getImageFromCamera(caller: Fragment, uri: Uri, callback: (Boolean) -> Unit) {
        caller.registerForActivityResult(MyTakePicture(), callback)(uri)
    }

    // temp workaround for Default TakePicture, which might not return thumbnail upon success
    internal class MyTakePicture :
        ActivityResultContract<Uri, Boolean>() {
        @CallSuper
        override fun createIntent(
            context: Context,
            input: Uri
        ): Intent {
            return Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                .putExtra(MediaStore.EXTRA_OUTPUT, input)
        }

        override fun parseResult(
            resultCode: Int,
            intent: Intent?
        ): Boolean {
            return (intent == null || resultCode != Activity.RESULT_OK).not()
        }
    }

    fun drawNewDoodle(caller: Fragment, callback: (Uri?) -> Unit) {
        caller.registerForActivityResult(MakeDoodle(), callback)(caller)
    }

    internal class MakeDoodle :
        ActivityResultContract<Fragment, Uri?>() {
        @CallSuper
        override fun createIntent(
            context: Context,
            input: Fragment
        ): Intent {
            return Intent(input.requireActivity(), DoodleActivity::class.java)
        }

        override fun parseResult(
            resultCode: Int,
            intent: Intent?
        ): Uri? {
            return if (intent == null || resultCode != Activity.RESULT_OK) null else intent.data
        }
    }

    fun getCookieFromQRCode(caller: Fragment, callback: (String?) -> Unit) {
        caller.registerForActivityResult(ScanQRCode(), callback)(caller)
    }

    internal class ScanQRCode :
        ActivityResultContract<Fragment, String?>() {
        @CallSuper
        override fun createIntent(
            context: Context,
            input: Fragment
        ): Intent {
            return Intent(input.requireActivity(), QRCookieActivity::class.java)
        }

        override fun parseResult(
            resultCode: Int,
            intent: Intent?
        ): String? {
            return if (intent == null || resultCode != Activity.RESULT_OK) null else intent.getStringExtra(
                Intents.Scan.RESULT
            )?.split(":")?.last()?.dropLast(1)?.removeSurrounding("\"", "\"")
        }
    }

}
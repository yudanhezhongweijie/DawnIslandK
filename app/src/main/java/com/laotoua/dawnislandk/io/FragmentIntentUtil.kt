package com.laotoua.dawnislandk.io

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.invoke
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import timber.log.Timber

object FragmentIntentUtil {

    private fun requestPermission(caller: Fragment, permission: String) {
        caller.prepareCall(ActivityResultContracts.RequestPermission()) {
            Timber.i("requesting $permission permission...")
        }(permission)
    }

    fun checkPermissions(caller: Fragment): Boolean {
        if (caller.requireContext()
                .checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(caller.requireContext(), "需要读取外部存储权限", Toast.LENGTH_SHORT).show()
            requestPermission(
                caller,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            return false
        }
        if (caller.requireContext()
                .checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(caller.requireContext(), "需要写入外部存储权限", Toast.LENGTH_SHORT).show()
            requestPermission(
                caller,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            return false
        }
        if (caller.requireContext()
                .checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(caller.requireContext(), "需要照相权限", Toast.LENGTH_SHORT).show()
            requestPermission(
                caller,
                Manifest.permission.CAMERA
            )
            return false
        }
        return true
    }

    fun getImageFromGallery(caller: Fragment, type: String, callback: (Uri?) -> Unit) {
        caller.prepareCall(ActivityResultContracts.GetContent(), callback)(type)
    }

    fun getImageFromCamera(caller: Fragment, uri: Uri, callback: (Bitmap?) -> Unit) {
        caller.prepareCall(ActivityResultContracts.TakePicture(), callback)(uri)
    }

}
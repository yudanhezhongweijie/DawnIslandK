package com.laotoua.dawnislandk.util

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.CallSuper
import androidx.fragment.app.Fragment
import com.king.zxing.Intents
import com.laotoua.dawnislandk.R
import com.laotoua.dawnislandk.screens.tasks.DoodleActivity
import com.laotoua.dawnislandk.screens.tasks.QRCookieActivity
import timber.log.Timber

object FragmentIntentUtil {

    private val allPermissions = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.CAMERA
    )

    private fun requestSinglePermission(caller: Fragment, permission: String) {
        caller.registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            Timber.i("requesting $permission permission...")
        }.launch(permission)
    }

    private fun requestMultiplePermission(caller: Fragment, permissions: Array<String>) {
        caller.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            Timber.i("requesting multiple permissions: $permissions")
        }.launch(permissions)
    }

    fun checkAndRequestSinglePermission(
        caller: Fragment,
        permission: String,
        request: Boolean
    ): Boolean {
        if (caller.requireContext()
                .checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED
        ) {
            val toastMsg = when (permission) {
                Manifest.permission.READ_EXTERNAL_STORAGE -> R.string.need_read_storage_permission
                Manifest.permission.WRITE_EXTERNAL_STORAGE -> R.string.need_write_storage_permission
                Manifest.permission.CAMERA -> R.string.need_take_picture_permission
                else -> {
                    throw Exception("Missing handler in checkAndRequestSinglePermission")
                }
            }
            Toast.makeText(
                caller.requireContext(),
                toastMsg,
                Toast.LENGTH_SHORT
            ).show()
            if (request) {
                requestSinglePermission(
                    caller,
                    permission
                )
            }
            return false
        }
        return true
    }

    fun checkAndRequestAllPermissions(
        caller: Fragment,
        permissions: Array<String>? = allPermissions
    ): Boolean {
        val missingPermissions = permissions!!.filterNot {
            checkAndRequestSinglePermission(
                caller,
                it,
                false
            )
        }.toTypedArray()

        requestMultiplePermission(
            caller,
            missingPermissions
        )
        return missingPermissions.isEmpty()
    }

    fun getImageFromGallery(caller: Fragment, type: String, callback: (Uri?) -> Unit) {
        caller.registerForActivityResult(ActivityResultContracts.GetContent(), callback)
            .launch(type)
    }

    fun getImageFromCamera(caller: Fragment, uri: Uri, callback: (Boolean) -> Unit) {
        caller.registerForActivityResult(ActivityResultContracts.TakePicture(), callback)
            .launch(uri)
    }

    fun drawNewDoodle(caller: Fragment, callback: (Uri?) -> Unit) {
        caller.registerForActivityResult(MakeDoodle(), callback).launch(caller)
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
        caller.registerForActivityResult(ScanQRCode(), callback).launch(caller)
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
            )
        }
    }

}
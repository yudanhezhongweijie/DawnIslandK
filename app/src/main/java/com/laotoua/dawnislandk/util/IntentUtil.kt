/*
 *  Copyright 2020 Fishballzzz
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package com.laotoua.dawnislandk.util

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.CallSuper
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.FragmentActivity
import com.king.zxing.Intents
import com.laotoua.dawnislandk.R
import com.laotoua.dawnislandk.screens.tasks.DoodleActivity
import com.laotoua.dawnislandk.screens.tasks.QRCookieActivity
import com.laotoua.dawnislandk.screens.tasks.ToolbarBackgroundCropActivity
import com.laotoua.dawnislandk.screens.util.ToolBar
import timber.log.Timber
import java.io.File

object IntentUtil {

    private val allPermissions = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.CAMERA
    )

    private fun informUserRequiredPermission(context: Context, permission: String) {
        val toastMsg = when (permission) {
            Manifest.permission.READ_EXTERNAL_STORAGE -> R.string.need_read_storage_permission
            Manifest.permission.WRITE_EXTERNAL_STORAGE -> R.string.need_write_storage_permission
            Manifest.permission.CAMERA -> R.string.need_take_picture_permission
            else -> {
                throw Exception("Missing handler in checkAndRequestSinglePermission")
            }
        }
        Toast.makeText(context, toastMsg, Toast.LENGTH_SHORT).show()
    }

    private fun requestSinglePermission(caller: FragmentActivity, permission: String) {
        caller.registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            Timber.i("requesting $permission permission...")
            if (it == false) {
                informUserRequiredPermission(caller, permission)
            } else {
                Toast.makeText(
                    caller,
                    R.string.please_try_again,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }.launch(permission)
    }

    private fun requestMultiplePermission(caller: FragmentActivity, permissions: Array<String>) {
        caller.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            Timber.i("requesting multiple permissions: $permissions")
            val missingPermissions = result.filter { it.value == false }.keys.toList()
            if (missingPermissions.isNotEmpty()) {
                missingPermissions.map {
                    informUserRequiredPermission(caller, it)
                }
            } else {
                Toast.makeText(
                    caller,
                    R.string.please_try_again,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }.launch(permissions)
    }

    fun checkAndRequestSinglePermission(
        caller: FragmentActivity,
        permission: String,
        request: Boolean
    ): Boolean {
        if (caller.checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
            if (request) requestSinglePermission(caller, permission)
            return false
        }
        return true
    }

    fun checkAndRequestAllPermissions(
        caller: FragmentActivity,
        permissions: Array<String> = allPermissions
    ): Boolean {
        val missingPermissions = permissions.filterNot {
            checkAndRequestSinglePermission(caller, it, false)
        }.toTypedArray()
        if (missingPermissions.isNotEmpty()) requestMultiplePermission(caller, missingPermissions)
        return missingPermissions.isEmpty()
    }

    fun getImageFromGallery(caller: FragmentActivity, type: String, callback: (Uri?) -> Unit) {
        caller.registerForActivityResult(ActivityResultContracts.GetContent(), callback)
            .launch(type)
    }

    fun getImageFromCamera(caller: FragmentActivity, callback: (Uri) -> Unit) {
        val timeStamp: String = ReadableTime.getCurrentTimeFileName()
        val relativeLocation = Environment.DIRECTORY_PICTURES + File.separator + "Dawn"
        val fileName = "DawnIsland_$timeStamp.jpg"
        try {
            val uri = ImageUtil.addPlaceholderImageToGallery(caller, fileName, relativeLocation)
            caller.registerForActivityResult(ActivityResultContracts.TakePicture()) {
                if (it == true) {
                    callback.invoke(uri)
                } else {
                    ImageUtil.removePlaceholderImageInGallery(caller, uri)
                }
            }.launch(uri)
        } catch (e: Exception) {
            Timber.e(e)
            Toast.makeText(caller, R.string.something_went_wrong, Toast.LENGTH_SHORT).show()
        }
    }

    fun drawNewDoodle(caller: FragmentActivity, callback: (Uri?) -> Unit) {
        caller.registerForActivityResult(MakeDoodle(), callback).launch(caller)
    }

    internal class MakeDoodle : ActivityResultContract<FragmentActivity, Uri?>() {
        @CallSuper
        override fun createIntent(context: Context, input: FragmentActivity): Intent {
            return Intent(input, DoodleActivity::class.java)
        }

        override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
            return if (intent == null || resultCode != Activity.RESULT_OK) null else intent.data
        }
    }

    fun getCookieFromQRCode(caller: FragmentActivity, callback: (String?) -> Unit) {
        caller.registerForActivityResult(ScanQRCode(), callback).launch(caller)
    }

    fun setToolbarBackgroundImage(caller: FragmentActivity, callback: (Uri?) -> Unit) {
        caller.registerForActivityResult(CropToolbarImage(), callback).launch(caller)
    }

    internal class ScanQRCode : ActivityResultContract<FragmentActivity, String?>() {
        @CallSuper
        override fun createIntent(context: Context, input: FragmentActivity): Intent {
            return Intent(input, QRCookieActivity::class.java)
        }

        override fun parseResult(resultCode: Int, intent: Intent?): String? {
            return if (intent == null || resultCode != Activity.RESULT_OK) null else intent.getStringExtra(
                Intents.Scan.RESULT
            )
        }
    }

    internal class CropToolbarImage : ActivityResultContract<FragmentActivity, Uri?>() {
        override fun createIntent(context: Context, input: FragmentActivity): Intent {
            val intent = Intent(input, ToolbarBackgroundCropActivity::class.java)
            val width = input.findViewById<Toolbar>(R.id.toolbar).measuredWidth + 100
            val height = input.findViewById<Toolbar>(R.id.toolbar).measuredHeight + ToolBar.getStatusBarHeight() + 200
            intent.putExtra("w", width.toFloat())
            intent.putExtra("h", height.toFloat())
            return intent
        }

        override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
            return if (intent == null || resultCode != Activity.RESULT_OK) null else intent.data
        }

    }
}
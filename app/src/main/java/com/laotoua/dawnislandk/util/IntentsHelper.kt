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
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.CallSuper
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.laotoua.dawnislandk.DawnApp
import com.laotoua.dawnislandk.R
import com.laotoua.dawnislandk.screens.MainActivity
import com.laotoua.dawnislandk.screens.profile.ProfileFragment
import com.laotoua.dawnislandk.screens.tasks.DoodleActivity
import com.laotoua.dawnislandk.screens.tasks.QRCookieActivity
import com.laotoua.dawnislandk.screens.tasks.ToolbarBackgroundCropActivity
import com.laotoua.dawnislandk.screens.util.ToolBar
import com.laotoua.dawnislandk.screens.widgets.popups.PostPopup
import timber.log.Timber
import java.io.File

class IntentsHelper(private val registry: ActivityResultRegistry, private val mainActivity: MainActivity) :
    DefaultLifecycleObserver {
    private lateinit var requestSinglePermission: ActivityResultLauncher<String>
    private lateinit var requestMultiplePermissions: ActivityResultLauncher<Array<String>>
    private lateinit var getImageFromGallery: ActivityResultLauncher<String>
    private lateinit var takePicture: ActivityResultLauncher<Uri>
    private lateinit var drawNewDoodle: ActivityResultLauncher<FragmentActivity>
    private lateinit var getCookieFromQRCode: ActivityResultLauncher<FragmentActivity>
    private lateinit var cropToolbarImage: ActivityResultLauncher<FragmentActivity>

    private var postPopup: PostPopup? = null
    private var profileFragment: ProfileFragment? = null
    private var placeHolderUri: Uri? = null

    private val allPermissions = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.CAMERA
    )

    private fun toast(stringId: Int) {
        Toast.makeText(mainActivity, stringId, Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(owner: LifecycleOwner) {
        requestSinglePermission =
            registry.register(REQUEST_SINGLE_PERM, owner, ActivityResultContracts.RequestPermission()) {
                if (it) {
                    toast(R.string.please_try_again)
                } else {
                    toast(R.string.please_give_permission_and_try_again)
                }
            }

        requestMultiplePermissions = registry.register(
            REQUEST_MULTIPLE_PERMS,
            owner,
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            if (permissions.filterValues { !it }.isEmpty()) {
                toast(R.string.please_try_again)
            } else {
                toast(R.string.please_give_permission_and_try_again)
            }
        }

        getImageFromGallery =
            registry.register(GET_IMAGE_FROM_GALLERY, owner, ActivityResultContracts.GetContent()) { uri ->
                // Handle the returned Uri
                postPopup?.compressAndPreviewImage(uri)
                postPopup = null
            }

        takePicture = registry.register(TAKE_PICTURE, owner, ActivityResultContracts.TakePicture()) {
            if (it == true) {
                // Handle the returned Uri
                postPopup?.compressAndPreviewImage(placeHolderUri)
                postPopup = null
            } else {
                placeHolderUri?.let { uri -> ImageUtil.removePlaceholderImageInGallery(mainActivity, uri) }
            }
        }

        drawNewDoodle = registry.register(MAKE_DOODLE, owner, MakeDoodle()) { uri ->
            Timber.d("Made a doodle. Prepare to upload...")
            postPopup?.compressAndPreviewImage(uri)
            postPopup = null
        }

        getCookieFromQRCode = registry.register(GET_COOKIE_FROM_QR_CODE, owner, ScanQRCode()) { cookie ->
            cookie?.run {
                profileFragment?.saveCookieWithInputName(this)
            }
            profileFragment = null
        }

        cropToolbarImage = registry.register(CROP_TOOLBAR_IMAGE, owner, CropToolbarImage()) { uri ->
            if (uri != null) {
                DawnApp.applicationDataStore.setCustomToolbarImagePath(uri.toString())
                toast(R.string.restart_to_apply_setting)
            } else {
                toast(R.string.cannot_load_image_file)
            }
        }

    }

    fun checkAndRequestSinglePermission(
        caller: FragmentActivity,
        permission: String,
        request: Boolean
    ): Boolean {
        if (caller.checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
            if (request) {
                Timber.i("requesting $permission permission...")
                requestSinglePermission.launch(permission)
            }
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
        if (missingPermissions.isNotEmpty()) {
            requestMultiplePermissions.launch(missingPermissions)
        }
        return missingPermissions.isEmpty()
    }

    fun getImageFromGallery(popup: PostPopup) {
        postPopup = popup
        getImageFromGallery.launch("image/*")
    }

    fun getImageFromCamera(caller: FragmentActivity, popup: PostPopup) {
        val timeStamp: String = ReadableTime.getCurrentTimeFileName()
        val relativeLocation = Environment.DIRECTORY_PICTURES + File.separator + "Dawn"
        val fileName = "DawnIsland_$timeStamp.jpg"
        try {
            placeHolderUri = ImageUtil.addPlaceholderImageToGallery(caller, fileName, relativeLocation)
            postPopup = popup
            takePicture.launch(placeHolderUri)
        } catch (e: Exception) {
            Timber.e(e)
            toast(R.string.something_went_wrong)
        }
    }

    fun drawNewDoodle(caller: FragmentActivity, popup: PostPopup) {
        postPopup = popup
        drawNewDoodle.launch(caller)
    }

    fun getCookieFromQRCode(caller: ProfileFragment) {
        profileFragment = caller
        getCookieFromQRCode.launch(caller.requireActivity())
    }

    fun setToolbarBackgroundImage(caller: FragmentActivity) {
        cropToolbarImage.launch(caller)
    }

    private class MakeDoodle : ActivityResultContract<FragmentActivity, Uri?>() {
        @CallSuper
        override fun createIntent(context: Context, input: FragmentActivity): Intent {
            return Intent(input, DoodleActivity::class.java)
        }

        override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
            return if (intent == null || resultCode != Activity.RESULT_OK) null else intent.data
        }
    }


    private class ScanQRCode : ActivityResultContract<FragmentActivity, String?>() {
        @CallSuper
        override fun createIntent(context: Context, input: FragmentActivity): Intent {
            return Intent(input, QRCookieActivity::class.java)
        }

        override fun parseResult(resultCode: Int, intent: Intent?): String? {
            return if (intent == null || resultCode != Activity.RESULT_OK) null else intent.getStringExtra(
                CAMERA_SCAN_RESULT
            )
        }
    }

    private class CropToolbarImage : ActivityResultContract<FragmentActivity, Uri?>() {
        override fun createIntent(context: Context, input: FragmentActivity): Intent {
            val intent = Intent(input, ToolbarBackgroundCropActivity::class.java)
            val toolbar = input.findViewById<Toolbar>(R.id.toolbar)
            val width = toolbar.measuredWidth + 100
            val height = toolbar.measuredHeight + ToolBar.getStatusBarHeight() + 200
            intent.putExtra("w", width.toFloat())
            intent.putExtra("h", height.toFloat())
            return intent
        }

        override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
            return if (intent == null || resultCode != Activity.RESULT_OK) null else intent.data
        }

    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        postPopup = null
    }

    companion object {

        const val REQUEST_SINGLE_PERM = "request_single_perm"
        const val REQUEST_MULTIPLE_PERMS = "request_multiple_perms"
        const val CROP_TOOLBAR_IMAGE = "crop_toolbar_image"
        const val GET_IMAGE_FROM_GALLERY = "get_image_from_gallery"
        const val TAKE_PICTURE = "take_picture"
        const val MAKE_DOODLE = "make_doodle"
        const val GET_COOKIE_FROM_QR_CODE = "get_cookie_from_qr_code"
        const val CAMERA_SCAN_RESULT = "camera_scan_result"
    }
}

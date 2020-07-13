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

package com.laotoua.dawnislandk.screens.widgets.popups

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Environment
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.laotoua.dawnislandk.R
import com.laotoua.dawnislandk.util.ImageUtil
import com.laotoua.dawnislandk.util.ReadableTime
import com.laotoua.dawnislandk.util.SingleLiveEvent
import com.laotoua.dawnislandk.util.lazyOnMainOnly
import com.lxj.xpopup.core.ImageViewerPopupView
import com.lxj.xpopup.photoview.PhotoView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File


@SuppressLint("ViewConstructor")
class ImageViewerPopup(
    private val imgUrl: String,
    context: Context
) : ImageViewerPopupView(context) {

    private val caller = context as FragmentActivity
    private val toastMsg = MutableLiveData<SingleLiveEvent<Int>>()
    private var saveShown = true
    private val saveButton by lazyOnMainOnly { findViewById<FloatingActionButton>(R.id.save) }

    override fun getImplLayoutId(): Int = R.layout.popup_image_viewer

    override fun onCreate() {
        super.onCreate()
        setXPopupImageLoader(ImageLoader())
    }

    override fun initPopupContent() {
        super.initPopupContent()
        pager.adapter = PopupPVA()
    }

    private val toastObs = Observer<SingleLiveEvent<Int>> {event->
        event.getContentIfNotHandled()?.let {
            Toast.makeText(caller, it, Toast.LENGTH_SHORT).show()
        }
    }
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        this.isShowSaveBtn = false
        saveButton.setOnClickListener { addPicToGallery(context, imgUrl) }
        toastMsg.observe(caller, toastObs)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        toastMsg.removeObserver(toastObs)
    }

    private fun checkAndRequestExternalStoragePermission(caller: FragmentActivity): Boolean {
        if (caller.checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            caller.registerForActivityResult(ActivityResultContracts.RequestPermission()) {
                if (it == false) {
                    Toast.makeText(
                        context,
                        R.string.need_write_storage_permission,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
            return false
        }
        return true
    }

    private fun addPicToGallery(context: Context, imgUrl: String) {
        caller.lifecycleScope.launch(Dispatchers.IO) {
            if (!checkAndRequestExternalStoragePermission(caller)) {
                return@launch
            }

            Timber.i("Saving image $imgUrl to Gallery... ")
            val relativeLocation = Environment.DIRECTORY_PICTURES + File.separator + "Dawn"
            var fileName = imgUrl.substringAfter("/")
            val fileExist = ImageUtil.isImageInGallery(caller, fileName)
            if (fileExist) {
                // Inform user and renamed file when the filename is already taken
                val name = fileName.substringBeforeLast(".")
                val ext = fileName.substringAfterLast(".")
                fileName = "${name}_${ReadableTime.getCurrentTimeFileName()}.$ext"
            }
            val saved = ImageUtil.copyImageFileToGallery(
                caller,
                fileName,
                relativeLocation,
                imageLoader.getImageFile(context, imgUrl)
            )
            if (fileExist && saved) toastMsg.postValue(SingleLiveEvent.create(R.string.image_already_exists_in_picture))
            else if (saved) toastMsg.postValue(SingleLiveEvent.create(R.string.image_saved))
            else toastMsg.postValue(SingleLiveEvent.create(R.string.something_went_wrong))
        }
    }

    inner class PopupPVA : PhotoViewAdapter() {
        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            val photoView =
                PhotoView(container.context)
            if (imageLoader != null) imageLoader.loadImage(
                position,
                urls[if (isInfinite) position % urls.size else position],
                photoView
            )
            container.addView(photoView)
            photoView.setOnClickListener {
                saveShown = if (saveShown) {
                    saveButton.hide()
                    saveButton.isClickable = false
                    false
                } else {
                    saveButton.isClickable = true
                    saveButton.show()
                    true
                }

            }
            return photoView
        }
    }
}
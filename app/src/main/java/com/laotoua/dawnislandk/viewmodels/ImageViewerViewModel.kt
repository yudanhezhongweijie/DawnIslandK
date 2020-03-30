package com.laotoua.dawnislandk.viewmodels

import android.content.ContentResolver
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bumptech.glide.Glide
import com.github.chrisbanes.photoview.PhotoView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

class ImageViewerViewModel : ViewModel() {
    private val TAG: String = "ImageViewerViewModel"

    // TODO: move cdn path to right place
    private val cdn = "https://nmbimg.fastmirror.org/image/"

    private val _status = MutableLiveData(false)
    val status: LiveData<Boolean> get() = _status

    /** REQUIRE caller to provide context
     *
     */
    fun loadImage(caller: Fragment, photoView: PhotoView, imgUrl: String) {
        Log.i(TAG, "Downloading image at ${cdn + imgUrl}")
        Glide.with(caller).load(cdn + imgUrl).into(photoView)
    }

    /** REQUIRE caller to provide context
     *
     */
    fun addPicToGallery(caller: Fragment, imgUrl: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                Log.i(TAG, "Saving image to Gallery... ")
                val relativeLocation =
                    Environment.DIRECTORY_PICTURES + File.separator + "Dawn"
                val name =
                    imgUrl.substring(imgUrl.lastIndexOf("/") + 1, imgUrl.lastIndexOf("."))
                val ext = imgUrl.substring(imgUrl.lastIndexOf(".") + 1)
                try {
                    val resolver: ContentResolver =
                        caller.requireActivity().contentResolver
                    val newImageDetails = ContentValues()
                    newImageDetails.put(MediaStore.MediaColumns.DISPLAY_NAME, "$name.$ext")
                    newImageDetails.put(MediaStore.MediaColumns.MIME_TYPE, "image/$ext")
                    newImageDetails.put(MediaStore.MediaColumns.RELATIVE_PATH, relativeLocation)
                    val contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    val image: Bitmap =
                        Glide.with(caller).asBitmap().load(cdn + imgUrl).submit().get()
                    val format = CompressFormat.JPEG
                    val uri = resolver.insert(contentUri, newImageDetails)
                        ?: throw IOException("Failed to create new MediaStore record.")
                    val stream = resolver.openOutputStream(uri)
                        ?: throw IOException("Failed to get output stream.")
                    image.compress(format, 100, stream)
                    Log.i(TAG, "Image saved")
                    _status.postValue(true)
                } catch (e: Exception) {
                    Log.e(TAG, "failed to save img from $imgUrl", e)
                    _status.postValue(false)
                }
            }
        }
    }
}

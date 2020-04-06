package com.laotoua.dawnislandk.util

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target.SIZE_ORIGINAL
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.laotoua.dawnislandk.R
import com.lxj.xpopup.core.ImageViewerPopupView
import com.lxj.xpopup.interfaces.XPopupImageLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.IOException


class ImageViewerPopup(private val caller: Fragment, context: Context, private val imgUrl: String) :
    ImageViewerPopupView(context) {

    private val _status = MutableLiveData<Boolean>()
    val status: LiveData<Boolean> get() = _status

    override fun getImplLayoutId(): Int {
        return R.layout.image_viewer_popup
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        findViewById<FloatingActionButton>(R.id.fabMenu).setOnClickListener {
            Timber.i("fab clicked")

            addPicToGallery(context, imgUrl)
        }


        status.observe(caller, Observer {
            when (it) {
                true -> Toast.makeText(context, "Image saved in Pictures/Dawn", Toast.LENGTH_SHORT)
                    .show()
                else -> Toast.makeText(context, "Error in saving image", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun addPicToGallery(context: Context, imgUrl: String) {
        caller.lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                Timber.i("Saving image to Gallery... ")
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
                    val uri = resolver.insert(contentUri, newImageDetails)
                        ?: throw IOException("Failed to create new MediaStore record.")
                    val stream = resolver.openOutputStream(uri)
                        ?: throw IOException("Failed to get output stream.")
                    val file = imageLoader.getImageFile(context, imgUrl)
                    stream.write(file.readBytes())
                    stream.close()
                    _status.postValue(true)
                } catch (e: Exception) {
                    Timber.e(e, "failed to save img from $imgUrl")
                    _status.postValue(false)
                }
            }
        }
    }


    class ImageLoader(val context: Context) : XPopupImageLoader {
        private val cdn = "https://nmbimg.fastmirror.org/image/"

        override fun getImageFile(context: Context, uri: Any): File? {
            try {
                return Glide.with(context).downloadOnly().load(cdn + uri).submit().get()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return null
        }

        override fun loadImage(position: Int, uri: Any, imageView: ImageView) {
            Glide.with(context).load(cdn + uri)
                .apply(RequestOptions().override(SIZE_ORIGINAL, SIZE_ORIGINAL))
                .into(imageView)
        }
    }
}
package com.laotoua.dawnislandk.screens.widgets.popups

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.os.Environment
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.laotoua.dawnislandk.R
import com.laotoua.dawnislandk.util.ImageUtil
import com.laotoua.dawnislandk.util.ReadableTime
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
    fragment: Fragment? = null,
    activity: Activity? = null
) :
    ImageViewerPopupView(fragment?.requireContext() ?: activity!!) {

    companion object {
        val universalImageLoader = ImageLoader()
    }

    private val caller = activity ?: fragment!!.requireActivity()
    private val toastMsg = MutableLiveData<Int>()
    private var saveShown = true
    private val saveButton by lazyOnMainOnly { findViewById<FloatingActionButton>(R.id.save) }

    override fun getImplLayoutId(): Int {
        return R.layout.popup_image_viewer
    }

    override fun onCreate() {
        super.onCreate()
        setXPopupImageLoader(universalImageLoader)
    }

    override fun initPopupContent() {
        super.initPopupContent()
        pager.adapter = PopupPVA()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        this.isShowSaveBtn = false
        saveButton.setOnClickListener { addPicToGallery(context, imgUrl) }
        toastMsg.observe(caller as LifecycleOwner, Observer {
            Toast.makeText(caller, it, Toast.LENGTH_SHORT).show()
        })
    }

    private fun addPicToGallery(context: Context, imgUrl: String) {
        (caller as LifecycleOwner).lifecycleScope.launch(Dispatchers.IO) {
            Timber.i("Saving image $imgUrl to Gallery... ")
            val relativeLocation = Environment.DIRECTORY_PICTURES + File.separator + "Dawn"
            var fileName = imgUrl.substringAfter("/")
            val fileExist = ImageUtil.imageExistInGalleryBasedOnFilenameAndExt(
                caller,
                fileName,
                relativeLocation
            )
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
            if (fileExist && saved) toastMsg.postValue(R.string.image_already_exists_in_picture)
            else if (saved) toastMsg.postValue(R.string.image_saved)
            else toastMsg.postValue(R.string.something_went_wrong)
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
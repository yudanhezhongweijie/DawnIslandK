package com.laotoua.dawnislandk.screens.widget.popup

import android.content.Context
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.laotoua.dawnislandk.util.Constants
import com.laotoua.dawnislandk.util.GlideApp
import com.lxj.xpopup.interfaces.XPopupImageLoader
import java.io.File

class ImageLoader :
    XPopupImageLoader {
    private val cdn = Constants.imageCDN

    override fun getImageFile(context: Context, uri: Any): File? {
        try {
            return Glide.with(context).downloadOnly().load(cdn + uri).submit().get()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    override fun loadImage(position: Int, uri: Any, imageView: ImageView) {
        GlideApp.with(imageView).load(cdn + uri)
            .apply(
                RequestOptions().override(
                    Target.SIZE_ORIGINAL,
                    Target.SIZE_ORIGINAL
                )
            )
            .into(imageView)
    }
}
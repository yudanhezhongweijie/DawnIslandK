package com.laotoua.dawnislandk.data.network

import android.content.Context
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.laotoua.dawnislandk.ui.util.GlideApp
import com.laotoua.dawnislandk.util.Constants
import com.lxj.xpopup.interfaces.XPopupImageLoader
import java.io.File

class ImageLoader(val context: Context) :
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
        GlideApp.with(context).load(cdn + uri)
            .apply(
                RequestOptions().override(
                    Target.SIZE_ORIGINAL,
                    Target.SIZE_ORIGINAL
                )
            )
            .into(imageView)
    }
}
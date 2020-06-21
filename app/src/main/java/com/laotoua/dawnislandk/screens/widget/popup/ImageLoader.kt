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

    override fun getImageFile(context: Context, uri: Any): File? {
        val imgUrl = if (uri.toString().startsWith("http")) uri else Constants.imageCDN + uri
        try {
            return Glide.with(context).downloadOnly().load(imgUrl).submit().get()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    override fun loadImage(position: Int, uri: Any, imageView: ImageView) {
        val imgUrl = if (uri.toString().startsWith("http")) uri else Constants.imageCDN + uri
        GlideApp.with(imageView).load(imgUrl)
            .apply(
                RequestOptions().override(
                    Target.SIZE_ORIGINAL,
                    Target.SIZE_ORIGINAL
                )
            )
            .into(imageView)
    }
}
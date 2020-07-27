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

import android.content.Context
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.laotoua.dawnislandk.R
import com.laotoua.dawnislandk.util.DawnConstants
import com.laotoua.dawnislandk.util.GlideApp
import com.lxj.xpopup.interfaces.XPopupImageLoader
import java.io.File

class ImageLoader :
    XPopupImageLoader {

    override fun getImageFile(context: Context, uri: Any): File? {
        val imgUrl = if (uri.toString().startsWith("http")) uri else DawnConstants.imageCDN + uri
        try {
            return Glide.with(context).downloadOnly().load(imgUrl).submit().get()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    override fun loadImage(position: Int, uri: Any, imageView: ImageView) {
        val imgUrl = if (uri.toString().startsWith("http")) uri else DawnConstants.imageCDN + uri
        GlideApp.with(imageView)
            .load(imgUrl)
            .placeholder(R.drawable.placeholder)
            .apply(
                RequestOptions().override(
                    Target.SIZE_ORIGINAL,
                    Target.SIZE_ORIGINAL
                )
            )
            .into(imageView)
    }
}
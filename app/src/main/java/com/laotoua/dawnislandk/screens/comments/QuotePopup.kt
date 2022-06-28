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

package com.laotoua.dawnislandk.screens.comments

import android.annotation.SuppressLint
import android.text.method.LinkMovementMethod
import android.view.View
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.laotoua.dawnislandk.DawnApp
import com.laotoua.dawnislandk.R
import com.laotoua.dawnislandk.data.local.entity.Comment
import com.laotoua.dawnislandk.screens.util.ContentTransformation.transformContent
import com.laotoua.dawnislandk.screens.util.ContentTransformation.transformCookie
import com.laotoua.dawnislandk.screens.util.ContentTransformation.transformTime
import com.laotoua.dawnislandk.screens.widgets.popups.ImageViewerPopup
import com.laotoua.dawnislandk.screens.widgets.spans.ReferenceSpan
import com.laotoua.dawnislandk.util.DataResource
import com.laotoua.dawnislandk.util.GlideApp
import com.laotoua.dawnislandk.util.LoadingStatus
import com.lxj.xpopup.XPopup
import com.lxj.xpopup.core.CenterPopupView
import com.lxj.xpopup.util.XPopupUtils

@SuppressLint("ViewConstructor")
// uses caller fragment's context, should not live without fragment
class QuotePopup(
    caller: CommentsFragment,
    liveQuote: LiveData<DataResource<Comment>>,
    private val currentPostId: String,
    private val po: String
) : CenterPopupView(caller.requireContext()) {

    private var mCaller: CommentsFragment? = caller
    private var mLiveQuote: LiveData<DataResource<Comment>>? = liveQuote
    override fun getImplLayoutId(): Int = R.layout.popup_quote

    override fun getMaxWidth(): Int = (XPopupUtils.getWindowWidth(context) * .9f).toInt()

    private val liveQuoteObs = Observer<DataResource<Comment>> {
        when (it.status) {
            LoadingStatus.SUCCESS -> {
                convertQuote(it.data!!, po)
                findViewById<ProgressBar>(R.id.progressBar).visibility = View.GONE
                findViewById<ConstraintLayout>(R.id.quote).visibility = View.VISIBLE
            }
            LoadingStatus.ERROR -> {
                dismiss()
                Toast.makeText(context, it.message, Toast.LENGTH_LONG).show()
            }
            // do nothing when loading or no data
            else -> {}
        }
    }

    fun listenToLiveQuote(lifecycleOwner: LifecycleOwner) {
        findViewById<ProgressBar>(R.id.progressBar).visibility = View.VISIBLE
        findViewById<ConstraintLayout>(R.id.quote).visibility = View.GONE
        mLiveQuote?.observe(lifecycleOwner, liveQuoteObs)
    }

    private fun convertQuote(quote: Comment, po: String) {
        mLiveQuote?.removeObserver(liveQuoteObs)

        findViewById<TextView>(R.id.userHash).text = transformCookie(quote.userHash, quote.admin, po)

        findViewById<ImageView>(R.id.OPHighlight).visibility =
            if (quote.userHash == po) View.VISIBLE else View.GONE

        findViewById<TextView>(R.id.timestamp).text = transformTime(quote.now)

        findViewById<TextView>(R.id.refId).text =
            context.resources.getString(R.string.ref_id_formatted, quote.id)

        findViewById<TextView>(R.id.sage).run {
            visibility = if (quote.sage == "1") {
                View.VISIBLE
            } else {
                View.GONE
            }
        }

        val title = quote.getSimplifiedTitle()
        findViewById<TextView>(R.id.title).run {
            if (title.isNotBlank()) {
                text = title
                visibility = View.VISIBLE
            } else {
                visibility = View.GONE
            }
        }

        val name = quote.getSimplifiedName()
        findViewById<TextView>(R.id.name).run {
            if (name.isNotBlank()) {
                text = name
                visibility = View.VISIBLE
            } else {
                visibility = View.GONE
            }
        }

        // load image
        findViewById<ImageView>(R.id.attachedImage).run {
            visibility = if (quote.img.isNotBlank()) {
                GlideApp.with(context)
                    .load(DawnApp.currentThumbCDN + quote.img + quote.ext)
//                    .override(250, 250)
                    .fitCenter()
                    .into(this)
                View.VISIBLE
            } else {
                View.GONE
            }
            setOnClickListener { imageView ->
                val viewerPopup = ImageViewerPopup(context)
                viewerPopup.setSingleSrcView(imageView as ImageView?, quote)
                XPopup.Builder(context)
                    .asCustom(viewerPopup)
                    .show()
            }
        }

        val referenceClickListener = object : ReferenceSpan.ReferenceClickHandler {
            override fun handleReference(id: String) {
                if (isShow) {
                    mCaller?.displayQuote(id)
                }
            }
        }

        findViewById<TextView>(R.id.content).run {
            maxLines = 15
            movementMethod = LinkMovementMethod.getInstance()
            text = transformContent(
                context,
                quote.content,
                DawnApp.applicationDataStore.lineHeight,
                DawnApp.applicationDataStore.segGap, referenceClickListener
            )
            textSize = DawnApp.applicationDataStore.textSize
            letterSpacing = DawnApp.applicationDataStore.letterSpace
        }

        findViewById<Button>(R.id.jumpToQuotedPost).run {
            visibility = if (quote.parentId != currentPostId) View.VISIBLE else View.GONE
            setOnClickListener {
                if (isShow) {
                    mCaller?.jumpToNewPost(quote.parentId)
                }
            }
        }
    }

    override fun onDismiss() {
        mLiveQuote?.removeObserver(liveQuoteObs)
        mLiveQuote = null
        mCaller = null
        super.onDismiss()
    }
}
package com.laotoua.dawnislandk.screens.replys

import android.annotation.SuppressLint
import android.content.Context
import android.text.method.LinkMovementMethod
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.laotoua.dawnislandk.DawnApp
import com.laotoua.dawnislandk.R
import com.laotoua.dawnislandk.data.local.Reply
import com.laotoua.dawnislandk.screens.util.ContentTransformation.transformContent
import com.laotoua.dawnislandk.screens.util.ContentTransformation.transformCookie
import com.laotoua.dawnislandk.screens.util.ContentTransformation.transformTime
import com.laotoua.dawnislandk.screens.widget.popup.ImageLoader
import com.laotoua.dawnislandk.screens.widget.popup.ImageViewerPopup
import com.laotoua.dawnislandk.screens.widget.span.ReferenceSpan
import com.laotoua.dawnislandk.util.*
import com.lxj.xpopup.XPopup
import com.lxj.xpopup.core.CenterPopupView
import com.lxj.xpopup.interfaces.SimpleCallback
import com.lxj.xpopup.util.XPopupUtils
import dagger.android.support.DaggerFragment

@SuppressLint("ViewConstructor")
// uses caller fragment's context, should not live without fragment
class QuotePopup(
    private val caller: DaggerFragment,
    private val replyVM: ReplysViewModel,
    private val quoteId: String,
    private val po: String
) : CenterPopupView(caller.requireContext()) {
    private val imageLoader: ImageLoader by lazyOnMainOnly { ImageLoader() }

    override fun getImplLayoutId(): Int = R.layout.popup_quote

    override fun getMaxWidth(): Int = (XPopupUtils.getWindowWidth(context) * .9f).toInt()

    private val liveQuote: LiveData<Reply> = replyVM.getQuote(quoteId)

    private val liveQuoteObs = Observer<Reply> {
        if (it != null) {
            convertQuote(it, po)
            findViewById<ProgressBar>(R.id.progressBar).visibility = View.GONE
            findViewById<ConstraintLayout>(R.id.quote).visibility = View.VISIBLE
        }
    }

    private val quoteDownloadStatusObs = Observer<EventPayload<String>> {
        if (it.loadingStatus == LoadingStatus.FAILED && it.payload == quoteId) {
            ensureQuotePopupDismissal()
            Toast.makeText(context, it.message, Toast.LENGTH_LONG).show()
        }
    }

    fun listenToLiveQuote() {
        findViewById<ProgressBar>(R.id.progressBar).visibility = View.VISIBLE
        findViewById<ConstraintLayout>(R.id.quote).visibility = View.GONE
        // observe quote live quote, loading Status
        liveQuote.observe(caller.viewLifecycleOwner, liveQuoteObs)
        replyVM.quoteLoadingStatus.observe(caller.viewLifecycleOwner, quoteDownloadStatusObs)
    }

    private fun convertQuote(quote: Reply, po: String) {
        // remove observers when data has come back
        liveQuote.removeObserver(liveQuoteObs)
        replyVM.quoteLoadingStatus.removeObserver(quoteDownloadStatusObs)

        findViewById<TextView>(R.id.userId).text =
            transformCookie(
                quote.userid,
                quote.admin,
                po
            )

        findViewById<TextView>(R.id.timestamp).text = transformTime(quote.now)

        // TODO: handle ads
        findViewById<TextView>(R.id.refId).text =
            context.resources.getString(R.string.ref_id_formatted, quote.id)

        // TODO: add sage transformation
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
            visibility = if (quote.img != "") {
                GlideApp.with(context)
                    .load(Constants.thumbCDN + quote.img + quote.ext)
//                    .override(250, 250)
                    .fitCenter()
                    .into(this)
                View.VISIBLE
            } else {
                View.GONE
            }
            setOnClickListener { imageView ->
                val url = quote.getImgUrl()
                val viewerPopup =
                    ImageViewerPopup(
                        url,
                        fragment = caller
                    )
                viewerPopup.setXPopupImageLoader(imageLoader)
                viewerPopup.setSingleSrcView(imageView as ImageView?, url)
                XPopup.Builder(context)
                    .asCustom(viewerPopup)
                    .show()
            }
        }

        val referenceClickListener = object : ReferenceSpan.ReferenceClickHandler {
            override fun handleReference(id: String) {
                showQuote(
                    caller,
                    replyVM,
                    context,
                    id,
                    po
                )
            }
        }

        findViewById<TextView>(R.id.content).run {
            /** when TextView is scrolled, resetting text does not reset scroll position
             *  WITHOUT scroll reset, text is not shown
             */
            maxLines = 15
            scrollY = 0
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
    }

    override fun onDismiss() {
        super.onDismiss()
        quotePopupList.remove(this)
    }

    companion object {

        private var quotePopupList = mutableListOf<QuotePopup>()

        fun ensureQuotePopupDismissal(): Boolean {
            quotePopupList.lastOrNull { it.isShow }.run {
                this?.dismiss()
                return this == null
            }
        }

        fun clearQuotePopups() {
            quotePopupList.clear()
        }

        fun showQuote(
            caller: DaggerFragment,
            replyVM: ReplysViewModel,
            context: Context,
            id: String,
            po: String
        ) {
            val top = QuotePopup(caller, replyVM, id, po)
            quotePopupList.add(top)
            XPopup.Builder(context)
                .setPopupCallback(object : SimpleCallback() {
                    override fun beforeShow() {
                        super.beforeShow()
                        top.listenToLiveQuote()
                    }
                })
                .asCustom(top)
                .show()
        }
    }
}
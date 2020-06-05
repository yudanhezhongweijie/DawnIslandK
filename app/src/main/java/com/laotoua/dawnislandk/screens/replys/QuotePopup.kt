package com.laotoua.dawnislandk.screens.replys

import android.annotation.SuppressLint
import android.content.Context
import android.text.method.LinkMovementMethod
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.laotoua.dawnislandk.DawnApp
import com.laotoua.dawnislandk.R
import com.laotoua.dawnislandk.data.local.Reply
import com.laotoua.dawnislandk.data.remote.NMBServiceClient
import com.laotoua.dawnislandk.data.repository.DataResource
import com.laotoua.dawnislandk.screens.util.ContentTransformation.transformContent
import com.laotoua.dawnislandk.screens.util.ContentTransformation.transformCookie
import com.laotoua.dawnislandk.screens.util.ContentTransformation.transformTime
import com.laotoua.dawnislandk.screens.widget.popup.ImageLoader
import com.laotoua.dawnislandk.screens.widget.popup.ImageViewerPopup
import com.laotoua.dawnislandk.util.Constants
import com.laotoua.dawnislandk.util.GlideApp
import com.laotoua.dawnislandk.util.lazyOnMainOnly
import com.lxj.xpopup.XPopup
import com.lxj.xpopup.core.CenterPopupView
import com.lxj.xpopup.interfaces.SimpleCallback
import dagger.android.support.DaggerFragment
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@SuppressLint("ViewConstructor")
class QuotePopup(private val caller: DaggerFragment, context: Context) : CenterPopupView(context) {

    private val imageLoader: ImageLoader by lazyOnMainOnly { ImageLoader() }

    override fun getImplLayoutId(): Int {
        return R.layout.popup_quote
    }

    init {
        caller.androidInjector().inject(this)
    }

    @Inject
    lateinit var webServiceClient: NMBServiceClient

    enum class DownloadStatus {
        IDLE,
        SUCCESS,
        DOWNLOADING,
        ERROR
    }

    private var status: DownloadStatus = DownloadStatus.IDLE

    private var quote: Reply? = null

    private lateinit var errorMsg: String

    private var mPo: String = ""

    private fun downloadQuote(id: String, po: String) {
        caller.lifecycleScope.launch {
            status = DownloadStatus.DOWNLOADING
            mPo = po
            DataResource.create(webServiceClient.getQuote(id)).run {
                status = when (this) {
                    is DataResource.Error -> {
                        errorMsg = message
                        Toast.makeText(
                            context,
                            "$errorMsg...",
                            Toast.LENGTH_SHORT
                        )
                            .show()
                        DownloadStatus.ERROR
                    }
                    is DataResource.Success -> {
                        quote = data!!
                        DownloadStatus.SUCCESS
                    }
                }
            }

            showAfterDownload()
        }
    }

    private fun convertQuote() {
        if (status != DownloadStatus.SUCCESS || quote == null) {
            Timber.e("Quote is not ready")
            return
        }
        findViewById<TextView>(R.id.userId).text =
            transformCookie(
                quote!!.userid,
                quote!!.admin,
                mPo
            )

        findViewById<TextView>(R.id.timestamp).text =
            transformTime(quote!!.now)

        // TODO: handle ads
        findViewById<TextView>(R.id.refId).text = quote!!.id

        // TODO: add sage transformation
        findViewById<TextView>(R.id.sage).run {
            visibility = if (quote!!.sage == "1") {
                View.VISIBLE
            } else {
                View.GONE
            }
        }

        val titleAndName = quote!!.getTitleAndName()
        findViewById<TextView>(R.id.titleAndName).run {
            if (titleAndName != "") {
                text = titleAndName
                visibility = View.VISIBLE
            } else {
                visibility = View.GONE
            }
        }

        // load image
        findViewById<ImageView>(R.id.attachedImage).run {
            visibility = if (quote!!.img != "") {
                GlideApp.with(context)
                    .load(Constants.thumbCDN + quote!!.img + quote!!.ext)
//                    .override(250, 250)
                    .fitCenter()
                    .into(this)
                View.VISIBLE
            } else {
                View.GONE
            }
            setOnClickListener { imageView ->
                val url = quote!!.getImgUrl()
                val viewerPopup =
                    ImageViewerPopup(
                        caller,
                        context,
                        url
                    )
                viewerPopup.setXPopupImageLoader(imageLoader)
                viewerPopup.setSingleSrcView(imageView as ImageView?, url)
                XPopup.Builder(context)
                    .asCustom(viewerPopup)
                    .show()
            }
        }

        val referenceClickListener: (id: String) -> Unit = { id ->
            // TODO: get Po based on Thread
            showQuote(
                caller,
                context,
                id,
                mPo
            )
        }

        findViewById<TextView>(R.id.content).run {
            /** when TextView is scrolled, resetting text does not reset scroll position
             *  WITHOUT scroll reset, text is not shown
             */
            maxLines = 15
            scrollY = 0
            movementMethod = LinkMovementMethod.getInstance()
            text = transformContent(
                quote!!.content,
                DawnApp.applicationDataStore.lineHeight,
                DawnApp.applicationDataStore.segGap, referenceClickListener
            )
            textSize = DawnApp.applicationDataStore.textSize
            letterSpacing = DawnApp.applicationDataStore.letterSpace
        }
    }

    private fun showAfterDownload() {
        if (status == DownloadStatus.SUCCESS) {
            XPopup.Builder(context)
                .setPopupCallback(object : SimpleCallback() {
                    override fun beforeShow() {
                        super.beforeShow()
                        convertQuote()
                    }
                })
                .asCustom(this)
                .show()
        }
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


        // TODO Repository for quotes
        fun showQuote(
            caller: DaggerFragment,
            context: Context,
            id: String,
            po: String
        ) {
            var top = quotePopupList.firstOrNull { it.isDismiss }
            if (top == null) {
                top = QuotePopup(caller, context)
                quotePopupList.add(top)
            }
            when (top.status) {
                DownloadStatus.SUCCESS -> {
                    if (top.quote?.id != id) {
                        top.downloadQuote(id, po)
                    } else if (!top.isShow) {
                        top.showAfterDownload()
                    }
                }
                DownloadStatus.IDLE -> {
                    top.downloadQuote(id, po)
                }
                DownloadStatus.DOWNLOADING -> {
                    // TODO: add animation
                    Timber.d("Downloading quote")
                }
                // retry
                DownloadStatus.ERROR -> {
                    Timber.d("Didn't get quote. Retrying...")
                    top.downloadQuote(id, po)
                }
            }
        }


    }
}
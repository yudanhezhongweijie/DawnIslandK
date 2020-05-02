package com.laotoua.dawnislandk.ui.popup

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.laotoua.dawnislandk.R
import com.laotoua.dawnislandk.data.entity.Reply
import com.laotoua.dawnislandk.data.network.ImageLoader
import com.laotoua.dawnislandk.data.network.NMBServiceClient
import com.laotoua.dawnislandk.ui.util.ContentTransformationUtil.transformContent
import com.laotoua.dawnislandk.ui.util.ContentTransformationUtil.transformCookie
import com.laotoua.dawnislandk.ui.util.ContentTransformationUtil.transformTime
import com.laotoua.dawnislandk.ui.util.ContentTransformationUtil.transformTitleAndName
import com.laotoua.dawnislandk.ui.util.GlideApp
import com.laotoua.dawnislandk.util.Constants
import com.laotoua.dawnislandk.viewmodel.DataResource
import com.lxj.xpopup.XPopup
import com.lxj.xpopup.core.CenterPopupView
import com.lxj.xpopup.interfaces.SimpleCallback
import kotlinx.coroutines.launch

@SuppressLint("ViewConstructor")
class QuotePopup(private val caller: Fragment, context: Context) : CenterPopupView(context) {

    private val imageLoader: ImageLoader by lazy { ImageLoader(context) }

    override fun getImplLayoutId(): Int {
        return R.layout.quote_popup
    }

    private fun convertQuote(quote: Reply, po: String) {
        findViewById<TextView>(R.id.quoteCookie).text =
            transformCookie(
                quote.userid,
                quote.admin!!,
                po
            )

        findViewById<TextView>(R.id.quoteTime).text =
            transformTime(quote.now)

        // TODO: handle ads
        findViewById<TextView>(R.id.quoteId).text = quote.id

        // TODO: add sage transformation
        findViewById<TextView>(R.id.sage).run {
            visibility = if (quote.sage == "1") {
                View.VISIBLE
            } else {
                View.GONE
            }
        }

        val titleAndName =
            transformTitleAndName(
                quote.title,
                quote.name
            )
        findViewById<TextView>(R.id.quoteTitleAndName).run {
            if (titleAndName != "") {
                text = titleAndName
                visibility = View.VISIBLE
            } else {
                visibility = View.GONE
            }
        }

        // load image
        findViewById<ImageView>(R.id.quoteImage).run {
            visibility = if (quote.img != "") {
                GlideApp.with(context)
                    .load(Constants.thumbCDN + quote.img + quote.ext)
                    .override(250, 250)
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
            val quotePopup = QuotePopup(caller, context)
            showQuote(caller, context, quotePopup, id, po)
        }

        findViewById<TextView>(R.id.quoteContent).run {
            val lineHeight =
                PreferenceManager.getDefaultSharedPreferences(context)
                    .getInt(Constants.LINE_HEIGHT, 0)
            val segGap =
                PreferenceManager.getDefaultSharedPreferences(context).getInt(Constants.SEG_GAP, 0)
            text = transformContent(
                quote.content, lineHeight, segGap, referenceClickListener
            )
            letterSpacing = PreferenceManager.getDefaultSharedPreferences(context)
                .getInt(Constants.LETTER_SPACE, 1) / 50f
        }

    }

    companion object {
        fun showQuote(
            caller: Fragment,
            context: Context,
            quotePopup: QuotePopup,
            id: String,
            po: String
        ) {
            caller.lifecycleScope.launch {
                DataResource.create(NMBServiceClient.getQuote(id)).run {
                    when (this) {
                        is DataResource.Error -> {
                            Toast.makeText(
                                context,
                                "${message}...",
                                Toast.LENGTH_SHORT
                            )
                                .show()
                        }
                        is DataResource.Success -> {
                            XPopup.Builder(context)
                                .setPopupCallback(object : SimpleCallback() {
                                    override fun beforeShow() {
                                        super.beforeShow()
                                        quotePopup.convertQuote(data!!, po)
                                    }
                                })
                                .asCustom(quotePopup)
                                .show()
                        }
                    }
                }
            }
        }
    }
}
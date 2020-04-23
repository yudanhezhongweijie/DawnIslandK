package com.laotoua.dawnislandk.ui.popup

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.laotoua.dawnislandk.R
import com.laotoua.dawnislandk.data.entity.Reply
import com.laotoua.dawnislandk.data.network.ImageLoader
import com.laotoua.dawnislandk.data.network.NMBServiceClient
import com.laotoua.dawnislandk.ui.util.*
import com.lxj.xpopup.XPopup
import com.lxj.xpopup.core.CenterPopupView
import com.lxj.xpopup.interfaces.SimpleCallback
import kotlinx.android.synthetic.main.quote_list_item.view.*
import kotlinx.coroutines.launch
import timber.log.Timber

@SuppressLint("ViewConstructor")
class QuotePopup(private val caller: Fragment, context: Context) : CenterPopupView(context) {

    private val imageLoader: ImageLoader by lazy { ImageLoader(context) }

    // TODO: clean CDN
    private val thumbCDN = "https://nmbimg.fastmirror.org/thumb/"
    override fun getImplLayoutId(): Int {
        return R.layout.quote_popup
    }

    private fun convertReply(reply: Reply, po: String) {
        findViewById<TextView>(R.id.replyCookie).text =
            transformCookie(
                reply.userid,
                reply.admin!!,
                po
            )

        findViewById<TextView>(R.id.replyTime).text =
            transformTime(reply.now)

        // TODO: handle ads
        findViewById<TextView>(R.id.replyId).text = reply.id

        // TODO: add sage transformation
        findViewById<TextView>(R.id.sage).run {
            visibility = if (reply.sage == "1") {
                View.VISIBLE
            } else {
                View.GONE
            }
        }

        val titleAndName =
            transformTitleAndName(
                reply.title,
                reply.name
            )
        findViewById<TextView>(R.id.replyTitleAndName).run {
            if (titleAndName != "") {
                text = titleAndName
                visibility = View.VISIBLE
            } else {
                visibility = View.GONE
            }
        }

        // load image
        findViewById<ImageView>(R.id.replyImage).run {
            visibility = if (reply.img != "") {
                GlideApp.with(context)
                    .load(thumbCDN + reply.img + reply.ext)
                    .override(250, 250)
                    .fitCenter()
                    .into(this)
                View.VISIBLE
            } else {
                View.GONE
            }
            setOnClickListener { imageView ->
                Timber.i("clicked on image in quote ${reply.id}")

                val url = reply.getImgUrl()
                // TODO support multiple image
                val viewerPopup =
                    ImageViewerPopup(
                        caller,
                        context,
                        url
                    )
                viewerPopup.setXPopupImageLoader(imageLoader)
                viewerPopup.setSingleSrcView(imageView as ImageView?, url)
                viewerPopup.setOnClickListener {
                    Timber.i("on click in thread")
                }
                XPopup.Builder(context)
                    .asCustom(viewerPopup)
                    .show()
            }
        }

        findViewById<LinearLayout>(R.id.replyQuotes).run {
            val quotes =
                extractQuote(reply.content)
            if (quotes.isNotEmpty()) {
                removeAllViews()
                val quotePopup = QuotePopup(caller, context)
                quotes.map { id ->
                    val q = LayoutInflater.from(context)
                        .inflate(R.layout.quote_list_item, this as ViewGroup, false)
                    q.quoteId.text = "No. $id"
                    q.setOnClickListener {
                        // TODO: get Po based on Thread
                        showQuote(caller, context, quotePopup, id, po)
                    }
                    addView(q)
                }
                visibility = View.VISIBLE
            } else {
                visibility = View.GONE
            }
        }

        findViewById<TextView>(R.id.replyContent).run {
            text = transformContent(
                removeQuote(reply.content)
            )
            setOnClickListener {
                Timber.i("Quote content click listener not implemented yet")
            }
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
                try {
                    val reply = NMBServiceClient.getQuote(id)
                    XPopup.Builder(context)
                        .setPopupCallback(object : SimpleCallback() {
                            override fun beforeShow() {
                                super.beforeShow()
                                quotePopup.convertReply(reply, po)
                            }
                        })
                        .asCustom(quotePopup)
                        .show()
                } catch (e: Exception) {
                    Timber.e(e, "Failed to get quote..")
                    Toast.makeText(context, "无法读取引用...", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
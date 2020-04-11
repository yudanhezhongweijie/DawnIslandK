package com.laotoua.dawnislandk.components

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.laotoua.dawnislandk.R
import com.laotoua.dawnislandk.entities.Reply
import com.laotoua.dawnislandk.util.*
import com.lxj.xpopup.core.CenterPopupView
import kotlinx.android.synthetic.main.quote_list_item.view.*

class QuotePopup(context: Context) : CenterPopupView(context) {

    private val replyMap = mutableMapOf<String, Reply>()

    // TODO
    private val thumbCDN = "https://nmbimg.fastmirror.org/thumb/"
    override fun getImplLayoutId(): Int {
        return R.layout.quote_popup
    }

    fun showReply(reply: Reply) {
        replyMap[reply.id] = reply
        convertReply(reply.id)
    }

    private fun convertReply(id: String) {
        val reply = replyMap[id]!!
        // TODO: fix po
        findViewById<TextView>(R.id.replyCookie).text =
            transformCookie(reply.userid, reply.admin!!, "")
//        card.setText(R.id.replyCookie, transformCookie(item.userid, item.admin!!, po))

        findViewById<TextView>(R.id.replyTime).text = transformTime(reply.now)
//        card.setText(R.id.replyTime, transformTime(item.now))
        // TODO: handle ads
        findViewById<TextView>(R.id.replyId).text = reply.id

//        card.setText(R.id.replyId, item.id)

        // TODO: add sage transformting
        findViewById<TextView>(R.id.sage).let {
            if (reply.sage == "1") {
                it.visibility = View.VISIBLE
            } else {
                it.visibility = View.GONE
            }
        }
//        if (item.sage == "1") {
//            card.setVisible(R.id.sage, true)
//        } else {
//            card.setGone(R.id.sage, true)
//        }

        val titleAndName = transformTitleAndName(reply.title, reply.name)
        findViewById<TextView>(R.id.replyTitleAndName).let {
            if (titleAndName != "") {
                it.text = titleAndName
                it.visibility = View.VISIBLE
            } else {
                it.visibility = View.GONE
            }
        }
//        if (titleAndName != "") {
//            card.setText(R.id.replyTitleAndName, titleAndName)
//            card.setVisible(R.id.replyTitleAndName, true)
//        } else {
//            card.setGone(R.id.replyTitleAndName, true)
//        }

        // TODO: click listener
        // load image
        findViewById<ImageView>(R.id.replyImage).let {
            if (reply.img != "") {
                GlideApp.with(context)
                    .load(thumbCDN + reply.img + reply.ext)
                    .override(250, 250)
                    .fitCenter()
                    .into(it)
                it.visibility = View.VISIBLE
            } else {
                it.visibility = View.GONE
            }
        }

        // TODO: click listener
        // TODO: need quotation handler, should be done in view however
        findViewById<LinearLayout>(R.id.replyQuotes).let { linearLayout ->
//            val quotesContainer: LinearLayout = card.getView(R.id.replyQuotes)
            val quotes = extractQuote(reply.content)
            if (quotes.isNotEmpty()) {
                linearLayout.removeAllViews()

                quotes.map {
                    val q = LayoutInflater.from(context)
                        .inflate(R.layout.quote_list_item, linearLayout as ViewGroup, false)
                    q.quoteId.text = "No. $it"
                    linearLayout.addView(q)
                }
                // special binding for quotes
//                bindCustomQuoteClickListener(card, R.id.quoteId, R.id.replyQuotes)

//                card.setVisible(R.id.replyQuotes, true)
                linearLayout.visibility = View.VISIBLE
            } else {
//                card.setGone(R.id.replyQuotes, true)
                linearLayout.visibility = View.GONE
            }
        }

        // TODO: click listener
//        card.setText(R.id.replyContent, transformContent(removeQuote(item.content)))
        findViewById<TextView>(R.id.replyContent).text =
            transformContent(removeQuote(reply.content))
    }
}
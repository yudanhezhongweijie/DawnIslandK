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

package com.laotoua.dawnislandk.screens.subscriptions

import android.os.Bundle
import android.os.Handler
import android.view.*
import android.widget.ImageView
import android.widget.Toast
import androidx.core.text.isDigitsOnly
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.setActionButtonEnabled
import com.afollestad.materialdialogs.input.getInputField
import com.afollestad.materialdialogs.input.input
import com.chad.library.adapter.base.binder.QuickItemBinder
import com.chad.library.adapter.base.util.getItemView
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.google.android.material.card.MaterialCardView
import com.laotoua.dawnislandk.DawnApp
import com.laotoua.dawnislandk.MainNavDirections
import com.laotoua.dawnislandk.R
import com.laotoua.dawnislandk.data.local.entity.FeedAndPost
import com.laotoua.dawnislandk.databinding.FragmentSubscriptionFeedBinding
import com.laotoua.dawnislandk.screens.adapters.*
import com.laotoua.dawnislandk.screens.posts.PostCardFactory
import com.laotoua.dawnislandk.screens.util.Layout.updateHeaderAndFooter
import com.laotoua.dawnislandk.screens.widgets.BaseNavFragment
import com.laotoua.dawnislandk.screens.widgets.popups.ImageViewerPopup
import com.laotoua.dawnislandk.util.EventPayload
import com.laotoua.dawnislandk.util.SingleLiveEvent
import com.lxj.xpopup.XPopup
import me.dkzwm.widget.srl.RefreshingListenerAdapter
import me.dkzwm.widget.srl.config.Constants
import timber.log.Timber
import java.util.*


class FeedsFragment : BaseNavFragment() {

    companion object {
        fun newInstance() = FeedsFragment()
    }

    private var binding: FragmentSubscriptionFeedBinding? = null

    private var mAdapter: QuickMultiBinder? = null

    private val viewModel: FeedsViewModel by viewModels { viewModelFactory }

    private var mHandler: Handler? = null
    private val mDelayedLoad = Runnable {
        viewModel.getNextPage()
    }
    private var delayedLoading = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_fragment_feed, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.jump -> {
                MaterialDialog(requireContext()).show {
                    title(R.string.page_jump)
                    var page = 0
                    input(
                        waitForPositiveButton = false,
                        hintRes = R.string.please_input_page_number
                    ) { dialog, text ->
                        val inputField = getInputField()
                        page = if (text.isNotBlank() && text.isDigitsOnly()) {
                            text.toString().toInt()
                        } else {
                            0
                        }
                        val isValid = page > 0
                        inputField.error =
                            if (isValid) null else context.resources.getString(R.string.please_input_page_number)
                        dialog.setActionButtonEnabled(WhichButton.POSITIVE, isValid)
                    }
                    positiveButton(R.string.submit) {
                        viewModel.jumpToPage(page)
                    }
                }
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        if (mAdapter == null) {
            mAdapter = QuickMultiBinder(sharedVM).apply {
                addItemBinder(SimpleTextBinder())
                addItemBinder(FeedAndPostBinder().apply {
                    addChildClickViewIds(R.id.attachedImage)
                }, FeedAndPostDiffer())

                loadMoreModule.setOnLoadMoreListener {
                    viewModel.getNextPage()
                }
            }
        }
        if (binding != null) {
            Timber.d("Fragment View Reusing!")
        } else {
            Timber.d("Fragment View Created")
            binding = FragmentSubscriptionFeedBinding.inflate(inflater, container, false)
            binding!!.srlAndRv.recyclerView.apply {
                setHasFixedSize(true)
                layoutManager = LinearLayoutManager(context)
                adapter = mAdapter
            }

            mAdapter!!.setDefaultEmptyView()
            binding!!.srlAndRv.refreshLayout.apply {
                setOnRefreshListener(object : RefreshingListenerAdapter() {
                    override fun onRefreshing() {
                        viewModel.refreshOrGetPreviousPage()
                    }
                })
            }
        }
        return binding!!.root
    }

    private val delFeedResponseObs = Observer<SingleLiveEvent<String>> {
        it.getContentIfNotHandled()?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    private val loadingObs = Observer<SingleLiveEvent<EventPayload<Nothing>>> {
        if (mAdapter == null || binding == null) return@Observer
        it.getContentIfNotHandled()?.run {
            updateHeaderAndFooter(binding!!.srlAndRv.refreshLayout, mAdapter!!, this)
            delayedLoading = false
        }
    }

    private val feedsObs = Observer<List<FeedAndPost>> { list ->
        if (mAdapter == null) return@Observer
        if (list.isEmpty()) {
            if (viewModel.lastJumpPage > 0) {
                Toast.makeText(
                    context,
                    requireContext().getString(
                        R.string.no_feed_on_page,
                        viewModel.lastJumpPage
                    ),
                    Toast.LENGTH_SHORT
                ).show()
            }
            mAdapter!!.setDiffNewData(null)
            return@Observer
        }

        val data: MutableList<Any> = ArrayList()
        var lastPage: Int? = null
        list.map {
            if (it.feed.page != lastPage) {
                data.add("页数: ${it.feed.page}")
                lastPage = it.feed.page
            }
            if (it.post != null) {
                data.add(it)
            }
        }
        mAdapter!!.setDiffNewData(data)
        Timber.i("${this.javaClass.simpleName} Adapter will have ${list.size} feeds")
    }

    override fun onResume() {
        super.onResume()

        if (viewModel.feeds.value.isNullOrEmpty() && !delayedLoading) {
            binding?.srlAndRv?.refreshLayout?.autoRefresh(Constants.ACTION_NOTHING, false)
            // give sometime to skip load if bypassing this fragment
            mHandler = mHandler ?: Handler()
            delayedLoading = mHandler!!.postDelayed(mDelayedLoad, 500)
        }

        viewModel.delFeedResponse.observe(viewLifecycleOwner, delFeedResponseObs)
        viewModel.loadingStatus.observe(viewLifecycleOwner, loadingObs)
        viewModel.feeds.observe(viewLifecycleOwner, feedsObs)
    }

    override fun onPause() {
        super.onPause()
        mHandler?.removeCallbacks(mDelayedLoad)
        mHandler = null
        viewModel.delFeedResponse.removeObserver(delFeedResponseObs)
        viewModel.loadingStatus.removeObserver(loadingObs)
        viewModel.feeds.removeObserver(feedsObs)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (!DawnApp.applicationDataStore.getViewCaching()) {
            mAdapter = null
            binding = null
        }
        Timber.d("Fragment View Destroyed ${binding == null}")
    }

    private class SimpleTextBinder : QuickItemBinder<String>() {
        override fun convert(holder: BaseViewHolder, data: String) {
            holder.setText(R.id.text, data)
        }

        override fun getLayoutId(): Int = R.layout.list_item_simple_text
    }

    inner class FeedAndPostBinder : QuickItemBinder<FeedAndPost>() {
        override fun convert(holder: BaseViewHolder, data: FeedAndPost) {
            holder.convertUserId(data.post!!.userid, "0")
            holder.convertRefId(context, data.post.id)
            holder.convertTimeStamp(data.post.now)
            holder.convertTitleAndName(
                data.post.getSimplifiedTitle(),
                data.post.getSimplifiedName()
            )
            holder.convertImage(data.post.getImgUrl())
            holder.convertContent(context, data.post.content)
        }

        override fun getLayoutId(): Int = R.layout.list_item_post

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
            val view = parent.getItemView(getLayoutId()).applyTextSizeAndLetterSpacing()
            PostCardFactory.applySettings(view as MaterialCardView)
            return BaseViewHolder(view)
        }

        override fun onClick(
            holder: BaseViewHolder,
            view: View,
            data: FeedAndPost,
            position: Int
        ) {
            val navAction =
                MainNavDirections.actionGlobalCommentsFragment(data.feed.postId, data.post!!.fid)
            findNavController().navigate(navAction)
        }

        override fun onLongClick(
            holder: BaseViewHolder,
            view: View,
            data: FeedAndPost,
            position: Int
        ): Boolean {
            val id = data.feed.postId
            MaterialDialog(context).show {
                title(text = "删除订阅 $id?")
                positiveButton(R.string.delete) {
                    viewModel.deleteFeed(data.feed)
                }
                negativeButton(R.string.cancel)
            }
            return true
        }

        override fun onChildClick(
            holder: BaseViewHolder,
            view: View,
            data: FeedAndPost,
            position: Int
        ) {
            if (view.id == R.id.attachedImage) {
                val viewerPopup = ImageViewerPopup(context)
                viewerPopup.setSingleSrcView(view as ImageView?, data.post!!)
                XPopup.Builder(context)
                    .asCustom(viewerPopup)
                    .show()
            }
        }
    }

    private class FeedAndPostDiffer : DiffUtil.ItemCallback<FeedAndPost>() {
        override fun areItemsTheSame(oldItem: FeedAndPost, newItem: FeedAndPost): Boolean {
            return oldItem.feed.postId == newItem.feed.postId && oldItem.feed.category == newItem.feed.category
        }

        override fun areContentsTheSame(oldItem: FeedAndPost, newItem: FeedAndPost): Boolean {
            return oldItem.post == newItem.post
        }
    }
}


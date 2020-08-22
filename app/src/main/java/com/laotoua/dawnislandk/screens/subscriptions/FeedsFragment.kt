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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.text.isDigitsOnly
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
import com.laotoua.dawnislandk.screens.util.Layout.toast
import com.laotoua.dawnislandk.screens.util.Layout.updateHeaderAndFooter
import com.laotoua.dawnislandk.screens.widgets.BaseNavFragment
import com.laotoua.dawnislandk.screens.widgets.popups.ImageViewerPopup
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

    private var viewCaching = false
    private var refreshing = false

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
                addOnScrollListener(object : RecyclerView.OnScrollListener() {
                    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                        if (activity == null || !isAdded) return
                        if (dy > 0) {
                            binding?.jump?.hide()
                            binding?.jump?.isClickable = false
                        } else if (dy < 0) {
                            binding?.jump?.show()
                            binding?.jump?.isClickable = true
                        }
                    }

                })
            }

            binding!!.srlAndRv.refreshLayout.apply {
                setOnRefreshListener(object : RefreshingListenerAdapter() {
                    override fun onRefreshing() {
                        viewModel.refreshOrGetPreviousPage()
                    }
                })
            }

            binding!!.jump.setOnClickListener {
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
                        refreshing = true
                        viewModel.jumpToPage(page)
                    }
                    negativeButton(R.string.cancel)
                }
            }
        }

        viewModel.delFeedResponse.observe(viewLifecycleOwner, {
            it.getContentIfNotHandled()?.let { message ->
                toast(message)
            }
        })
        viewModel.loadingStatus.observe(viewLifecycleOwner) {
            if (mAdapter == null || binding == null) return@observe
            it.getContentIfNotHandled()?.run {
                updateHeaderAndFooter(binding!!.srlAndRv.refreshLayout, mAdapter!!, this)
            }
        }
        viewModel.feeds.observe(viewLifecycleOwner) { list ->
            if (mAdapter == null) return@observe
            if (list.isEmpty()) {
                if (viewModel.lastJumpPage > 0) {
                    toast(getString(R.string.no_feed_on_page, viewModel.lastJumpPage))
                }
                mAdapter?.setList(null)
                return@observe
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
            if (refreshing) {
                binding?.srlAndRv?.recyclerView?.scrollToPosition(0)
                mAdapter?.setNewInstance(data)
            } else {
                mAdapter?.setDiffNewData(data)
            }
            refreshing = false
            Timber.i("${this.javaClass.simpleName} Adapter will have ${list.size} feeds")
        }
        if (viewModel.feeds.value.isNullOrEmpty()) {
            binding?.srlAndRv?.refreshLayout?.autoRefresh(Constants.ACTION_NOTIFY, false)
        }

        viewCaching = false
        return binding!!.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (!viewCaching) {
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
                .convertRefId(context, data.post.id)
                .convertTimeStamp(data.post.now)
                .convertTitleAndName(
                    data.post.getSimplifiedTitle(),
                    data.post.getSimplifiedName()
                )
                .convertImage(data.post.getImgUrl())
                .convertContent(context, data.post.content)
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
            viewCaching = DawnApp.applicationDataStore.getViewCaching()
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


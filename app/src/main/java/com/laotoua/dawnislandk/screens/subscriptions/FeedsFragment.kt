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
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.afollestad.materialdialogs.MaterialDialog
import com.chad.library.adapter.base.binder.QuickItemBinder
import com.chad.library.adapter.base.util.getItemView
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.google.android.material.card.MaterialCardView
import com.laotoua.dawnislandk.R
import com.laotoua.dawnislandk.data.local.entity.FeedAndPost
import com.laotoua.dawnislandk.databinding.FragmentSubscriptionFeedBinding
import com.laotoua.dawnislandk.screens.MainActivity
import com.laotoua.dawnislandk.screens.SharedViewModel
import com.laotoua.dawnislandk.screens.adapters.*
import com.laotoua.dawnislandk.screens.posts.PostCardFactory
import com.laotoua.dawnislandk.screens.util.Layout.updateHeaderAndFooter
import com.laotoua.dawnislandk.screens.widgets.BaseNavFragment
import com.laotoua.dawnislandk.screens.widgets.popups.ImageViewerPopup
import com.laotoua.dawnislandk.util.LoadingStatus
import com.lxj.xpopup.XPopup
import me.dkzwm.widget.srl.RefreshingListenerAdapter
import me.dkzwm.widget.srl.config.Constants
import timber.log.Timber
import java.util.*


class FeedsFragment : BaseNavFragment() {

    companion object {
        fun newInstance() = FeedsFragment()
    }

    private var _binding: FragmentSubscriptionFeedBinding? = null
    private val binding: FragmentSubscriptionFeedBinding get() = _binding!!

    private val viewModel: FeedsViewModel by viewModels { viewModelFactory }

    private var mHandler: Handler? = null
    private val mDelayedLoad = Runnable {
        viewModel.getNextPage()
    }
    private var delayedLoading = false
    private var currentPage = 0

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
                Toast.makeText(requireContext(), "TODO", Toast.LENGTH_SHORT).show()
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentSubscriptionFeedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // initial load
        if (viewModel.feeds.value.isNullOrEmpty() && !delayedLoading) {
            binding.srlAndRv.refreshLayout.autoRefresh(Constants.ACTION_NOTHING, false)
            // give sometime to skip load if bypassing this fragment
            mHandler = mHandler ?: Handler()
            delayedLoading = mHandler!!.postDelayed(mDelayedLoad, 500)
        }

        val mAdapter = QuickMultiBinder(sharedVM).apply {
            addItemBinder(SimpleTextBinder())
            addItemBinder(FeedAndPostBinder(sharedVM, this@FeedsFragment).apply {
                addChildClickViewIds(R.id.attachedImage)
            })

            loadMoreModule.setOnLoadMoreListener {
                viewModel.getNextPage()
            }
        }

        binding.srlAndRv.recyclerView.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            adapter = mAdapter
        }

        binding.srlAndRv.refreshLayout.apply {
            setOnRefreshListener(object : RefreshingListenerAdapter() {
                override fun onRefreshing() {
                    viewModel.refreshOrGetPreviousPage()
                }
            })
        }

        viewModel.delFeedResponse.observe(viewLifecycleOwner, Observer {
            it.getContentIfNotHandled()?.let { eventPayload ->
                Toast.makeText(context, eventPayload.message, Toast.LENGTH_SHORT).show()
                if (eventPayload.loadingStatus == LoadingStatus.SUCCESS) mAdapter.removeAt(
                    eventPayload.payload!!
                )
            }
        })

        viewModel.loadingStatus.observe(viewLifecycleOwner, Observer {
            it.getContentIfNotHandled()?.run {
                updateHeaderAndFooter(binding.srlAndRv.refreshLayout, mAdapter, this)
                delayedLoading = false
            }
        })

        mAdapter.setDefaultEmptyView()
        viewModel.feeds.observe(viewLifecycleOwner, Observer {list->
            if (list.isEmpty()) {
                mAdapter.setDiffNewData(null)
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
            mAdapter.setDiffNewData(data)
            Timber.i("${this.javaClass.simpleName} Adapter will have ${list.size} feeds")
        })
    }

    override fun onPause() {
        super.onPause()
        mHandler?.removeCallbacks(mDelayedLoad)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mHandler = null
        _binding = null
        Timber.d("Fragment View Destroyed")
    }

    private class SimpleTextBinder : QuickItemBinder<String>() {
        override fun convert(holder: BaseViewHolder, data: String) {
            holder.setText(R.id.text, data)
        }

        override fun getLayoutId(): Int = R.layout.list_item_simple_text
    }

    inner class FeedAndPostBinder(
        private val sharedViewModel: SharedViewModel,
        private val callerFragment: BaseNavFragment
    ) :
        QuickItemBinder<FeedAndPost>() {
        override fun convert(holder: BaseViewHolder, data: FeedAndPost) {
            holder.convertUserId(data.post!!.userid, "0")
            holder.convertRefId(context, data.post.id)
            holder.convertTimeStamp(data.post.now)
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

            sharedViewModel.setPost(data.feed.postId, data.post!!.fid)
            (context as MainActivity).showComment()
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
                        viewModel.deleteFeed(id, position)
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
                val url = data.post!!.getImgUrl()
                val viewerPopup = ImageViewerPopup(imgUrl = url, fragment = callerFragment)
                viewerPopup.setSingleSrcView(view as ImageView?, url)
                XPopup.Builder(context)
                    .asCustom(viewerPopup)
                    .show()
            }
        }
    }
}


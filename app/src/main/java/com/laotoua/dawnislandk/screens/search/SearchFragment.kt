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

package com.laotoua.dawnislandk.screens.search

import android.os.Bundle
import android.text.style.UnderlineSpan
import android.view.*
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.text.toSpannable
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import com.chad.library.adapter.base.binder.QuickItemBinder
import com.chad.library.adapter.base.util.getItemView
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.google.android.material.card.MaterialCardView
import com.laotoua.dawnislandk.DawnApp
import com.laotoua.dawnislandk.MainNavDirections
import com.laotoua.dawnislandk.R
import com.laotoua.dawnislandk.data.remote.SearchResult
import com.laotoua.dawnislandk.databinding.FragmentSearchBinding
import com.laotoua.dawnislandk.screens.adapters.*
import com.laotoua.dawnislandk.screens.posts.PostCardFactory
import com.laotoua.dawnislandk.screens.util.Layout.toast
import com.laotoua.dawnislandk.screens.util.Layout.updateHeaderAndFooter
import com.laotoua.dawnislandk.screens.widgets.BaseNavFragment
import com.laotoua.dawnislandk.screens.widgets.popups.ImageViewerPopup
import com.laotoua.dawnislandk.util.EventPayload
import com.laotoua.dawnislandk.util.SingleLiveEvent
import com.lxj.xpopup.XPopup
import me.dkzwm.widget.srl.RefreshingListenerAdapter
import timber.log.Timber
import java.util.*


class SearchFragment : BaseNavFragment() {

    private val args: SearchFragmentArgs by navArgs()

    private val viewModel: SearchViewModel by viewModels { viewModelFactory }
    private var binding: FragmentSearchBinding? = null
    private var mAdapter: QuickMultiBinder? = null
    private var pageCounter: TextView? = null

    private var viewCaching = false
    private var refreshing = false
    private var currentPage = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_fragment_search, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        pageCounter = menu.findItem(R.id.pageCounter).actionView.findViewById(R.id.text)
        super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.search -> {
                if (DawnApp.applicationDataStore.firstCookieHash == null) {
                    toast(R.string.need_cookie_to_search)
                    return true
                }

                MaterialDialog(requireContext()).show {
                    title(R.string.search)
                    customView(R.layout.dialog_search, noVerticalPadding = true).apply {
                        findViewById<Button>(R.id.search).setOnClickListener {
                            val query = findViewById<TextView>(R.id.searchInputText).text.toString()
                            if (query.isNotBlank() && query != viewModel.query) {
                                refreshing = true
                                viewModel.search(query)
                                currentPage = 0
                                dismiss()
                            } else {
                                toast(R.string.please_input_valid_text)
                            }
                        }

                        findViewById<Button>(R.id.jumpToPost).setOnClickListener {
                            val threadId = findViewById<TextView>(R.id.searchInputText).text
                                .filter { it.isDigit() }.toString()
                            if (threadId.isNotEmpty()) {
                                dismiss()
                                viewCaching = DawnApp.applicationDataStore.getViewCaching()
                                // Does not have fid here. fid will be generated when data comes back in reply
                                val navAction = MainNavDirections.actionGlobalCommentsFragment(threadId, "")
                                findNavController().navigate(navAction)
                            } else {
                                toast(R.string.please_input_valid_text)
                            }
                        }
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
                addItemBinder(HitBinder().apply {
                    addChildClickViewIds(R.id.attachedImage)
                })

                loadMoreModule.setOnLoadMoreListener {
                    viewModel.getNextPage()
                }
            }
        }
        if (binding != null) {
            Timber.d("Fragment View Reusing!")
        } else {
            Timber.d("Fragment View Created")
            binding = FragmentSearchBinding.inflate(inflater, container, false)
            binding?.srlAndRv?.refreshLayout?.setOnRefreshListener(object :
                RefreshingListenerAdapter() {
                override fun onRefreshing() {
                    binding?.srlAndRv?.refreshLayout?.refreshComplete(true)
                }
            })

            binding?.srlAndRv?.recyclerView?.apply {
                setHasFixedSize(true)
                layoutManager = LinearLayoutManager(context)
                adapter = mAdapter
                addOnScrollListener(object : RecyclerView.OnScrollListener() {
                    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                        if (activity == null || !isAdded || binding == null || mAdapter == null) return
                        val firstVisiblePos =
                            (layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
                        if (firstVisiblePos > 0 && firstVisiblePos < mAdapter!!.data.lastIndex) {
                            if (mAdapter!!.getItem(firstVisiblePos) is SearchResult.Hit) {
                                updateCurrentPage((mAdapter!!.getItem(firstVisiblePos) as SearchResult.Hit).page)
                            }
                        }
                    }
                })
            }
            if (viewModel.query.isBlank()) viewModel.search(args.query)
        }

        viewModel.loadingStatus.observe(viewLifecycleOwner, Observer<SingleLiveEvent<EventPayload<Nothing>>> {
            if (binding == null || mAdapter == null) return@Observer
            it.getContentIfNotHandled()?.run {
                updateHeaderAndFooter(binding!!.srlAndRv.refreshLayout, mAdapter!!, this)
            }
        })


        viewModel.searchResult.observe(viewLifecycleOwner, Observer<List<SearchResult>> { list ->
            if (mAdapter == null) return@Observer
            if (list.isEmpty()) {
                mAdapter!!.setDiffNewData(null)
                hideCurrentPageText()
                return@Observer
            }
            if (currentPage == 0) updateCurrentPage(1)
            val data: MutableList<Any> = ArrayList()
            list.map {
                data.add("搜索: ${list.firstOrNull()?.query} 页数: ${it.page}")
                data.addAll(it.hits)
            }
            if (refreshing) {
                mAdapter!!.setList(data)
                binding?.srlAndRv?.recyclerView?.scrollToPosition(0)
            } else mAdapter!!.setDiffNewData(data)
            refreshing = false
        })

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

    private fun updateCurrentPage(page: Int) {
        if (page != currentPage || pageCounter?.text?.isBlank() == true) {
            pageCounter?.text =
                (page.toString() + " / " + viewModel.maxPage.toString()).toSpannable()
                    .apply { setSpan(UnderlineSpan(), 0, length, 0) }
            currentPage = page
        }
    }

    private fun hideCurrentPageText() {
        pageCounter?.text = ""
    }

    private class SimpleTextBinder : QuickItemBinder<String>() {
        override fun convert(holder: BaseViewHolder, data: String) {
            holder.setText(R.id.text, data)
        }

        override fun getLayoutId(): Int = R.layout.list_item_simple_text
    }

    inner class HitBinder : QuickItemBinder<SearchResult.Hit>() {
        override fun convert(holder: BaseViewHolder, data: SearchResult.Hit) {
            holder.convertUserId(data.userid, "0")
            holder.convertRefId(context, data.id)
            holder.convertTimeStamp(data.now)
            holder.convertImage(data.getImgUrl())
            holder.convertContent(context, data.content)
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
            data: SearchResult.Hit,
            position: Int
        ) {
            viewCaching = DawnApp.applicationDataStore.getViewCaching()
            val navAction = MainNavDirections.actionGlobalCommentsFragment(data.getPostId(), "")
            findNavController().navigate(navAction)
        }

        override fun onChildClick(
            holder: BaseViewHolder,
            view: View,
            data: SearchResult.Hit,
            position: Int
        ) {
            if (view.id == R.id.attachedImage) {
                val viewerPopup = ImageViewerPopup(context)
                viewerPopup.setSingleSrcView(view as ImageView?, data)
                XPopup.Builder(context)
                    .asCustom(viewerPopup)
                    .show()
            }
        }
    }
}
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
import android.widget.Toast
import androidx.core.text.toSpannable
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
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
import com.laotoua.dawnislandk.screens.MainActivity
import com.laotoua.dawnislandk.screens.adapters.*
import com.laotoua.dawnislandk.screens.posts.PostCardFactory
import com.laotoua.dawnislandk.screens.util.Layout.updateHeaderAndFooter
import com.laotoua.dawnislandk.screens.widgets.BaseNavFragment
import com.laotoua.dawnislandk.screens.widgets.popups.ImageViewerPopup
import com.laotoua.dawnislandk.util.EventPayload
import com.laotoua.dawnislandk.util.SingleLiveEvent
import com.lxj.xpopup.XPopup
import timber.log.Timber
import java.util.*


class SearchFragment : BaseNavFragment() {

    companion object {
        fun newInstance() = SearchFragment()
    }

    private val viewModel: SearchViewModel by viewModels { viewModelFactory }
    private var _binding: FragmentSearchBinding? = null
    private val binding: FragmentSearchBinding get() = _binding!!

    private var _mAdapter: QuickMultiBinder? = null
    private val mAdapter: QuickMultiBinder get() = _mAdapter!!

    private var pageCounter: TextView? = null

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
                    Toast.makeText(context, R.string.need_cookie_to_search, Toast.LENGTH_SHORT)
                        .show()
                    return true
                }

                MaterialDialog(requireContext()).show {
                    title(R.string.search)
                    customView(R.layout.dialog_search, noVerticalPadding = true).apply {
                        findViewById<Button>(R.id.search).setOnClickListener {
                            val query = findViewById<TextView>(R.id.searchInputText).text.toString()
                            if (query.isNotBlank() && query != viewModel.query) {
                                viewModel.search(query)
                                currentPage = 0
                                dismiss()
                            } else {
                                Toast.makeText(
                                    context,
                                    R.string.please_input_valid_text,
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }

                        findViewById<Button>(R.id.jumpToPost).setOnClickListener {
                            val threadId = findViewById<TextView>(R.id.searchInputText).text
                                .filter { it.isDigit() }.toString()
                            if (threadId.isNotEmpty()) {
                                // Does not have fid here. fid will be generated when data comes back in reply
                                dismiss()
                                val navAction =
                                    MainNavDirections.actionGlobalCommentsFragment(threadId, "")
                                findNavController().navigate(navAction)
                            } else {
                                Toast.makeText(
                                    context,
                                    R.string.please_input_valid_text,
                                    Toast.LENGTH_SHORT
                                ).show()
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
        if (_mAdapter == null) {
            _mAdapter = QuickMultiBinder(sharedVM).apply {
                addItemBinder(SimpleTextBinder())
                addItemBinder(HitBinder().apply {
                    addChildClickViewIds(R.id.attachedImage)
                })

                loadMoreModule.setOnLoadMoreListener {
                    viewModel.getNextPage()
                }
            }
        }
        if (_binding != null) {
            Timber.d("Fragment View Reusing!")
        } else {
            Timber.d("Fragment View Created")
            _binding = FragmentSearchBinding.inflate(inflater, container, false)
            binding.srlAndRv.recyclerView.apply {
                setHasFixedSize(true)
                layoutManager = LinearLayoutManager(context)
                adapter = mAdapter
                addOnScrollListener(object : RecyclerView.OnScrollListener() {
                    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                        if (_binding == null) return
                        val firstVisiblePos =
                            (layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
                        if (firstVisiblePos > 0 && firstVisiblePos < mAdapter.data.lastIndex) {
                            if (mAdapter.getItem(firstVisiblePos) is String) {
                                updateCurrentPage(
                                    (mAdapter.getItem(firstVisiblePos) as String).substringAfter(
                                        ":"
                                    ).trim().toInt()
                                )
                            }
                        }
                    }
                })
            }

            mAdapter.setDefaultEmptyView()
        }
        return binding.root
    }

    private val searchResultObs = Observer<List<SearchResult>> { list ->
        if (_mAdapter == null) return@Observer
        if (list.isEmpty()) {
            mAdapter.setDiffNewData(null)
            hideCurrentPageText()
            return@Observer
        }
        if (currentPage == 0) updateCurrentPage(1)
        val data: MutableList<Any> = ArrayList()
        data.add("搜索： ${list.firstOrNull()?.query}")
        list.map {
            data.add("结果页数: ${it.page}")
            data.addAll(it.hits)
        }
        mAdapter.setDiffNewData(data)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (!DawnApp.applicationDataStore.getViewCaching()) {
            _mAdapter = null
            _binding = null
        }
        Timber.d("Fragment View Destroyed ${_binding == null}")
    }

    private val loadingStatusObs = Observer<SingleLiveEvent<EventPayload<Nothing>>> {
        if (_binding == null || _mAdapter == null) return@Observer
        it.getContentIfNotHandled()?.run {
            updateHeaderAndFooter(binding.srlAndRv.refreshLayout, mAdapter, this)
        }
    }

    override fun onResume() {
        super.onResume()
        (requireActivity() as MainActivity).setToolbarTitle(R.string.search)
        viewModel.searchResult.observe(viewLifecycleOwner, searchResultObs)
        viewModel.loadingStatus.observe(viewLifecycleOwner, loadingStatusObs)
    }

    override fun onPause() {
        super.onPause()
        viewModel.searchResult.removeObserver(searchResultObs)
        viewModel.loadingStatus.removeObserver(loadingStatusObs)
    }

    private fun updateCurrentPage(page: Int) {
        if (page != currentPage) {
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
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

package com.laotoua.dawnislandk.screens.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.datetime.datePicker
import com.chad.library.adapter.base.binder.QuickItemBinder
import com.chad.library.adapter.base.util.getItemView
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.google.android.material.card.MaterialCardView
import com.laotoua.dawnislandk.R
import com.laotoua.dawnislandk.data.local.entity.Post
import com.laotoua.dawnislandk.databinding.FragmentHistoryBrowsingBinding
import com.laotoua.dawnislandk.screens.MainActivity
import com.laotoua.dawnislandk.screens.SharedViewModel
import com.laotoua.dawnislandk.screens.adapters.*
import com.laotoua.dawnislandk.screens.posts.PostCardFactory
import com.laotoua.dawnislandk.screens.widgets.BaseNavFragment
import com.laotoua.dawnislandk.screens.widgets.popups.ImageViewerPopup
import com.laotoua.dawnislandk.util.ReadableTime
import com.lxj.xpopup.XPopup
import timber.log.Timber
import java.util.*
import kotlin.collections.ArrayList

class BrowsingHistoryFragment : BaseNavFragment() {

    companion object {
        fun newInstance() = BrowsingHistoryFragment()
    }

    private var _binding: FragmentHistoryBrowsingBinding? = null
    private val binding: FragmentHistoryBrowsingBinding get() = _binding!!

    private val viewModel: BrowsingHistoryViewModel by viewModels { viewModelFactory }

    private var endDate = Calendar.getInstance()
    private var startDate = Calendar.getInstance().apply { add(Calendar.WEEK_OF_YEAR, -1) }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentHistoryBrowsingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val mAdapter = QuickMultiBinder(sharedVM).apply {
            addItemBinder(DateStringBinder())
            addItemBinder(PostBinder(sharedVM, this@BrowsingHistoryFragment).apply {
                addChildClickViewIds(R.id.attachedImage)
            })
        }

        binding.recyclerView.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            adapter = mAdapter
        }

        viewModel.browsingHistoryList.observe(viewLifecycleOwner, Observer { list ->
            if (list.isEmpty()) {
                if (!mAdapter.hasEmptyView()) mAdapter.setDefaultEmptyView()
                mAdapter.setDiffNewData(null)
                return@Observer
            }
            var lastDate: Long? = null
            val data: MutableList<Any> = ArrayList()
            list.map {
                if (lastDate == null || it.browsingHistory.date != lastDate) {
                    data.add(
                        ReadableTime.getDateString(
                            it.browsingHistory.date,
                            ReadableTime.DATE_ONLY_FORMAT
                        )
                    )
                }
                if (it.post != null) {
                    data.add(it.post)
                    lastDate = it.browsingHistory.date
                }
            }
            mAdapter.setDiffNewData(data)
            mAdapter.setFooterView(
                layoutInflater.inflate(
                    R.layout.view_no_more_data,
                    binding.recyclerView,
                    false
                )
            )
            Timber.i("${this.javaClass.simpleName} Adapter will have ${list.size} posts")
        })

        binding.startDate.text = ReadableTime.getDateString(startDate.time)
        binding.endDate.text = ReadableTime.getDateString(endDate.time)
        binding.startDate.setOnClickListener {
            MaterialDialog(requireContext()).show {
                datePicker(currentDate = startDate) { _, date ->
                    setStartDate(date)
                }
            }
        }

        binding.endDate.setOnClickListener {
            MaterialDialog(requireContext()).show {
                datePicker(currentDate = endDate) { _, date ->
                    setEndDate(date)
                }
            }
        }

        binding.confirmDate.setOnClickListener {
            if (startDate.before(endDate)) {
                viewModel.searchByDate()
            } else {
                Toast.makeText(context, R.string.data_range_selection_error, Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private fun setStartDate(date: Calendar) {
        startDate = date
        viewModel.setStartDate(date.time)
        binding.startDate.text = ReadableTime.getDateString(date.time)
    }

    private fun setEndDate(date: Calendar) {
        endDate = date
        viewModel.setEndDate(date.time)
        binding.endDate.text = ReadableTime.getDateString(date.time)
    }

    private class DateStringBinder : QuickItemBinder<String>() {
        override fun convert(holder: BaseViewHolder, data: String) {
            holder.setText(R.id.text, data)
        }

        override fun getLayoutId(): Int = R.layout.list_item_simple_text
    }

    private class PostBinder(
        private val sharedViewModel: SharedViewModel,
        private val callerFragment: BrowsingHistoryFragment
    ) :
        QuickItemBinder<Post>() {
        override fun convert(holder: BaseViewHolder, data: Post) {
            holder.convertUserId(data.userid, data.admin)
            holder.convertTitleAndName(data.getSimplifiedTitle(), data.getSimplifiedName())
            holder.convertRefId(context, data.id)
            holder.convertTimeStamp(data.now)
            holder.convertForumAndReplyCount(
                data.replyCount,
                sharedViewModel.getForumDisplayName(data.fid)
            )
            holder.convertSage(data.sage)
            holder.convertImage(data.getImgUrl())
            holder.convertContent(context, data.content)
        }

        override fun getLayoutId(): Int = R.layout.list_item_post

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
            val view = parent.getItemView(getLayoutId()).applyTextSizeAndLetterSpacing()
            PostCardFactory.applySettings(view as MaterialCardView)
            return BaseViewHolder(view)
        }

        override fun onClick(holder: BaseViewHolder, view: View, data: Post, position: Int) {
            sharedViewModel.setPost(data.id, data.fid)
            (context as MainActivity).showComment()
        }

        override fun onChildClick(holder: BaseViewHolder, view: View, data: Post, position: Int) {
            if (view.id == R.id.attachedImage) {
                val url = data.getImgUrl()
                val viewerPopup = ImageViewerPopup(imgUrl = url, fragment = callerFragment)
                viewerPopup.setSingleSrcView(view as ImageView?, url)
                XPopup.Builder(context)
                    .asCustom(viewerPopup)
                    .show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
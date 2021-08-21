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
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chad.library.adapter.base.binder.QuickItemBinder
import com.chad.library.adapter.base.util.getItemView
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.google.android.material.card.MaterialCardView
import com.laotoua.dawnislandk.DawnApp
import com.laotoua.dawnislandk.MainNavDirections
import com.laotoua.dawnislandk.R
import com.laotoua.dawnislandk.data.local.entity.DailyTrend
import com.laotoua.dawnislandk.data.local.entity.Trend
import com.laotoua.dawnislandk.databinding.FragmentSubscriptionTrendBinding
import com.laotoua.dawnislandk.screens.adapters.*
import com.laotoua.dawnislandk.screens.posts.PostCardFactory
import com.laotoua.dawnislandk.screens.util.Layout.updateHeaderAndFooter
import com.laotoua.dawnislandk.screens.widgets.BaseNavFragment
import com.laotoua.dawnislandk.util.*
import me.dkzwm.widget.srl.RefreshingListenerAdapter
import timber.log.Timber
import java.util.*

class TrendsFragment : BaseNavFragment() {

    companion object {
        fun newInstance() = TrendsFragment()
    }

    private var binding: FragmentSubscriptionTrendBinding? = null

    private var mAdapter: QuickMultiBinder? = null

    private var viewCaching = false

    private val viewModel: TrendsViewModel by viewModels { viewModelFactory }

    private val trendsObs = Observer<DataResource<List<DailyTrend>>> {
        if (mAdapter == null || binding == null || sharedVM.currentDomain.value == DawnConstants.TNMBDomain) return@Observer
        updateHeaderAndFooter(
            binding!!.srlAndRv.refreshLayout,
            mAdapter!!,
            EventPayload(it.status, it.message, null)
        )
        if (it.status == LoadingStatus.LOADING) return@Observer
        val list = it.data
        if (list.isNullOrEmpty()) {
            mAdapter?.showNoData()
            return@Observer
        }
        val data: MutableList<Any> = ArrayList()
        list.map { dailyTrend ->
            data.add(ReadableTime.getDateString(dailyTrend.date))
            data.addAll(dailyTrend.trends)
        }
        mAdapter?.setDiffNewData(data)
        mAdapter?.setFooterView(
            layoutInflater.inflate(
                R.layout.view_no_more_data,
                binding!!.srlAndRv.recyclerView,
                false
            )
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        if (mAdapter == null) {
            mAdapter = QuickMultiBinder(sharedVM).apply {
                addItemBinder(TrendBinder().apply {
                    addChildClickViewIds(R.id.attachedImage)
                }, TrendDiffer())
                addItemBinder(DateStringBinder(), DateStringDiffer())
                loadMoreModule.isEnableLoadMore = false
                loadMoreModule.enableLoadMoreEndClick = false
            }
        }
        if (binding != null) {
            Timber.d("Fragment View Reusing!")
        } else {
            Timber.d("Fragment View Created")
            binding = FragmentSubscriptionTrendBinding.inflate(inflater, container, false)

            binding!!.srlAndRv.refreshLayout.apply {
                setOnRefreshListener(object : RefreshingListenerAdapter() {
                    override fun onRefreshing() {
                        refreshTrends()
                    }
                })
            }

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

            binding!!.jump.setOnClickListener {
                val navAction = MainNavDirections.actionGlobalCommentsFragment("15347469", "")
                navAction.targetPage = viewModel.maxPage
                findNavController().navigate(navAction)
            }

        }

        sharedVM.currentDomain.observe(viewLifecycleOwner) {
            if (it == DawnConstants.TNMBDomain) {
                mAdapter?.showNoData()
                mAdapter?.setDiffNewData(ArrayList())
                if (binding != null) mAdapter?.setFooterView(layoutInflater.inflate(R.layout.view_no_more_data, binding!!.srlAndRv.recyclerView, false))

            }
        }

        viewModel.latestTrends.observe(viewLifecycleOwner, trendsObs)
        viewCaching = false
        return binding!!.root
    }

    private fun refreshTrends() {
        viewModel.getLatestTrend()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (!viewCaching) {
            mAdapter = null
            binding = null
        }
        Timber.d("Fragment View Destroyed ${binding == null}")
    }

    inner class TrendBinder : QuickItemBinder<Trend>() {
        override fun convert(holder: BaseViewHolder, data: Trend) {
            holder.setText(R.id.trendRank, data.rank)
                .convertRefId(context, data.id)
                .setText(R.id.trendForum, data.forum)
                .setText(R.id.hits, data.hits)
                .convertContent(context, data.content)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
            val view = parent.getItemView(getLayoutId()).applyTextSizeAndLetterSpacing()
            PostCardFactory.applySettings(view as MaterialCardView)
            return BaseViewHolder(view)
        }

        override fun getLayoutId(): Int = R.layout.list_item_trend


        override fun onClick(holder: BaseViewHolder, view: View, data: Trend, position: Int) {
            viewCaching = DawnApp.applicationDataStore.getViewCaching()
            data.toPost(sharedVM.getForumIdByName(data.forum)).run {
                val navAction = MainNavDirections.actionGlobalCommentsFragment(id, fid)
                findNavController().navigate(navAction)
            }
        }
    }

    inner class TrendDiffer : DiffUtil.ItemCallback<Trend>() {
        override fun areItemsTheSame(oldItem: Trend, newItem: Trend): Boolean {
            return oldItem.id == newItem.id && oldItem.rank == newItem.rank && oldItem.hits == newItem.hits
        }

        override fun areContentsTheSame(oldItem: Trend, newItem: Trend): Boolean {
            return true
        }
    }

    private class DateStringBinder : QuickItemBinder<String>() {
        override fun convert(holder: BaseViewHolder, data: String) {
            holder.setText(R.id.text, data)
        }

        override fun getLayoutId(): Int = R.layout.list_item_simple_text
    }

    private class DateStringDiffer : DiffUtil.ItemCallback<String>() {
        override fun areItemsTheSame(oldItem: String, newItem: String): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: String, newItem: String): Boolean {
            return true
        }
    }
}

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
import androidx.recyclerview.widget.LinearLayoutManager
import com.laotoua.dawnislandk.MainNavDirections
import com.laotoua.dawnislandk.R
import com.laotoua.dawnislandk.data.local.entity.DailyTrend
import com.laotoua.dawnislandk.data.local.entity.Trend
import com.laotoua.dawnislandk.databinding.FragmentSubscriptionTrendBinding
import com.laotoua.dawnislandk.screens.adapters.QuickAdapter
import com.laotoua.dawnislandk.screens.util.Layout.updateHeaderAndFooter
import com.laotoua.dawnislandk.screens.widgets.BaseNavFragment
import com.laotoua.dawnislandk.util.DataResource
import com.laotoua.dawnislandk.util.EventPayload
import me.dkzwm.widget.srl.RefreshingListenerAdapter
import timber.log.Timber

class TrendsFragment : BaseNavFragment() {

    companion object {
        fun newInstance() = TrendsFragment()
    }

    private var _binding: FragmentSubscriptionTrendBinding? = null
    private val binding get() = _binding!!

    private lateinit var mAdapter: QuickAdapter<Trend>

    private val viewModel: TrendsViewModel by viewModels { viewModelFactory }

    private val trendsObs = Observer<DataResource<DailyTrend>> {
        updateHeaderAndFooter(binding.srlAndRv.refreshLayout, mAdapter, EventPayload(it.status, it.message, null))
        val list = it.data?.trends
        if (list.isNullOrEmpty()) {
            if (!mAdapter.hasEmptyView()) mAdapter.setDefaultEmptyView()
            mAdapter.setDiffNewData(null)
            return@Observer
        }
        mAdapter.setList(list.toMutableList())
        mAdapter.setFooterView(
            layoutInflater.inflate(
                R.layout.view_no_more_data,
                binding.srlAndRv.recyclerView,
                false
            )
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentSubscriptionTrendBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mAdapter = QuickAdapter<Trend>(R.layout.list_item_trend, sharedVM).apply {
            loadMoreModule.isEnableLoadMore = false
            setOnItemClickListener { _, _, position ->
                val target = getItem(position)
                getItem(position).toPost(sharedVM.getForumIdByName(target.forum)).run {
                    val navAction =
                        MainNavDirections.actionGlobalCommentsFragment(id, fid)
                    findNavController().navigate(navAction)
                }
            }
        }

        binding.srlAndRv.refreshLayout.apply {
            setOnRefreshListener(object : RefreshingListenerAdapter() {
                override fun onRefreshing() {
                    refreshTrends()
                }
            })
        }

        binding.srlAndRv.recyclerView.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            adapter = mAdapter
        }

        viewModel.latestTrends.observe(viewLifecycleOwner, trendsObs)
    }

    private fun refreshTrends(){
        viewModel.latestTrends.removeObserver(trendsObs)
        viewModel.getLatestTrend()
        viewModel.latestTrends.observe(viewLifecycleOwner, trendsObs)

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        Timber.d("Fragment View Destroyed")
    }
}

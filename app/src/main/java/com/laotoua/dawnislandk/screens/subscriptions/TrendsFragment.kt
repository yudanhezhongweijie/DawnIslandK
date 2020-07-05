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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.laotoua.dawnislandk.R
import com.laotoua.dawnislandk.data.local.entity.Trend
import com.laotoua.dawnislandk.databinding.FragmentSubscriptionTrendBinding
import com.laotoua.dawnislandk.screens.MainActivity
import com.laotoua.dawnislandk.screens.adapters.QuickAdapter
import com.laotoua.dawnislandk.screens.util.Layout.updateHeaderAndFooter
import com.laotoua.dawnislandk.screens.widgets.BaseNavFragment
import me.dkzwm.widget.srl.RefreshingListenerAdapter
import me.dkzwm.widget.srl.config.Constants
import timber.log.Timber

class TrendsFragment : BaseNavFragment() {

    companion object {
        fun newInstance() = TrendsFragment()
    }

    private var _binding: FragmentSubscriptionTrendBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TrendsViewModel by viewModels { viewModelFactory }

    private var mHandler: Handler? = null
    private val mDelayedLoad = Runnable {
        viewModel.getLatestTrend()
    }
    private var delayedLoading = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentSubscriptionTrendBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // initial load
        if (viewModel.trends.value.isNullOrEmpty() && !delayedLoading) {
            binding.srlAndRv.refreshLayout.autoRefresh(
                Constants.ACTION_NOTHING,
                false
            )
            // give sometime to skip load if bypassing this fragment
            mHandler = mHandler ?: Handler()
            delayedLoading = mHandler!!.postDelayed(mDelayedLoad, 500)
        }
        val mAdapter = QuickAdapter<Trend>(R.layout.list_item_trend, sharedVM).apply {
            loadMoreModule.isEnableLoadMore = false
            setOnItemClickListener { _, _, position ->
                val target = getItem(position)
                target.toPost(sharedVM.getForumIdByName(target.forum)).run {
                    sharedVM.setPost(id, fid)
                }
                (requireActivity() as MainActivity).showComment()
            }
        }

        binding.srlAndRv.refreshLayout.apply {
            setOnRefreshListener(object : RefreshingListenerAdapter() {
                override fun onRefreshing() {
                    viewModel.getLatestTrend()
                }
            })
        }

        binding.srlAndRv.recyclerView.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            adapter = mAdapter
        }

        viewModel.loadingStatus.observe(viewLifecycleOwner, Observer {
            it.getContentIfNotHandled()?.run {
                updateHeaderAndFooter(binding.srlAndRv.refreshLayout, mAdapter, this)
                delayedLoading = false
            }
        })

        viewModel.trends.observe(viewLifecycleOwner, Observer { list ->
            if (list.isEmpty()) {
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
}

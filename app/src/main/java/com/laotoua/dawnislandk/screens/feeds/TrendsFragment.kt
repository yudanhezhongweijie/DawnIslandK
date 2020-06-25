package com.laotoua.dawnislandk.screens.feeds

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
import com.laotoua.dawnislandk.databinding.FragmentTrendBinding
import com.laotoua.dawnislandk.screens.MainActivity
import com.laotoua.dawnislandk.screens.adapters.QuickAdapter
import com.laotoua.dawnislandk.screens.util.Layout.updateHeaderAndFooter
import com.laotoua.dawnislandk.screens.widget.BaseNavFragment
import me.dkzwm.widget.srl.RefreshingListenerAdapter
import me.dkzwm.widget.srl.config.Constants
import timber.log.Timber

class TrendsFragment : BaseNavFragment() {

    companion object {
        fun newInstance() = TrendsFragment()
    }

    private var _binding: FragmentTrendBinding? = null
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
        _binding = FragmentTrendBinding.inflate(inflater, container, false)
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
                if (!mAdapter.hasEmptyView()) mAdapter.setEmptyView(R.layout.view_no_data)
                mAdapter.setDiffNewData(null)
                return@Observer
            }
            mAdapter.setDiffNewData(list.toMutableList())
            mAdapter.setFooterView(
                layoutInflater.inflate(
                    R.layout.view_no_more_data,
                    binding.srlAndRv.recyclerView,
                    false
                )
            )
        })

    }

    override fun onResume() {
        super.onResume()
        (parentFragment as FeedPagerFragment).setToolbarClickListener {
            binding.srlAndRv.recyclerView.layoutManager?.scrollToPosition(0)
        }
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

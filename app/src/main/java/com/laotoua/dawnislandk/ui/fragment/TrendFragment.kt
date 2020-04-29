package com.laotoua.dawnislandk.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.laotoua.dawnislandk.R
import com.laotoua.dawnislandk.data.entity.Trend
import com.laotoua.dawnislandk.databinding.TrendFragmentBinding
import com.laotoua.dawnislandk.ui.adapter.QuickAdapter
import com.laotoua.dawnislandk.viewmodel.LoadingStatus
import com.laotoua.dawnislandk.viewmodel.SharedViewModel
import com.laotoua.dawnislandk.viewmodel.TrendViewModel
import me.dkzwm.widget.srl.RefreshingListenerAdapter
import me.dkzwm.widget.srl.config.Constants
import me.dkzwm.widget.srl.extra.header.ClassicHeader
import me.dkzwm.widget.srl.indicator.IIndicator
import timber.log.Timber

class TrendFragment : Fragment() {

    private var _binding: TrendFragmentBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TrendViewModel by viewModels()
    private val sharedVM: SharedViewModel by activityViewModels()
    private val mAdapter = QuickAdapter(R.layout.trend_list_item)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = TrendFragmentBinding.inflate(inflater, container, false)
        binding.trendsView.setHasFixedSize(true)
        binding.trendsView.layoutManager = LinearLayoutManager(context)
        binding.trendsView.adapter = mAdapter

        binding.refreshLayout.setHeaderView(ClassicHeader<IIndicator>(context))
        binding.refreshLayout.setOnRefreshListener(object : RefreshingListenerAdapter() {
            override fun onRefreshing() {
                viewModel.refresh()
                mAdapter.setNewInstance(mutableListOf())
            }
        })
        // initial load
        if (mAdapter.data.size == 0) binding.refreshLayout.autoRefresh(
            Constants.ACTION_NOTIFY,
            false
        )

        viewModel.loadingStatus.observe(viewLifecycleOwner, Observer {
            it.getContentIfNotHandled()?.run {
                when (this.loadingStatus) {
                    LoadingStatus.FAILED -> {
                        binding.refreshLayout.refreshComplete(false)
                        mAdapter.loadMoreModule.loadMoreFail()
                        Toast.makeText(
                            context,
                            it.peekContent().message,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    LoadingStatus.NODATA -> {
                        binding.refreshLayout.refreshComplete()
                        mAdapter.loadMoreModule.loadMoreEnd()
                        Timber.i("No more data...")
                    }
                    LoadingStatus.SUCCESS -> {
                        binding.refreshLayout.refreshComplete()
                        mAdapter.loadMoreModule.loadMoreComplete()
                        Timber.i("Finished loading data...")
                    }
                    LoadingStatus.LOADING -> {
                        // do nothing
                    }

                }
            }
        })

        mAdapter.loadMoreModule.isEnableLoadMore = false

        // item click
        mAdapter.setOnItemClickListener { adapter, _, position ->
            val target = adapter.getItem(position) as Trend
            sharedVM.setThread(target.toThread(sharedVM.getForumIdByName(target.forum)))
            val action =
                PagerFragmentDirections.actionPagerFragmentToReplyFragment()
            findNavController().navigate(action)
        }

        viewModel.trendList.observe(viewLifecycleOwner, Observer { list ->
            mAdapter.setDiffNewData(list.toMutableList())
        })

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

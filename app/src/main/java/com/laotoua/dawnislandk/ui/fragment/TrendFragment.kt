package com.laotoua.dawnislandk.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.laotoua.dawnislandk.R
import com.laotoua.dawnislandk.data.entity.Trend
import com.laotoua.dawnislandk.databinding.FragmentTrendBinding
import com.laotoua.dawnislandk.ui.adapter.QuickAdapter
import com.laotoua.dawnislandk.ui.util.UIUtils.updateHeaderAndFooter
import com.laotoua.dawnislandk.viewmodel.SharedViewModel
import com.laotoua.dawnislandk.viewmodel.TrendViewModel
import me.dkzwm.widget.srl.RefreshingListenerAdapter
import me.dkzwm.widget.srl.config.Constants
import me.dkzwm.widget.srl.extra.header.ClassicHeader
import me.dkzwm.widget.srl.indicator.IIndicator
import timber.log.Timber

class TrendFragment : Fragment() {

    private var _binding: FragmentTrendBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TrendViewModel by viewModels()
    private val sharedVM: SharedViewModel by activityViewModels()
    private val mAdapter = QuickAdapter(R.layout.list_item_trend)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentTrendBinding.inflate(inflater, container, false)

        binding.refreshLayout.apply {
            setHeaderView(ClassicHeader<IIndicator>(context))
            setOnRefreshListener(object : RefreshingListenerAdapter() {
                override fun onRefreshing() {
                    viewModel.refresh()
                    mAdapter.setList(emptyList())
                }
            })
        }

        binding.trendsView.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            adapter = mAdapter
        }


        viewModel.loadingStatus.observe(viewLifecycleOwner, Observer {
            it.getContentIfNotHandled()?.run {
                updateHeaderAndFooter(binding.refreshLayout, mAdapter, this)
            }
        })


        // item click
        mAdapter.apply {
            // initial load
            if (data.size == 0) {
                binding.refreshLayout.autoRefresh(
                    Constants.ACTION_NOTIFY,
                    false
                )
            }

            loadMoreModule.isEnableLoadMore = false

            setOnItemClickListener { adapter, _, position ->
                val target = adapter.getItem(position) as Trend
                sharedVM.setThread(target.toThread(sharedVM.getForumIdByName(target.forum)))
                val action =
                    PagerFragmentDirections.actionPagerFragmentToReplyFragment()
                findNavController().navigate(action)
            }
        }

        viewModel.trendList.observe(viewLifecycleOwner, Observer { list ->
            mAdapter.setDiffNewData(list.toMutableList())
        })

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        Timber.d("Fragment View Destroyed")
    }
}

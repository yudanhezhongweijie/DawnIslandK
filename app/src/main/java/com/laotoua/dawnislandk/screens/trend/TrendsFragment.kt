package com.laotoua.dawnislandk.screens.trend

import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.laotoua.dawnislandk.R
import com.laotoua.dawnislandk.data.local.Trend
import com.laotoua.dawnislandk.databinding.FragmentTrendBinding
import com.laotoua.dawnislandk.screens.MainActivity
import com.laotoua.dawnislandk.screens.PagerFragment
import com.laotoua.dawnislandk.screens.PagerFragmentDirections
import com.laotoua.dawnislandk.screens.SharedViewModel
import com.laotoua.dawnislandk.screens.adapters.QuickAdapter
import com.laotoua.dawnislandk.screens.util.Layout.updateHeaderAndFooter
import dagger.android.support.DaggerFragment
import me.dkzwm.widget.srl.RefreshingListenerAdapter
import me.dkzwm.widget.srl.config.Constants
import timber.log.Timber
import javax.inject.Inject

class TrendsFragment : DaggerFragment() {

    private var _binding: FragmentTrendBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val viewModel: TrendsViewModel by viewModels { viewModelFactory }
    private val sharedVM: SharedViewModel by activityViewModels{ viewModelFactory }

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
            binding.refreshLayout.autoRefresh(
                Constants.ACTION_NOTHING,
                false
            )
            // give sometime to skip load if bypassing this fragment
            mHandler = mHandler ?: Handler()
            delayedLoading = mHandler!!.postDelayed(mDelayedLoad, 500)
        }
        val mAdapter = QuickAdapter<Trend>(R.layout.list_item_trend).apply {
            loadMoreModule.isEnableLoadMore = false
            setOnItemClickListener { _, _, position ->
                val target = getItem(position)
                target.toThread(sharedVM.getForumIdByName(target.forum)).run {
                    sharedVM.setThread(id, fid)
                }
                (requireActivity() as MainActivity).showReply()
            }
        }

        binding.refreshLayout.apply {
            setOnRefreshListener(object : RefreshingListenerAdapter() {
                override fun onRefreshing() {
                    viewModel.getLatestTrend()
                    mAdapter.setList(emptyList())
                }
            })
        }

        binding.recyclerView.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            adapter = mAdapter
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    if (dy > 0) {
                        (parentFragment as PagerFragment).hideMenu()
                    } else if (dy < 0) {
                        (parentFragment as PagerFragment).showMenu()
                    }
                }
            })
        }

        viewModel.loadingStatus.observe(viewLifecycleOwner, Observer {
            it.getContentIfNotHandled()?.run {
                updateHeaderAndFooter(binding.refreshLayout, mAdapter, this)
                delayedLoading = false
            }
        })

        viewModel.trends.observe(viewLifecycleOwner, Observer { list ->
            mAdapter.setDiffNewData(list.toMutableList())
        })

    }

    override fun onResume() {
        super.onResume()
        (parentFragment as PagerFragment).setToolbarClickListener {
            binding.recyclerView.layoutManager?.scrollToPosition(0)
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

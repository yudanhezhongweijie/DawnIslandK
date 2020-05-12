package com.laotoua.dawnislandk.ui.fragment

import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
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
import com.laotoua.dawnislandk.ui.util.ToolBarUtil.immersiveToolbar
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

    private val mHandler = Handler()
    private val mDelayedLoad = Runnable {
        viewModel.refresh()
    }
    private var delayedLoading = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentTrendBinding.inflate(inflater, container, false)

        binding.toolbarLayout.toolbar.apply {
            immersiveToolbar()
            setTitle(R.string.trend)
            setSubtitle(R.string.adnmb)
            val drawerLayout = requireActivity().findViewById<DrawerLayout>(R.id.drawerLayout)
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
            setNavigationIcon(R.drawable.ic_menu_white_24px)
            setNavigationOnClickListener {
                drawerLayout.openDrawer(GravityCompat.START)
            }

            setOnClickListener {
                binding.recyclerView.layoutManager?.scrollToPosition(0)
            }
        }

        binding.refreshLayout.apply {
            setHeaderView(ClassicHeader<IIndicator>(context))
            setOnRefreshListener(object : RefreshingListenerAdapter() {
                override fun onRefreshing() {
                    viewModel.refresh()
                    mAdapter.setList(emptyList())
                }
            })
        }

        binding.recyclerView.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            adapter = mAdapter
        }


        viewModel.loadingStatus.observe(viewLifecycleOwner, Observer {
            it.getContentIfNotHandled()?.run {
                updateHeaderAndFooter(binding.refreshLayout, mAdapter, this)
                delayedLoading = false
            }
        })


        // item click
        mAdapter.apply {
            loadMoreModule.isEnableLoadMore = false
            setOnItemClickListener { adapter, _, position ->
                val target = adapter.getItem(position) as Trend
                sharedVM.setThread(target.toThread(sharedVM.getForumIdByName(target.forum)))
                val action =
                    PagerFragmentDirections.actionPagerFragmentToReplyFragment()
                /**
                 *  add prefix to finNav won't fail in simultaneous clicks
                 */
                this@TrendFragment.findNavController().navigate(action)
            }
        }

        viewModel.trendList.observe(viewLifecycleOwner, Observer { list ->
            mAdapter.setDiffNewData(list.toMutableList())
        })

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        // initial load
        if (mAdapter.data.size == 0 && !delayedLoading) {
            binding.refreshLayout.autoRefresh(
                Constants.ACTION_NOTHING,
                false
            )
            // give sometime to skip load if bypassing this fragment
            delayedLoading = mHandler.postDelayed(mDelayedLoad, 500)
        }
    }

    override fun onPause() {
        super.onPause()
        mHandler.removeCallbacks(mDelayedLoad)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        Timber.d("Fragment View Destroyed")
    }
}

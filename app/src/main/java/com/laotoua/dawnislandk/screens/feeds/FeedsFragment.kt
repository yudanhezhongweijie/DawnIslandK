package com.laotoua.dawnislandk.screens.feeds

import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.afollestad.materialdialogs.MaterialDialog
import com.laotoua.dawnislandk.R
import com.laotoua.dawnislandk.data.local.Thread
import com.laotoua.dawnislandk.databinding.FragmentFeedBinding
import com.laotoua.dawnislandk.screens.PagerFragmentDirections
import com.laotoua.dawnislandk.screens.SharedViewModel
import com.laotoua.dawnislandk.screens.adapters.QuickAdapter
import com.laotoua.dawnislandk.screens.util.Layout.updateHeaderAndFooter
import com.laotoua.dawnislandk.screens.util.ToolBar.immersiveToolbar
import com.laotoua.dawnislandk.screens.widget.popup.ImageLoader
import com.laotoua.dawnislandk.screens.widget.popup.ImageViewerPopup
import com.laotoua.dawnislandk.util.LoadingStatus
import com.lxj.xpopup.XPopup
import dagger.android.support.DaggerFragment
import me.dkzwm.widget.srl.RefreshingListenerAdapter
import me.dkzwm.widget.srl.config.Constants
import me.dkzwm.widget.srl.extra.header.ClassicHeader
import me.dkzwm.widget.srl.indicator.IIndicator
import timber.log.Timber
import javax.inject.Inject


class FeedsFragment : DaggerFragment() {

    private var _binding: FragmentFeedBinding? = null
    private val binding: FragmentFeedBinding get() = _binding!!

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    private val viewModel: FeedsViewModel by viewModels { viewModelFactory }
    private val sharedVM: SharedViewModel by activityViewModels()

    private var mHandler: Handler? = null
    private val mDelayedLoad = Runnable {
        viewModel.getNextPage()
    }
    private var delayedLoading = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentFeedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // initial load
        if (viewModel.feeds.value.isNullOrEmpty() && !delayedLoading) {
            binding.refreshLayout.autoRefresh(
                Constants.ACTION_NOTHING,
                false
            )
            // give sometime to skip load if bypassing this fragment
            mHandler = mHandler ?: Handler()
            delayedLoading = mHandler!!.postDelayed(mDelayedLoad, 500)
        }

        binding.toolbarLayout.toolbar.apply {
            immersiveToolbar()
            setTitle(R.string.my_feed)
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

        val imageLoader = ImageLoader()

        val mAdapter = QuickAdapter(R.layout.list_item_thread).apply {

            /*** connect SharedVm and adapter
             *  may have better way of getting runtime data
             */
            setSharedVM(sharedVM)

            setOnItemClickListener { adapter, _, position ->
                sharedVM.setThread(adapter.getItem(position) as Thread)
                val action =
                    PagerFragmentDirections.actionPagerFragmentToReplyFragment()
                /**
                 *  add prefix to finNav won't fail in simultaneous clicks
                 */
                this@FeedsFragment.findNavController().navigate(action)
            }

            // long click to delete
            setOnItemLongClickListener { adapter, _, position ->
                val id = (adapter.getItem(position) as Thread).id
                MaterialDialog(requireContext()).show {
                    title(text = "删除订阅 $id?")
                    positiveButton(R.string.delete) {
                        viewModel.deleteFeed(id, position)
                    }
                    negativeButton(R.string.cancel)
                }

                true
            }

            addChildClickViewIds(R.id.attachedImage)
            setOnItemChildClickListener { adapter, view, position ->
                if (view.id == R.id.attachedImage) {
                    val url = (adapter.getItem(
                        position
                    ) as Thread).getImgUrl()

                    // TODO support multiple image
                    val viewerPopup =
                        ImageViewerPopup(
                            this@FeedsFragment,
                            requireContext(),
                            url
                        )
                    viewerPopup.setXPopupImageLoader(imageLoader)
                    viewerPopup.setSingleSrcView(view as ImageView?, url)

                    XPopup.Builder(context)
                        .asCustom(viewerPopup)
                        .show()
                }
            }

            // load more
            loadMoreModule.setOnLoadMoreListener {
                viewModel.getNextPage()
            }
        }

        binding.recyclerView.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            adapter = mAdapter
        }

        binding.refreshLayout.apply {
            setHeaderView(ClassicHeader<IIndicator>(context))
            setOnRefreshListener(object : RefreshingListenerAdapter() {
                override fun onRefreshing() {
                    mAdapter.setList(emptyList())
                    viewModel.refresh()
                }
            })
        }

        viewModel.delFeedResponse.observe(viewLifecycleOwner, Observer {
            it.getContentIfNotHandled()?.let { eventPayload ->
                Toast.makeText(context, eventPayload.message, Toast.LENGTH_SHORT).show()
                if (eventPayload.loadingStatus == LoadingStatus.SUCCESS) mAdapter.remove(
                    eventPayload.payload!!
                )
            }
        })

        viewModel.loadingStatus.observe(viewLifecycleOwner, Observer {
            it.getContentIfNotHandled()?.run {
                updateHeaderAndFooter(binding.refreshLayout, mAdapter, this)
                delayedLoading = false
            }
        })

        viewModel.feeds.observe(viewLifecycleOwner, Observer {
            mAdapter.setDiffNewData(it.toMutableList())
            Timber.i("${this.javaClass.simpleName} Adapter will have ${it.size} threads")
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


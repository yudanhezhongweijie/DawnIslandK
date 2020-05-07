package com.laotoua.dawnislandk.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.afollestad.materialdialogs.MaterialDialog
import com.laotoua.dawnislandk.R
import com.laotoua.dawnislandk.data.entity.Thread
import com.laotoua.dawnislandk.data.network.ImageLoader
import com.laotoua.dawnislandk.databinding.FragmentFeedBinding
import com.laotoua.dawnislandk.ui.adapter.QuickAdapter
import com.laotoua.dawnislandk.ui.popup.ImageViewerPopup
import com.laotoua.dawnislandk.ui.util.UIUtils.updateHeaderAndFooter
import com.laotoua.dawnislandk.viewmodel.FeedViewModel
import com.laotoua.dawnislandk.viewmodel.LoadingStatus
import com.laotoua.dawnislandk.viewmodel.SharedViewModel
import com.lxj.xpopup.XPopup
import me.dkzwm.widget.srl.RefreshingListenerAdapter
import me.dkzwm.widget.srl.config.Constants
import me.dkzwm.widget.srl.extra.header.ClassicHeader
import me.dkzwm.widget.srl.indicator.IIndicator
import timber.log.Timber


class FeedFragment : Fragment() {

    private var _binding: FragmentFeedBinding? = null
    private val binding: FragmentFeedBinding get() = _binding!!

    private val viewModel: FeedViewModel by viewModels()
    private val sharedVM: SharedViewModel by activityViewModels()
    private val mAdapter = QuickAdapter(R.layout.list_item_thread)

    private val imageLoader: ImageLoader by lazy { ImageLoader(requireContext()) }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        sharedVM.setFragment(this)
        _binding = FragmentFeedBinding.inflate(inflater, container, false)
        binding.feedsView.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            adapter = mAdapter
        }

        /*** connect SharedVm and adapter
         *  may have better way of getting runtime data
         */
        mAdapter.setSharedVM(sharedVM)

        binding.refreshLayout.apply {
            setHeaderView(ClassicHeader<IIndicator>(context))
            setOnRefreshListener(object : RefreshingListenerAdapter() {
                override fun onRefreshing() {
                    mAdapter.setList(emptyList())
                    viewModel.refresh()
                }
            })
        }

        // item click
        mAdapter.apply {
            // initial load
            if (data.size == 0) binding.refreshLayout.autoRefresh(
                Constants.ACTION_NOTIFY,
                false
            )

            setOnItemClickListener { adapter, _, position ->
                sharedVM.setThread(adapter.getItem(position) as Thread)
                val action =
                    PagerFragmentDirections.actionPagerFragmentToReplyFragment()
                findNavController().navigate(action)
            }

            // long click to delete
            setOnItemLongClickListener { _, _, position ->
                val id = (mAdapter.getItem(position) as Thread).id
                MaterialDialog(requireContext()).show {
                    title(text = "删除订阅 $id?")
                    positiveButton(R.string.delete) {
                        viewModel.deleteFeed(id, position)
                    }
                    negativeButton(R.string.cancel)
                }

                true
            }

            addChildClickViewIds(R.id.threadImage)
            setOnItemChildClickListener { adapter, view, position ->
                if (view.id == R.id.threadImage) {
                    val url = (adapter.getItem(
                        position
                    ) as Thread).getImgUrl()

                    // TODO support multiple image
                    val viewerPopup =
                        ImageViewerPopup(
                            this@FeedFragment,
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
                viewModel.get2PagesFeeds()
            }
        }

        viewModel.delFeedResponse.observe(viewLifecycleOwner, Observer { it ->
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
            }
        })

        viewModel.feeds.observe(viewLifecycleOwner, Observer {
            mAdapter.setDiffNewData(it.toMutableList())
            Timber.i("Adapter will have ${it.size} threads")
        })

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        Timber.d("Fragment View Destroyed")
    }
}


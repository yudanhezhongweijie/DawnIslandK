package com.laotoua.dawnislandk.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.laotoua.dawnislandk.R
import com.laotoua.dawnislandk.databinding.FeedFragmentBinding
import com.laotoua.dawnislandk.entity.Thread
import com.laotoua.dawnislandk.network.ImageLoader
import com.laotoua.dawnislandk.ui.adapter.QuickAdapter
import com.laotoua.dawnislandk.ui.popup.ImageViewerPopup
import com.laotoua.dawnislandk.viewmodel.FeedViewModel
import com.laotoua.dawnislandk.viewmodel.SharedViewModel
import com.lxj.xpopup.XPopup
import timber.log.Timber


class FeedFragment : Fragment() {

    private var _binding: FeedFragmentBinding? = null
    private val binding: FeedFragmentBinding get() = _binding!!

    private val viewModel: FeedViewModel by viewModels()
    private val sharedVM: SharedViewModel by activityViewModels()
    private val mAdapter = QuickAdapter(R.layout.thread_list_item)

    private val imageLoader: ImageLoader by lazy { ImageLoader(requireContext()) }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        sharedVM.setFragment(this)
        _binding = FeedFragmentBinding.inflate(inflater, container, false)
        binding.feedsView.layoutManager = LinearLayoutManager(context)
        binding.feedsView.adapter = mAdapter

        /*** connect SharedVm and adapter
         *  may have better way of getting runtime data
         */
        mAdapter.setSharedVM(sharedVM)

        // item click
        mAdapter.setOnItemClickListener { adapter, _, position ->
            sharedVM.setThread(adapter.getItem(position) as Thread)
            val action =
                PagerFragmentDirections.actionPagerFragmentToReplyFragment()
            findNavController().navigate(action)
            Timber.i("clicked on item")

        }

        mAdapter.addChildClickViewIds(R.id.threadImage)
        mAdapter.setOnItemChildClickListener { adapter, view, position ->
            if (view.id == R.id.threadImage) {

                Timber.i("clicked on image at $position")
                val url = (adapter.getItem(
                    position
                ) as Thread).getImgUrl()

                // TODO support multiple image
                val viewerPopup =
                    ImageViewerPopup(
                        this,
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
        mAdapter.loadMoreModule.setOnLoadMoreListener {
            Timber.i("Fetching new data...")
            viewModel.getFeeds()
        }

        viewModel.loadFail.observe(viewLifecycleOwner, Observer {
            // TODO: can be either out of new Data or api error
            if (it == true) {
                mAdapter.loadMoreModule.loadMoreFail()
                Timber.i("Failed to load new data...")
            }
        })

        viewModel.feeds.observe(viewLifecycleOwner, Observer {
            mAdapter.setDiffNewData(it.toMutableList())
            mAdapter.loadMoreModule.loadMoreComplete()
            Timber.i("New data found. Adapter now have ${mAdapter.data.size} threads")

        })

        return binding.root
    }
}

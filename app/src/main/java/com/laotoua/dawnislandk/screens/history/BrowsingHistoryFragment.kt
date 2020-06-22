package com.laotoua.dawnislandk.screens.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.laotoua.dawnislandk.R
import com.laotoua.dawnislandk.data.local.entity.Post
import com.laotoua.dawnislandk.databinding.FragmentBrowsingHistoryBinding
import com.laotoua.dawnislandk.screens.MainActivity
import com.laotoua.dawnislandk.screens.PagerFragment
import com.laotoua.dawnislandk.screens.SharedViewModel
import com.laotoua.dawnislandk.screens.adapters.QuickAdapter
import com.laotoua.dawnislandk.screens.util.ToolBar.immersiveToolbar
import com.laotoua.dawnislandk.screens.widget.popup.ImageLoader
import com.laotoua.dawnislandk.screens.widget.popup.ImageViewerPopup
import com.lxj.xpopup.XPopup
import dagger.android.support.DaggerFragment
import timber.log.Timber
import javax.inject.Inject

class BrowsingHistoryFragment : DaggerFragment() {

    private var _binding: FragmentBrowsingHistoryBinding? = null
    private val binding: FragmentBrowsingHistoryBinding get() = _binding!!

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val viewModel: BrowsingHistoryViewModel by viewModels { viewModelFactory }
    private val sharedVM: SharedViewModel by activityViewModels{ viewModelFactory }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentBrowsingHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.apply {
            immersiveToolbar()
            setTitle(R.string.browsing_history)
            setSubtitle(R.string.toolbar_subtitle)
            setNavigationIcon(R.drawable.ic_arrow_back_white_24px)
            setNavigationOnClickListener {
                findNavController().popBackStack()
            }
        }

        val imageLoader = ImageLoader()
        val mAdapter = QuickAdapter<Post>(R.layout.list_item_post, sharedVM).apply {
            setOnItemClickListener { _, _, position ->
                getItem(position).run {
                    sharedVM.setPost(id, fid)
                }
                (requireActivity() as MainActivity).showComment()
            }

            addChildClickViewIds(R.id.attachedImage)
            setOnItemChildClickListener { _, view, position ->
                if (view.id == R.id.attachedImage) {
                    val url = getItem(position).getImgUrl()

                    val viewerPopup =
                        ImageViewerPopup(
                            url,
                            fragment = this@BrowsingHistoryFragment
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
//                viewModel.getNextPage()
            }
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

//        binding.refreshLayout.apply {
//            setOnRefreshListener(object : RefreshingListenerAdapter() {
//                override fun onRefreshing() {
//                    viewModel.refresh()
//                }
//            })
//        }


        viewModel.browsingHistoryList.observe(viewLifecycleOwner, Observer {
//            mAdapter.setDiffNewData(it.toMutableList())
            Timber.i("${this.javaClass.simpleName} Adapter will have ${it.size} threads")
        })
    }
}
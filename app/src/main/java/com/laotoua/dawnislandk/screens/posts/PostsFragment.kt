package com.laotoua.dawnislandk.screens.posts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.laotoua.dawnislandk.R
import com.laotoua.dawnislandk.data.local.entity.Post
import com.laotoua.dawnislandk.databinding.FragmentPostBinding
import com.laotoua.dawnislandk.screens.MainActivity
import com.laotoua.dawnislandk.screens.PagerFragment
import com.laotoua.dawnislandk.screens.SharedViewModel
import com.laotoua.dawnislandk.screens.adapters.QuickAdapter
import com.laotoua.dawnislandk.screens.util.Layout.updateHeaderAndFooter
import com.laotoua.dawnislandk.screens.widget.popup.ImageLoader
import com.laotoua.dawnislandk.screens.widget.popup.ImageViewerPopup
import com.lxj.xpopup.XPopup
import dagger.android.support.DaggerFragment
import me.dkzwm.widget.srl.RefreshingListenerAdapter
import me.dkzwm.widget.srl.config.Constants
import timber.log.Timber
import javax.inject.Inject


class PostsFragment : DaggerFragment() {

    private var _binding: FragmentPostBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    private val viewModel: PostsViewModel by viewModels { viewModelFactory }
    private val sharedVM: SharedViewModel by activityViewModels{ viewModelFactory }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentPostBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (parentFragment as PagerFragment).setToolbarClickListener {
            binding.recyclerView.layoutManager?.scrollToPosition(0)
        }

        // initial load
        if (viewModel.posts.value.isNullOrEmpty()) {
            binding.refreshLayout.autoRefresh(
                Constants.ACTION_NOTHING,
                false
            )
        }

        val imageLoader = ImageLoader()

        val mAdapter = QuickAdapter<Post>(R.layout.list_item_post, sharedVM).apply {
            setOnItemClickListener { _, _, position ->
                getItem(position).run {
                    sharedVM.setPost(id,fid)
                }
                (requireActivity() as MainActivity).showComment()
            }

            addChildClickViewIds(R.id.attachedImage)
            setOnItemChildClickListener { _, view, position ->
                if (view.id == R.id.attachedImage) {
                    val url = getItem(position).getImgUrl()

                    // TODO support multiple image
                    val viewerPopup =
                        ImageViewerPopup(
                            imgUrl = url,
                            fragment = this@PostsFragment
                        )
                    viewerPopup.setXPopupImageLoader(imageLoader)
                    viewerPopup.setSingleSrcView(view as ImageView?, url)

                    XPopup.Builder(context)
                        .asCustom(viewerPopup)
                        .show()
                }
            }

            loadMoreModule.setOnLoadMoreListener {
                viewModel.getPosts()
            }
        }

        binding.refreshLayout.apply {
            setOnRefreshListener(object : RefreshingListenerAdapter() {
                override fun onRefreshing() {
                    viewModel.refresh()
                }
            })
        }

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = mAdapter
            setHasFixedSize(true)
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
            }
        })

        viewModel.posts.observe(viewLifecycleOwner, Observer {
            mAdapter.setDiffNewData(it.toMutableList())
            Timber.i("${this.javaClass.simpleName} Adapter will have ${it.size} threads")
        })

        sharedVM.selectedForumId.observe(viewLifecycleOwner, Observer {
            if (viewModel.currentFid != it) mAdapter.setList(emptyList())
            viewModel.setForum(it)
        })

    }

    override fun onResume() {
        super.onResume()
        (parentFragment as PagerFragment).setToolbarClickListener {
            binding.recyclerView.layoutManager?.scrollToPosition(0)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        Timber.d("Fragment View Destroyed")
    }

}


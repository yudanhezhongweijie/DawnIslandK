package com.laotoua.dawnislandk.screens.threads

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils.loadAnimation
import android.widget.ImageView
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.laotoua.dawnislandk.R
import com.laotoua.dawnislandk.data.local.Thread
import com.laotoua.dawnislandk.databinding.FragmentThreadBinding
import com.laotoua.dawnislandk.screens.MainActivity
import com.laotoua.dawnislandk.screens.PagerFragment
import com.laotoua.dawnislandk.screens.SharedViewModel
import com.laotoua.dawnislandk.screens.adapters.QuickAdapter
import com.laotoua.dawnislandk.screens.util.Layout.updateHeaderAndFooter
import com.laotoua.dawnislandk.screens.widget.popup.ImageLoader
import com.laotoua.dawnislandk.screens.widget.popup.ImageViewerPopup
import com.laotoua.dawnislandk.screens.widget.popup.PostPopup
import com.laotoua.dawnislandk.util.lazyOnMainOnly
import com.lxj.xpopup.XPopup
import dagger.android.support.DaggerFragment
import me.dkzwm.widget.srl.RefreshingListenerAdapter
import me.dkzwm.widget.srl.config.Constants
import timber.log.Timber
import javax.inject.Inject


class ThreadsFragment : DaggerFragment() {

    private var _binding: FragmentThreadBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    private val viewModel: ThreadsViewModel by viewModels { viewModelFactory }
    private val sharedVM: SharedViewModel by activityViewModels{ viewModelFactory }

    private var isFabOpen = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentThreadBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (parentFragment as PagerFragment).setToolbarClickListener {
            binding.recyclerView.layoutManager?.scrollToPosition(0)
        }

        // initial load
        if (viewModel.threads.value.isNullOrEmpty()) {
            binding.refreshLayout.autoRefresh(
                Constants.ACTION_NOTHING,
                false
            )
        }

        val imageLoader = ImageLoader()
        val postPopup: PostPopup by lazyOnMainOnly { PostPopup(this, requireContext(), sharedVM) }

        val mAdapter = QuickAdapter<Thread>(R.layout.list_item_thread).apply {
            /*** connect SharedVm and adapter
             *  may have better way of getting runtime data
             */
            setSharedVM(sharedVM)

            setOnItemClickListener { _, _, position ->
                hideMenu()
                sharedVM.setThread(getItem(position))
                (requireActivity() as MainActivity).showReply()
            }

            addChildClickViewIds(R.id.attachedImage)
            setOnItemChildClickListener { _, view, position ->
                if (view.id == R.id.attachedImage) {
                    hideMenu()
                    val url = getItem(position).getImgUrl()

                    // TODO support multiple image
                    val viewerPopup =
                        ImageViewerPopup(
                            this@ThreadsFragment,
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

            loadMoreModule.setOnLoadMoreListener {
                viewModel.getThreads()
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
                        hideMenu()
                        binding.fabMenu.hide()
                        binding.fabMenu.isClickable = false
                    } else if (dy < 0) {
                        binding.fabMenu.show()
                        binding.fabMenu.isClickable = true
                    }
                }
            })
        }

        viewModel.loadingStatus.observe(viewLifecycleOwner, Observer {
            it.getContentIfNotHandled()?.run {
                updateHeaderAndFooter(binding.refreshLayout, mAdapter, this)
            }
        })

        viewModel.threads.observe(viewLifecycleOwner, Observer {
            mAdapter.setDiffNewData(it.toMutableList())
            Timber.i("${this.javaClass.simpleName} Adapter will have ${it.size} threads")
        })

        sharedVM.selectedForumId.observe(viewLifecycleOwner, Observer {
            if (viewModel.currentFid != it) mAdapter.setList(emptyList())
            viewModel.setForum(it)
            hideMenu()
        })

        binding.fabMenu.setOnClickListener {
            toggleMenu()
        }

        binding.post.setOnClickListener {
            hideMenu()
            postPopup.setupAndShow(
                sharedVM.selectedForumId.value,
                true,
                sharedVM.getForumNameMapping()
            )
        }
    }

    override fun onResume() {
        super.onResume()
        binding.fabMenu.show()
        (parentFragment as PagerFragment).setToolbarClickListener {
            binding.recyclerView.layoutManager?.scrollToPosition(0)
        }
    }

    override fun onPause() {
        super.onPause()
        hideMenu()
        binding.fabMenu.hide()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        Timber.d("Fragment View Destroyed")
    }

    private fun hideMenu() {
        val rotateBackward = loadAnimation(
            requireContext(),
            R.anim.rotate_backward
        )
        binding.fabMenu.startAnimation(rotateBackward)
        binding.post.hide()
        isFabOpen = false
    }

    private fun showMenu() {
        val rotateForward = loadAnimation(
            requireContext(),
            R.anim.rotate_forward
        )
        binding.fabMenu.startAnimation(rotateForward)

        binding.post.show()
        isFabOpen = true
    }

    private fun toggleMenu() {
        if (isFabOpen) {
            hideMenu()
        } else {
            showMenu()
        }
    }

}


package com.laotoua.dawnislandk.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils.loadAnimation
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.laotoua.dawnislandk.R
import com.laotoua.dawnislandk.data.entity.Thread
import com.laotoua.dawnislandk.data.network.ImageLoader
import com.laotoua.dawnislandk.databinding.ThreadFragmentBinding
import com.laotoua.dawnislandk.ui.adapter.QuickAdapter
import com.laotoua.dawnislandk.ui.popup.ImageViewerPopup
import com.laotoua.dawnislandk.ui.popup.PostPopup
import com.laotoua.dawnislandk.ui.util.UIUtils.updateHeaderAndFooter
import com.laotoua.dawnislandk.viewmodel.SharedViewModel
import com.laotoua.dawnislandk.viewmodel.ThreadViewModel
import com.lxj.xpopup.XPopup
import me.dkzwm.widget.srl.RefreshingListenerAdapter
import me.dkzwm.widget.srl.config.Constants
import me.dkzwm.widget.srl.extra.header.ClassicHeader
import me.dkzwm.widget.srl.indicator.IIndicator
import timber.log.Timber


class ThreadFragment : Fragment() {

    private var _binding: ThreadFragmentBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ThreadViewModel by viewModels()
    private val sharedVM: SharedViewModel by activityViewModels()
    private val mAdapter =
        QuickAdapter(R.layout.thread_list_item)

    private val postPopup: PostPopup by lazy { PostPopup(this, requireContext()) }

    private var isFabOpen = false

    private val imageLoader: ImageLoader by lazy { ImageLoader(requireContext()) }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        sharedVM.setFragment(this)
        _binding = ThreadFragmentBinding.inflate(inflater, container, false)

        binding.refreshLayout.apply {
            setHeaderView(ClassicHeader<IIndicator>(context))
            setOnRefreshListener(object : RefreshingListenerAdapter() {
                override fun onRefreshing() {
                    mAdapter.setList(emptyList())
                    viewModel.refresh()
                }
            })
        }

        binding.threadsView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = mAdapter
            setHasFixedSize(true)
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    if (dy > 0) {
                        hideMenu()
                        binding.fabMenu.hide()
                    } else if (dy < 0) binding.fabMenu.show()
                }
            })
        }

        /*** connect SharedVm and adapter
         *  may have better way of getting runtime data
         */
        mAdapter.setSharedVM(sharedVM)

        mAdapter.apply {
            // initial load
            if (data.size == 0) binding.refreshLayout.autoRefresh(Constants.ACTION_NOTHING, false)

            setOnItemClickListener { adapter, _, position ->
                hideMenu()
                sharedVM.setThread(adapter.getItem(position) as Thread)
                val action =
                    PagerFragmentDirections.actionPagerFragmentToReplyFragment()
                findNavController().navigate(action)
            }

            addChildClickViewIds(R.id.threadImage)
            setOnItemChildClickListener { adapter, view, position ->
                if (view.id == R.id.threadImage) {
                    hideMenu()
                    val url = (adapter.getItem(
                        position
                    ) as Thread).getImgUrl()

                    // TODO support multiple image
                    val viewerPopup =
                        ImageViewerPopup(
                            this@ThreadFragment,
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
                Timber.i("Fetching new data...")
                viewModel.getThreads()
            }

        }

        viewModel.loadingStatus.observe(viewLifecycleOwner, Observer {
            it.getContentIfNotHandled()?.run {
                updateHeaderAndFooter(binding.refreshLayout, mAdapter, this)
            }
        })

        viewModel.thread.observe(viewLifecycleOwner, Observer {
            mAdapter.setDiffNewData(it.toMutableList())
            Timber.i("Adapter will have ${it.size} threads")
        })

        sharedVM.selectedForum.observe(viewLifecycleOwner, Observer {
            if (viewModel.currentForum == null) {
                viewModel.setForum(it)
            } else if (viewModel.currentForum != null && viewModel.currentForum!!.id != it.id) {
                Timber.i("Forum has changed to ${it.name}. Cleaning old adapter data...")
                mAdapter.setList(emptyList())
                viewModel.setForum(it)
                hideMenu()
            }
        })

        binding.fabMenu.setOnClickListener {
            toggleMenu()
        }

        binding.setting.setOnClickListener {
            hideMenu()
            val action =
                PagerFragmentDirections.actionPagerFragmentToSettingsFragment()
            findNavController().navigate(action)
        }

        binding.post.setOnClickListener {
            hideMenu()
            PostPopup.show(
                this,
                postPopup,
                sharedVM.selectedForum.value!!.id,
                true,
                sharedVM.getForumNameMapping()
            )
        }

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        binding.fabMenu.show()
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
        binding.setting.hide()
        binding.post.hide()
        isFabOpen = false
    }

    private fun showMenu() {
        val rotateForward = loadAnimation(
            requireContext(),
            R.anim.rotate_forward
        )
        binding.fabMenu.startAnimation(rotateForward)

        binding.setting.show()
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

package com.laotoua.dawnislandk.screens.threads

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils.loadAnimation
import android.widget.ImageView
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.laotoua.dawnislandk.R
import com.laotoua.dawnislandk.data.local.Thread
import com.laotoua.dawnislandk.databinding.FragmentThreadBinding
import com.laotoua.dawnislandk.screens.PagerFragmentDirections
import com.laotoua.dawnislandk.screens.SharedViewModel
import com.laotoua.dawnislandk.screens.adapters.QuickAdapter
import com.laotoua.dawnislandk.screens.util.Layout.updateHeaderAndFooter
import com.laotoua.dawnislandk.screens.util.ToolBar.immersiveToolbar
import com.laotoua.dawnislandk.screens.widget.popup.ImageLoader
import com.laotoua.dawnislandk.screens.widget.popup.ImageViewerPopup
import com.laotoua.dawnislandk.screens.widget.popup.PostPopup
import com.laotoua.dawnislandk.util.lazyOnMainOnly
import com.lxj.xpopup.XPopup
import dagger.android.support.DaggerFragment
import me.dkzwm.widget.srl.RefreshingListenerAdapter
import me.dkzwm.widget.srl.config.Constants
import me.dkzwm.widget.srl.extra.header.ClassicHeader
import me.dkzwm.widget.srl.indicator.IIndicator
import timber.log.Timber
import javax.inject.Inject


class ThreadsFragment : DaggerFragment() {

    private var _binding: FragmentThreadBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    private val viewModel: ThreadsViewModel by viewModels { viewModelFactory }
    private val sharedVM: SharedViewModel by activityViewModels()

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
        binding.toolbarLayout.toolbar.apply {
            immersiveToolbar()
            updateTitle()
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

        // initial load
        if (viewModel.threads.value.isNullOrEmpty()) {
            binding.refreshLayout.autoRefresh(
                Constants.ACTION_NOTHING,
                false
            )
        }

        val imageLoader = ImageLoader()
        val postPopup: PostPopup by lazyOnMainOnly { PostPopup(this, requireContext()) }

        val mAdapter = QuickAdapter(R.layout.list_item_thread).apply {
            /*** connect SharedVm and adapter
             *  may have better way of getting runtime data
             */
            setSharedVM(sharedVM)

            setOnItemClickListener { adapter, _, position ->
                hideMenu()
                sharedVM.setThread(adapter.getItem(position) as Thread)
                val action =
                    PagerFragmentDirections.actionPagerFragmentToReplyFragment()
                /**
                 *  add prefix to finNav won't fail in simultaneous clicks
                 */
                this@ThreadsFragment.findNavController().navigate(action)
            }

            addChildClickViewIds(R.id.attachedImage)
            setOnItemChildClickListener { adapter, view, position ->
                if (view.id == R.id.attachedImage) {
                    hideMenu()
                    val url = (adapter.getItem(
                        position
                    ) as Thread).getImgUrl()

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
                Timber.i("Fetching new data...")
                viewModel.getThreads()

            }
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

        sharedVM.selectedForum.observe(viewLifecycleOwner, Observer {
            if (viewModel.currentForum == null) {
                viewModel.setForum(it)
            } else if (viewModel.currentForum != null && viewModel.currentForum!!.id != it.id) {
                Timber.i("Forum has changed to ${it.name}. Cleaning old adapter data...")
                mAdapter.setList(emptyList())
                viewModel.setForum(it)
                hideMenu()

                updateTitle()
            }
        })

        binding.fabMenu.setOnClickListener {
            toggleMenu()
        }

        binding.setting.setOnClickListener {
            hideMenu()
            /**
             * navigation during scroll will crash the app
             */
            binding.recyclerView.stopScroll()
            val action =
                PagerFragmentDirections.actionPagerFragmentToSettingsFragment()
            findNavController().navigate(action)
        }

        binding.post.setOnClickListener {
            hideMenu()
            PostPopup.show(
                this,
                postPopup,
                sharedVM.selectedForum.value?.id,
                true,
                sharedVM.getForumNameMapping()
            )
        }
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

    private fun updateTitle() {
        binding.toolbarLayout.toolbar.title = "A岛 • ${viewModel.currentForum?.name ?: "时间线"}"
    }
}


package com.laotoua.dawnislandk.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils.loadAnimation
import android.widget.ImageView
import android.widget.Toast
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
import com.laotoua.dawnislandk.viewmodel.LoadingStatus
import com.laotoua.dawnislandk.viewmodel.SharedViewModel
import com.laotoua.dawnislandk.viewmodel.ThreadViewModel
import com.lxj.xpopup.XPopup
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

        binding.threadsView.layoutManager = LinearLayoutManager(context)
        binding.threadsView.adapter = mAdapter

        /*** connect SharedVm and adapter
         *  may have better way of getting runtime data
         */
        mAdapter.setSharedVM(sharedVM)

        // item click
        mAdapter.setOnItemClickListener { adapter, _, position ->
            hideMenu()
            sharedVM.setThread(adapter.getItem(position) as Thread)
            val action =
                PagerFragmentDirections.actionPagerFragmentToReplyFragment()
            findNavController().navigate(action)

        }


        // image
        mAdapter.addChildClickViewIds(R.id.threadImage)
        mAdapter.setOnItemChildClickListener { adapter, view, position ->
            if (view.id == R.id.threadImage) {

                hideMenu()

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
            viewModel.getThreads()
        }

        viewModel.loadingStatus.observe(viewLifecycleOwner, Observer {
            if (it.getContentIfNotHandled()?.loadingStatus == LoadingStatus.FAILED) {
                Toast.makeText(
                    context,
                    it.peekContent().message,
                    Toast.LENGTH_LONG
                ).show()
            }
        })

        viewModel.thread.observe(viewLifecycleOwner, Observer {
            mAdapter.setDiffNewData(it.toMutableList())
            mAdapter.loadMoreModule.loadMoreComplete()
            Timber.i("New data found or new observer added. Adapter now have ${mAdapter.data.size} threads")

        })

        sharedVM.selectedForum.observe(viewLifecycleOwner, Observer {
            if (viewModel.currentForum == null) {
                viewModel.setForum(it)
            } else if (viewModel.currentForum != null && viewModel.currentForum!!.id != it.id) {
                Timber.i("Forum has changed to ${it.name}. Cleaning old adapter data...")
                mAdapter.setList(ArrayList())
                viewModel.setForum(it)
                hideMenu()
            }
        })


        binding.threadsView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy > 0) {
                    hideMenu()
                    binding.fabMenu.hide()
                } else if (dy < 0) binding.fabMenu.show()
            }
        })

        binding.fabMenu.setOnClickListener {
            toggleMenu()
        }

        binding.setting.setOnClickListener {
            Timber.i("clicked on setting")
            hideMenu()
            val action =
                PagerFragmentDirections.actionPagerFragmentToSettingsFragment()
            findNavController().navigate(action)
        }

        binding.post.setOnClickListener {
            Timber.i("Clicked on post")
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.i("Thread Fragment destroyed!!!")
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

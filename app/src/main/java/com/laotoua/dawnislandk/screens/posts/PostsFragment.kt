package com.laotoua.dawnislandk.screens.posts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import com.laotoua.dawnislandk.DawnApp
import com.laotoua.dawnislandk.R
import com.laotoua.dawnislandk.data.local.entity.Post
import com.laotoua.dawnislandk.databinding.FragmentPostBinding
import com.laotoua.dawnislandk.screens.MainActivity
import com.laotoua.dawnislandk.screens.adapters.QuickAdapter
import com.laotoua.dawnislandk.screens.util.Layout.updateHeaderAndFooter
import com.laotoua.dawnislandk.screens.util.ToolBar.immersiveToolbar
import com.laotoua.dawnislandk.screens.widget.BaseNavFragment
import com.laotoua.dawnislandk.screens.widget.popup.ImageViewerPopup
import com.laotoua.dawnislandk.screens.widget.popup.PostPopup
import com.laotoua.dawnislandk.util.lazyOnMainOnly
import com.lxj.xpopup.XPopup
import me.dkzwm.widget.srl.RefreshingListenerAdapter
import me.dkzwm.widget.srl.config.Constants
import timber.log.Timber


class PostsFragment : BaseNavFragment() {

    private var _binding: FragmentPostBinding? = null
    private val binding get() = _binding!!
    private val viewModel: PostsViewModel by viewModels { viewModelFactory }
    private var isFabOpen = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentPostBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.apply {
            immersiveToolbar()
            setSubtitle(R.string.toolbar_subtitle)
            setOnClickListener { binding.srlAndRv.recyclerView.layoutManager?.scrollToPosition(0) }
        }
        // initial load
        if (viewModel.posts.value.isNullOrEmpty()) {
            binding.srlAndRv.refreshLayout.autoRefresh(
                Constants.ACTION_NOTHING,
                false
            )
        }

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
                            imgUrl = url,
                            fragment = this@PostsFragment
                        )
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

        binding.srlAndRv.refreshLayout.apply {
            setOnRefreshListener(object : RefreshingListenerAdapter() {
                override fun onRefreshing() {
                    viewModel.refresh()
                }
            })
        }

        binding.srlAndRv.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = mAdapter
            setHasFixedSize(true)
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    if (dy > 0) {
                        hideFabMenu()
                        binding.fabMenu.hide()
                    } else if (dy < 0) {
                        binding.fabMenu.show()
                    }
                }
            })
        }

        binding.fabMenu.setOnClickListener {
            toggleFabMenu()
        }

        binding.forumRule.setOnClickListener {
            MaterialDialog(requireContext()).show {
                cornerRadius(res = R.dimen.dp_10)
                val forumId = sharedVM.selectedForumId.value!!
                val biId = if (forumId.toInt() > 0) forumId.toInt() else 1
                val resourceId: Int = context.resources.getIdentifier(
                    "bi_$biId", "drawable",
                    context.packageName
                )
                icon(resourceId)
                title(text = sharedVM.getForumDisplayName(forumId))
                message(text = sharedVM.getForumMsg(forumId)) { html() }
                positiveButton(R.string.acknowledge)
            }
        }

        val postPopup: PostPopup by lazyOnMainOnly { PostPopup(this, requireContext(), sharedVM) }
        binding.post.setOnClickListener {
            hideFabMenu()
            postPopup.setupAndShow(
                sharedVM.selectedForumId.value,
                sharedVM.selectedForumId.value!!,
                true
            )
        }

        binding.search.setOnClickListener {
            hideFabMenu()
            MaterialDialog(requireContext()).show {
                cornerRadius(res = R.dimen.dp_10)
                title(R.string.search)
                customView(R.layout.dialog_search, noVerticalPadding = true).apply {
                    findViewById<Button>(R.id.search).setOnClickListener {
                        Toast.makeText(context, "还没做。。。", Toast.LENGTH_SHORT).show()
                    }

                    findViewById<Button>(R.id.jumpToPost).setOnClickListener {
                        val threadId = findViewById<TextView>(R.id.searchInputText).text
                            .filter { it.isDigit() }.toString()
                        if (threadId.isNotEmpty()) {
                            // Does not have fid here. fid will be generated when data comes back in reply
                            sharedVM.setPost(threadId, "")
                            dismiss()
                            (requireActivity() as MainActivity).showComment()
                        } else {
                            Toast.makeText(
                                context,
                                R.string.please_input_valid_text,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
        }

        binding.announcement.setOnClickListener {
            hideFabMenu()
            DawnApp.applicationDataStore.nmbNotice?.let { notice ->
                MaterialDialog(requireContext()).show {
                    cornerRadius(res = R.dimen.dp_10)
                    title(res = R.string.announcement)
                    message(text = notice.content) { html() }
                    positiveButton(R.string.close)
                }
            }
        }

        viewModel.loadingStatus.observe(viewLifecycleOwner, Observer {
            it.getContentIfNotHandled()?.run {
                updateHeaderAndFooter(binding.srlAndRv.refreshLayout, mAdapter, this)
            }
        })

        viewModel.posts.observe(viewLifecycleOwner, Observer {
            if (it.isEmpty()) {
                if (!mAdapter.hasEmptyView()) mAdapter.setEmptyView(R.layout.view_no_data)
                mAdapter.setDiffNewData(null)
                return@Observer
            }
            mAdapter.setDiffNewData(it.toMutableList())
            Timber.i("${this.javaClass.simpleName} Adapter will have ${it.size} threads")
        })

        sharedVM.selectedForumId.observe(viewLifecycleOwner, Observer {
            if (viewModel.currentFid != it) mAdapter.setList(emptyList())
            viewModel.setForum(it)
            binding.toolbar.title = sharedVM.getToolbarTitle()
        })
    }


    private fun hideFabMenu() {
        val rotateBackward = AnimationUtils.loadAnimation(
            requireContext(),
            R.anim.rotate_backward
        )
        binding.fabMenu.startAnimation(rotateBackward)
        binding.announcement.hide()
        binding.search.hide()
        binding.post.hide()
        isFabOpen = false
    }

    private fun showFabMenu() {
        val rotateForward = AnimationUtils.loadAnimation(
            requireContext(),
            R.anim.rotate_forward
        )
        binding.fabMenu.startAnimation(rotateForward)
        binding.announcement.show()
        binding.search.show()
        binding.post.show()
        isFabOpen = true
    }

    private fun toggleFabMenu() {
        if (isFabOpen) {
            hideFabMenu()
        } else {
            showFabMenu()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        Timber.d("Fragment View Destroyed")
    }
}


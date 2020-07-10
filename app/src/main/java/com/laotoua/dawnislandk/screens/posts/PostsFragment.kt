/*
 *  Copyright 2020 Fishballzzz
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package com.laotoua.dawnislandk.screens.posts

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.listItems
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import com.laotoua.dawnislandk.DawnApp
import com.laotoua.dawnislandk.MainNavDirections
import com.laotoua.dawnislandk.R
import com.laotoua.dawnislandk.data.local.entity.Post
import com.laotoua.dawnislandk.databinding.FragmentPostBinding
import com.laotoua.dawnislandk.screens.MainActivity
import com.laotoua.dawnislandk.screens.adapters.QuickAdapter
import com.laotoua.dawnislandk.screens.util.Layout.updateHeaderAndFooter
import com.laotoua.dawnislandk.screens.widgets.BaseNavFragment
import com.laotoua.dawnislandk.screens.widgets.popups.ImageViewerPopup
import com.laotoua.dawnislandk.screens.widgets.popups.PostPopup
import com.laotoua.dawnislandk.util.DawnConstants
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_fragment_post, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.forumRule -> {
                if (sharedVM.selectedForumId.value == null) {
                    Toast.makeText(
                        requireContext(),
                        R.string.please_try_again_later,
                        Toast.LENGTH_SHORT
                    ).show()
                    return true
                }
                MaterialDialog(requireContext()).show {
                    val forumId = sharedVM.selectedForumId.value!!
                    val biId = if (forumId.toInt() > 0) forumId.toInt() else 1
                    val resourceId: Int = context.resources.getIdentifier(
                        "bi_$biId", "drawable",
                        context.packageName
                    )
                    icon(resourceId)
                    title(text = sharedVM.getForumDisplayName(forumId))
                    message(text = sharedVM.getForumMsg(forumId)) {
                        html { link ->
                            val uri = if (link.startsWith("/")) {
                                DawnConstants.nmbHost + link
                            } else link
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
                            if (intent.resolveActivity(requireActivity().packageManager) != null) {
                                startActivity(intent)
                            }
                        }
                    }
                    positiveButton(R.string.acknowledge)
                }
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // initial load
        if (viewModel.posts.value.isNullOrEmpty()) {
            binding.srlAndRv.refreshLayout.autoRefresh(
                Constants.ACTION_NOTHING,
                false
            )
        }

        binding.flingInterceptor.bindListener {
            (activity as MainActivity).showDrawer()
        }

        val postPopup: PostPopup by lazyOnMainOnly { PostPopup(this, requireContext(), sharedVM) }
        val mAdapter = QuickAdapter<Post>(R.layout.list_item_post, sharedVM).apply {
            setOnItemClickListener { _, _, position ->
                getItem(position).run {
                    val navAction =
                        MainNavDirections.actionGlobalCommentsFragment(id, fid)
                    findNavController().navigate(navAction)
                }
            }
            setOnItemLongClickListener { _, _, position ->
                MaterialDialog(requireContext()).show {
                    title(R.string.post_options)
                    listItems(R.array.post_options) { _, index, _ ->
                        if (index == 0) {
                            MaterialDialog(requireContext()).show {
                                title(R.string.report_reasons)
                                listItemsSingleChoice(res = R.array.report_reasons) { _, _, text ->
                                    postPopup.setupAndShow(
                                        "18",//值班室
                                        "18",
                                        newPost = true,
                                        quote = "\n>>No.${getItem(position).id}\n${context.getString(
                                            R.string.report_reasons
                                        )}: $text"
                                    )
                                }
                                cancelOnTouchOutside(false)
                            }
                        }
                    }
                }
                true
            }

            addChildClickViewIds(R.id.attachedImage)
            setOnItemChildClickListener { _, view, position ->
                if (view.id == R.id.attachedImage) {
                    val url = getItem(position).getImgUrl()
                    val viewerPopup = ImageViewerPopup(url, requireContext())
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
                        if (_binding == null) return
                        hideFabMenu()
                        binding.fabMenu.hide()
                        binding.fabMenu.isClickable = false
                    } else if (dy < 0) {
                        if (_binding == null) return
                        binding.fabMenu.show()
                        binding.fabMenu.isClickable = true
                    }
                }
            })
        }

            binding.fabMenu.setOnClickListener {
                toggleFabMenu()
            }

            binding.post.setOnClickListener {
                if (sharedVM.selectedForumId.value == null) {
                    Toast.makeText(
                        requireContext(),
                        R.string.please_try_again_later,
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }
                hideFabMenu()
                postPopup.setupAndShow(
                    sharedVM.selectedForumId.value,
                    sharedVM.selectedForumId.value!!,
                    true
                )
            }

            binding.announcement.setOnClickListener {
                hideFabMenu()
                DawnApp.applicationDataStore.nmbNotice?.let { notice ->
                    MaterialDialog(requireContext()).show {
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
                if (!mAdapter.hasEmptyView()) mAdapter.setDefaultEmptyView()
                mAdapter.setDiffNewData(null)
                return@Observer
        }
            // adds title when navigate from website url
            if (mAdapter.data.isNullOrEmpty() && (requireActivity() as MainActivity).supportActionBar?.title.isNullOrBlank()) {
                (requireActivity() as MainActivity).setToolbarTitle(sharedVM.getForumDisplayName(it.first().fid))
    }
            mAdapter.setDiffNewData(it.toMutableList())
            Timber.i("${this.javaClass.simpleName} Adapter will have ${it.size} threads")
        })

        sharedVM.selectedForumId.observe(viewLifecycleOwner, Observer {
            if (viewModel.currentFid != it) mAdapter.setList(emptyList())
            viewModel.setForum(it)
            (requireActivity() as MainActivity).setToolbarTitle(sharedVM.getToolbarTitle())
        })
    }

    private fun hideFabMenu() {
        val rotateBackward = AnimationUtils.loadAnimation(
            requireContext(),
            R.anim.rotate_backward
        )
        binding.fabMenu.startAnimation(rotateBackward)
        binding.announcement.hide()
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


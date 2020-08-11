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
import android.widget.*
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.list.listItems
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import com.laotoua.dawnislandk.DawnApp
import com.laotoua.dawnislandk.MainNavDirections
import com.laotoua.dawnislandk.R
import com.laotoua.dawnislandk.data.local.entity.Notification
import com.laotoua.dawnislandk.data.local.entity.Post
import com.laotoua.dawnislandk.databinding.FragmentPostBinding
import com.laotoua.dawnislandk.screens.MainActivity
import com.laotoua.dawnislandk.screens.adapters.QuickAdapter
import com.laotoua.dawnislandk.screens.util.Layout.toast
import com.laotoua.dawnislandk.screens.util.Layout.updateHeaderAndFooter
import com.laotoua.dawnislandk.screens.widgets.BaseNavFragment
import com.laotoua.dawnislandk.screens.widgets.popups.ImageViewerPopup
import com.laotoua.dawnislandk.screens.widgets.popups.PostPopup
import com.laotoua.dawnislandk.util.DawnConstants
import com.laotoua.dawnislandk.util.EventPayload
import com.laotoua.dawnislandk.util.SingleLiveEvent
import com.laotoua.dawnislandk.util.lazyOnMainOnly
import com.lxj.xpopup.XPopup
import me.dkzwm.widget.srl.RefreshingListenerAdapter
import timber.log.Timber


class PostsFragment : BaseNavFragment() {

    private var binding: FragmentPostBinding? = null
    private var mAdapter: QuickAdapter<Post>? = null
    private var redCircle: FrameLayout? = null
    private var countTextView: TextView? = null
    private val viewModel: PostsViewModel by viewModels { viewModelFactory }
    private val postPopup: PostPopup by lazyOnMainOnly { PostPopup(requireActivity(), sharedVM) }
    private var isFabOpen = false
    private var viewCaching = false
    private var refreshing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_fragment_post, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        val rootView = menu.findItem(R.id.feedNotification)
        rootView.actionView.apply {
            redCircle = findViewById(R.id.viewAlertRedCircle)
            countTextView = findViewById(R.id.viewAlertCountTextView)
            // sometimes menu is prepared after sharedVM observation, add a catch update here
            val count = sharedVM.notifications.value?.filterNot { n -> n.read }?.size ?: 0
            updateFeedNotificationIcon(count)
            setOnClickListener { onOptionsItemSelected(rootView) }
        }
        super.onPrepareOptionsMenu(menu)
    }

    private fun updateFeedNotificationIcon(count: Int) {
        // if alert count extends into two digits, just show the red circle
        countTextView?.text = if (count in 1..9) "$count" else ""
        redCircle?.visibility = if (count > 0) View.VISIBLE else View.GONE
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.forumRule -> {
                if (activity == null || !isAdded) return true
                val fid = sharedVM.selectedForumId.value
                if (fid == null) {
                    toast(R.string.please_try_again_later)
                    return true
                }
                val fidInt: Int?
                try {
                    fidInt = fid.toInt()
                } catch (e: Exception) {
                    toast(R.string.did_not_select_forum_id)
                    return true
                }
                MaterialDialog(requireContext()).show {
                    val biId = if (fidInt > 0) fidInt else 1
                    val resourceId: Int = context.resources.getIdentifier(
                        "bi_$biId", "drawable",
                        context.packageName
                    )
                    icon(resourceId)
                    title(text = sharedVM.getForumDisplayName(fid))
                    message(text = sharedVM.getForumMsg(fid)) {
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
            R.id.feedNotification -> {
                val action = PostsFragmentDirections.actionPostsFragmentToNotificationFragment()
                findNavController().navigate(action)
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        if (mAdapter == null) {
            mAdapter = QuickAdapter<Post>(R.layout.list_item_post, sharedVM).apply {
                setOnItemClickListener { _, _, position ->
                    if (activity == null || !isAdded) return@setOnItemClickListener
                    viewCaching = DawnApp.applicationDataStore.getViewCaching()
                    getItem(position).run {
                        val navAction = MainNavDirections.actionGlobalCommentsFragment(id, fid)
                        findNavController().navigate(navAction)
                    }
                }
                setOnItemLongClickListener { _, _, position ->
                    if (activity == null || !isAdded) return@setOnItemLongClickListener true
                    MaterialDialog(requireContext()).show {
                        title(R.string.post_options)
                        listItems(R.array.post_options) { _, index, _ ->
                            when (index) {
                                0 -> {
                                    MaterialDialog(requireContext()).show {
                                        title(R.string.report_reasons)
                                        listItemsSingleChoice(res = R.array.report_reasons) { _, _, text ->
                                            postPopup.setupAndShow(
                                                "18",//值班室
                                                "18",
                                                newPost = true,
                                                quote = ">>No.${getItem(position).id}\n${context.getString(
                                                    R.string.report_reasons
                                                )}: $text\n"
                                            )
                                        }
                                        cancelOnTouchOutside(false)
                                    }
                                }
                                1 -> {
                                    val post = getItem(position)
                                    if (!post.isStickyTopBanner()) {
                                        viewModel.blockPost(post)
                                        toast(getString(R.string.blocked_post, post.id))
                                        mAdapter?.removeAt(position)
                                    } else {
                                        toast("你真的想屏蔽这个串吗？(ᯣ ̶̵̵̵̶̶̶̶̵̫̋̋̅̅̅ᯣ )", Toast.LENGTH_LONG)
                                    }
                                }
                                else -> {
                                    throw Exception("Unhandled option")
                                }
                            }
                        }
                    }
                    true
                }

                addChildClickViewIds(R.id.attachedImage)
                setOnItemChildClickListener { _, view, position ->
                    if (activity == null || !isAdded) return@setOnItemChildClickListener
                    if (view.id == R.id.attachedImage) {
                        val viewerPopup = ImageViewerPopup(requireContext())
                        viewerPopup.setSingleSrcView(view as ImageView?, getItem(position))
                        XPopup.Builder(context)
                            .asCustom(viewerPopup)
                            .show()
                    }
                }

                loadMoreModule.setOnLoadMoreListener {
                    viewModel.getPosts()
                }
            }
        }

        if (binding != null) {
            Timber.d("Fragment View Reusing!")
        } else {
            Timber.d("Fragment View Created")
            binding = FragmentPostBinding.inflate(inflater, container, false)

            binding!!.srlAndRv.refreshLayout.apply {
                setOnRefreshListener(object : RefreshingListenerAdapter() {
                    override fun onRefreshing() {
                        viewModel.refresh()
                    }
                })
            }

            binding!!.srlAndRv.recyclerView.apply {
                layoutManager = LinearLayoutManager(context)
                adapter = mAdapter
                setHasFixedSize(true)
                addOnScrollListener(object : RecyclerView.OnScrollListener() {
                    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                        if (activity == null || !isAdded) return
                        if (dy > 0) {
                            hideFabMenu()
                            binding?.fabMenu?.hide()
                            binding?.fabMenu?.isClickable = false
                        } else if (dy < 0) {
                            binding?.fabMenu?.show()
                            binding?.fabMenu?.isClickable = true
                        }
                    }
                })
            }

            binding!!.fabMenu.setOnClickListener {
                if (activity == null || !isAdded) return@setOnClickListener
                toggleFabMenu()
            }

            binding!!.post.setOnClickListener {
                if (activity == null || !isAdded) return@setOnClickListener
                if (sharedVM.selectedForumId.value == null) {
                    toast(R.string.please_try_again_later)
                    return@setOnClickListener
                }
                hideFabMenu()
                postPopup.setupAndShow(
                    sharedVM.selectedForumId.value,
                    sharedVM.selectedForumId.value!!,
                    true
                )
            }

            binding!!.announcement.setOnClickListener {
                if (activity == null || !isAdded) return@setOnClickListener
                hideFabMenu()
                DawnApp.applicationDataStore.nmbNotice?.let { notice ->
                    MaterialDialog(requireContext()).show {
                        title(res = R.string.announcement)
                        message(text = notice.content) { html() }
                        positiveButton(R.string.close)
                    }
                }
            }

            binding!!.search.setOnClickListener {
                if (activity == null || !isAdded) return@setOnClickListener
                hideFabMenu()
                if (DawnApp.applicationDataStore.firstCookieHash == null) {
                    toast(R.string.need_cookie_to_search)
                    return@setOnClickListener
                }

                MaterialDialog(requireContext()).show {
                    title(R.string.search)
                    customView(R.layout.dialog_search, noVerticalPadding = true).apply {
                        findViewById<Button>(R.id.search).setOnClickListener {
                            val query = findViewById<TextView>(R.id.searchInputText).text.toString()
                            if (query.isNotBlank()) {
                                dismiss()
                                viewCaching = DawnApp.applicationDataStore.getViewCaching()
                                val action =
                                    PostsFragmentDirections.actionPostsFragmentToSearchFragment(
                                        query
                                    )
                                findNavController().navigate(action)
                            } else {
                                toast(R.string.please_input_valid_text)
                            }
                        }

                        findViewById<Button>(R.id.jumpToPost).setOnClickListener {
                            val threadId = findViewById<TextView>(R.id.searchInputText).text
                                .filter { it.isDigit() }.toString()
                            if (threadId.isNotEmpty()) {
                                // Does not have fid here. fid will be generated when data comes back in reply
                                dismiss()
                                viewCaching = DawnApp.applicationDataStore.getViewCaching()
                                val navAction =
                                    MainNavDirections.actionGlobalCommentsFragment(threadId, "")
                                findNavController().navigate(navAction)
                            } else {
                                toast(R.string.please_input_valid_text)
                            }
                        }
                    }
                }
            }

            binding!!.flingInterceptor.bindListener {
                if (activity == null || !isAdded) return@bindListener
                (activity as MainActivity).showDrawer()
            }
        }

        viewModel.loadingStatus.observe(
            viewLifecycleOwner,
            Observer<SingleLiveEvent<EventPayload<Nothing>>> {
                if (mAdapter == null || binding == null || activity == null || !isAdded) return@Observer
                it.getContentIfNotHandled()?.run {
                    updateHeaderAndFooter(binding!!.srlAndRv.refreshLayout, mAdapter!!, this)
                }
            })

        viewModel.posts.observe(viewLifecycleOwner, Observer<List<Post>> {
            if (mAdapter == null || binding == null || activity == null || !isAdded) return@Observer
            if (it.isEmpty()) {
                if (!mAdapter!!.hasEmptyView()) mAdapter!!.setDefaultEmptyView()
                mAdapter!!.setDiffNewData(null)
                return@Observer
            }
            // set forum when navigate from website url
            if (sharedVM.selectedForumId.value == null) {
                sharedVM.setForumId(it.first().fid)
            }
            if (refreshing) {
                mAdapter!!.setList(it.toMutableList())
                binding?.srlAndRv?.recyclerView?.scrollToPosition(0)
            } else mAdapter!!.setDiffNewData(it.toMutableList())
            refreshing = false
            Timber.i("${this.javaClass.simpleName} Adapter will have ${it.size} threads")
        })

        sharedVM.selectedForumId.observe(viewLifecycleOwner, Observer<String> {
            if (mAdapter == null || binding == null || activity == null || !isAdded) return@Observer
            if (viewModel.currentFid != it) {
                refreshing = true
                viewModel.setForum(it)
                sharedVM.forumRefresh = false
            } else if (sharedVM.forumRefresh) {
                refreshing = true
                viewModel.refresh()
                sharedVM.forumRefresh = false
            }
        })

        sharedVM.notifications.observe(viewLifecycleOwner, Observer<List<Notification>> { list ->
            val count = list.filterNot { it.read }.size
            updateFeedNotificationIcon(count)
        })

        viewCaching = false
        return binding!!.root
    }

    private fun hideFabMenu() {
        val rotateBackward = AnimationUtils.loadAnimation(requireContext(), R.anim.rotate_backward)
        binding?.fabMenu?.startAnimation(rotateBackward)
        binding?.announcement?.hide()
        binding?.search?.hide()
        binding?.post?.hide()
        isFabOpen = false
    }

    private fun showFabMenu() {
        val rotateForward = AnimationUtils.loadAnimation(requireContext(), R.anim.rotate_forward)
        binding?.fabMenu?.startAnimation(rotateForward)
        binding?.announcement?.show()
        binding?.search?.show()
        binding?.post?.show()
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
        if (!viewCaching) {
            mAdapter = null
            binding = null
        }
        Timber.d("Fragment View Destroyed ${binding == null}")
    }
}


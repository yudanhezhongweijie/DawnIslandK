package com.laotoua.dawnislandk.screens.replys


import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.text.style.UnderlineSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.text.toSpannable
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.laotoua.dawnislandk.DawnApp.Companion.applicationDataStore
import com.laotoua.dawnislandk.R
import com.laotoua.dawnislandk.data.local.Reply
import com.laotoua.dawnislandk.databinding.FragmentReplyBinding
import com.laotoua.dawnislandk.screens.MainActivity
import com.laotoua.dawnislandk.screens.SharedViewModel
import com.laotoua.dawnislandk.screens.adapters.QuickAdapter
import com.laotoua.dawnislandk.screens.util.Layout.updateHeaderAndFooter
import com.laotoua.dawnislandk.screens.util.ToolBar.immersiveToolbar
import com.laotoua.dawnislandk.screens.widget.DoubleClickListener
import com.laotoua.dawnislandk.screens.widget.popup.ImageLoader
import com.laotoua.dawnislandk.screens.widget.popup.ImageViewerPopup
import com.laotoua.dawnislandk.screens.widget.popup.PostPopup
import com.laotoua.dawnislandk.screens.widget.span.ReferenceSpan
import com.laotoua.dawnislandk.util.EventPayload
import com.laotoua.dawnislandk.util.SingleLiveEvent
import com.laotoua.dawnislandk.util.lazyOnMainOnly
import com.lxj.xpopup.XPopup
import com.lxj.xpopup.interfaces.SimpleCallback
import dagger.android.support.DaggerFragment
import me.dkzwm.widget.srl.RefreshingListenerAdapter
import me.dkzwm.widget.srl.config.Constants
import timber.log.Timber
import javax.inject.Inject


class ReplysFragment : DaggerFragment() {

    private var _binding: FragmentReplyBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    private val viewModel: ReplysViewModel by viewModels { viewModelFactory }
    private val sharedVM: SharedViewModel by activityViewModels{ viewModelFactory }

    private var isFabOpen = false
    private var _mAdapter: QuickAdapter<Reply>? = null
    private val mAdapter get() = _mAdapter!!

    // last visible item indicates the current page, uses for remembering last read page
    private var currentPage = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentReplyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.apply {
            immersiveToolbar()
            setSubtitle(R.string.toolbar_subtitle)
            val drawerLayout = requireActivity().findViewById<DrawerLayout>(R.id.drawerLayout)
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
            setNavigationIcon(R.drawable.ic_arrow_back_white_24px)
            setNavigationOnClickListener {
                (requireActivity() as MainActivity).hideReply()
            }
            setOnClickListener(
                DoubleClickListener(callback = object : DoubleClickListener.DoubleClickCallBack {
                    override fun doubleClicked() {
                        binding.recyclerView.layoutManager?.scrollToPosition(0)
                    }
                })
            )
        }

        val imageLoader = ImageLoader()
        val postPopup: PostPopup by lazyOnMainOnly { PostPopup(this, requireContext(), sharedVM) }
        val jumpPopup: JumpPopup by lazyOnMainOnly { JumpPopup(requireContext()) }

        _mAdapter = QuickAdapter<Reply>(R.layout.list_item_reply).apply {
            setReferenceClickListener(object : ReferenceSpan.ReferenceClickHandler {
                override fun handleReference(id: String) {
                    QuotePopup.showQuote(this@ReplysFragment, viewModel, requireContext(), id, viewModel.po)
                }
            })
            /*** connect SharedVm and adapter
             *  may have better way of getting runtime data
             */
            setSharedVM(sharedVM)

            setOnItemClickListener { _, _, _ ->
                hideFabMenu()
            }

            addChildClickViewIds(
                R.id.attachedImage,
                R.id.refId,
                R.id.expandSummary
            )

            setOnItemChildClickListener { _, view, position ->
                when (view.id) {
                    R.id.attachedImage -> {
                        hideFabMenu()
                        val url = getItem(position).getImgUrl()
                        // TODO support multiple image
                        val viewerPopup =
                            ImageViewerPopup(this@ReplysFragment, requireContext(), url)
                        viewerPopup.setXPopupImageLoader(imageLoader)
                        viewerPopup.setSingleSrcView(view as ImageView?, url)

                        XPopup.Builder(context)
                            .asCustom(viewerPopup)
                            .show()
                    }
                    R.id.refId -> {
                        val content = "${(view as TextView).text}\n"
                        postPopup.setupAndShow(
                            viewModel.currentThreadId,
                            quote = content
                        )
                    }
                    R.id.expandSummary -> {
                        data[position].visible = true
                        notifyItemChanged(position)
                    }
                }
            }

            // load more
            loadMoreModule.setOnLoadMoreListener {
                viewModel.getNextPage()
            }
        }

        binding.refreshLayout.apply {
            setOnRefreshListener(object : RefreshingListenerAdapter() {
                override fun onRefreshing() {
                    if (mAdapter.getItem(
                            (binding.recyclerView.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
                        ).page == 1
                    ) {
                        Toast.makeText(context, "没有上一页了。。。", Toast.LENGTH_SHORT).show()
                        refreshComplete(true, 100L)
                    } else {
                        viewModel.getPreviousPage()
                    }
                }
            })
        }

        binding.recyclerView.apply {
            val llm = LinearLayoutManager(context)
            layoutManager = llm
            adapter = mAdapter
            setHasFixedSize(true)
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    if (dy > 0) {
                        hideFab()
                    } else if (dy < 0) {
                        showFab()
                        if (llm.findFirstVisibleItemPosition() <= 2 && !binding.refreshLayout.isRefreshing) {
                            viewModel.getPreviousPage()
                        }
                    }

                    val lastVisiblePos = llm.findLastVisibleItemPosition()
                    if (lastVisiblePos < mAdapter.data.lastIndex) {
                        updateCurrentPage(mAdapter.getItem(lastVisiblePos).page)
                    }
                }
            })
        }

        binding.filter.setOnClickListener {
            binding.filter.apply {
                isActivated = isActivated.not()
                if (!isActivated) {
                    viewModel.clearFilter()
                    Toast.makeText(context, R.string.reply_filter_off, Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.onlyPo()
                    Toast.makeText(context, R.string.reply_filter_on, Toast.LENGTH_SHORT).show()
                }
                (binding.recyclerView.layoutManager as LinearLayoutManager).run {
                    val startPos = findFirstVisibleItemPosition()
                    val itemCount = findLastVisibleItemPosition() - startPos
                    mAdapter.notifyItemRangeChanged(startPos, itemCount + initialPrefetchItemCount)
                }
            }
        }

        binding.fabMenu.setOnClickListener {
            toggleMenu()
        }

        binding.copyId.setOnClickListener {
            copyId(">>No.${viewModel.currentThreadId}")
            hideFabMenu()
        }

        binding.post.setOnClickListener {
            hideFabMenu()
            val page = getCurrentPage(mAdapter)
            postPopup.setupAndShow(viewModel.currentThreadId) {
                if (page == viewModel.maxPage) {
                    mAdapter.loadMoreModule.loadMoreToLoading()
                }
            }
        }

        binding.jump.setOnClickListener {
            hideFabMenu()
            val page = getCurrentPage(mAdapter)
            XPopup.Builder(context)
                .setPopupCallback(object : SimpleCallback() {
                    override fun beforeShow() {
                        super.beforeShow()
                        jumpPopup.updatePages(page, viewModel.maxPage)
                    }
                })
                .asCustom(jumpPopup)
                .show()
                .dismissWith {
                    if (jumpPopup.submit) {
                        binding.refreshLayout.autoRefresh(Constants.ACTION_NOTHING, false)
                        mAdapter.setList(emptyList())
                        Timber.i("Jumping to ${jumpPopup.targetPage}...")
                        viewModel.jumpTo(jumpPopup.targetPage)
                    }
                }
        }

        binding.addFeed.setOnClickListener {
            hideFabMenu()
            viewModel.addFeed(applicationDataStore.feedId, viewModel.currentThreadId)
        }
    }

    private val addFeedObs = Observer<SingleLiveEvent<EventPayload<Nothing>>> {
        it.getContentIfNotHandled()?.let { eventPayload ->
            Toast.makeText(context, eventPayload.message, Toast.LENGTH_SHORT).show()
        }
    }

    private val selectedThreadIdObs = Observer<String> {
        if (it != viewModel.currentThreadId) {
            mAdapter.setList(emptyList())
            binding.pageCounter.text = ""
            currentPage = 0
        }
        viewModel.setThreadId(it)
        updateTitle()
    }

    private val loadingStatusObs = Observer<SingleLiveEvent<EventPayload<Nothing>>> {
        it.getContentIfNotHandled()?.run {
            updateHeaderAndFooter(binding.refreshLayout, mAdapter, this)
        }
    }

    private val replysObs = Observer<MutableList<Reply>> {
        if (it.isEmpty()) return@Observer
        if (mAdapter.data.isEmpty()) updateCurrentPage(it.first().page)
        mAdapter.setDiffNewData(it.toMutableList())
        mAdapter.setPo(viewModel.po)
        Timber.i("${this.javaClass.simpleName} Adapter will have ${mAdapter.data.size} threads")
    }

    private fun subscribeUI() {
        viewModel.addFeedResponse.observe(viewLifecycleOwner, addFeedObs)
        sharedVM.selectedThreadId.observe(viewLifecycleOwner, selectedThreadIdObs)
        viewModel.loadingStatus.observe(viewLifecycleOwner, loadingStatusObs)
        viewModel.replys.observe(viewLifecycleOwner, replysObs)
    }

    private fun unsubscribeUI() {
        viewModel.addFeedResponse.removeObserver(addFeedObs)
        sharedVM.selectedThreadId.removeObserver(selectedThreadIdObs)
        viewModel.loadingStatus.removeObserver(loadingStatusObs)
        viewModel.replys.removeObserver(replysObs)
    }

    override fun onPause() {
        super.onPause()
        hideFab()
        unsubscribeUI()
    }

    override fun onResume() {
        super.onResume()
        showFab()
        subscribeUI()
    }

    private fun copyId(text: String) {
        val clipboard = getSystemService(
            requireContext(),
            ClipboardManager::class.java
        )
        val clip: ClipData =
            ClipData.newPlainText("currentThreadId", text)
        clipboard?.setPrimaryClip(clip)
        Toast.makeText(context, R.string.thread_id_copied, Toast.LENGTH_SHORT).show()
    }

    private fun getCurrentPage(adapter: QuickAdapter<Reply>): Int {
        val pos = (binding.recyclerView.layoutManager as LinearLayoutManager)
            .findLastVisibleItemPosition()
            .coerceAtLeast(0)
            .coerceAtMost(adapter.data.lastIndex)
        return adapter.getItem(pos).page
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _mAdapter = null
        _binding = null
        Timber.d("Fragment View Destroyed")
    }

    override fun onDestroy() {
        super.onDestroy()
        QuotePopup.clearQuotePopups()
    }

    private fun hideFab(){
        binding.fabMenu.hide()
        hideFabMenu()
        binding.fabMenu.isClickable = false
    }

    private fun showFab(){
        binding.fabMenu.show()
        binding.fabMenu.isClickable = true
    }

    private fun hideFabMenu() {
        val rotateBackward = AnimationUtils.loadAnimation(
            requireContext(),
            R.anim.rotate_backward
        )
        binding.fabMenu.startAnimation(rotateBackward)
        binding.post.hide()
        binding.jump.hide()
        binding.copyId.hide()
        binding.addFeed.hide()
        isFabOpen = false
    }

    private fun showFabMenu() {
        val rotateForward = AnimationUtils.loadAnimation(
            requireContext(),
            R.anim.rotate_forward
        )
        binding.fabMenu.startAnimation(rotateForward)
        binding.post.show()
        binding.jump.show()
        binding.copyId.show()
        binding.addFeed.show()
        isFabOpen = true
    }

    private fun toggleMenu() {
        if (isFabOpen) {
            hideFabMenu()
        } else {
            showFabMenu()
        }
    }

    private fun updateTitle() {
        binding.toolbar.title = "${sharedVM.getSelectedThreadForumName()} • ${viewModel.currentThreadId}"
    }

    private fun updateCurrentPage(page: Int) {
        if (page != currentPage) {
            viewModel.saveReadingProgress(page)
            binding.pageCounter.text =
                (page.toString() + " / " + viewModel.maxPage.toString()).toSpannable()
                    .apply { setSpan(UnderlineSpan(), 0, length, 0) }

            currentPage = page
        }
    }
}

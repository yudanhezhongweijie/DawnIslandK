package com.laotoua.dawnislandk.ui.fragment


import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat.getSystemService
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.laotoua.dawnislandk.R
import com.laotoua.dawnislandk.data.entity.Reply
import com.laotoua.dawnislandk.data.network.ImageLoader
import com.laotoua.dawnislandk.data.state.AppState
import com.laotoua.dawnislandk.databinding.FragmentReplyBinding
import com.laotoua.dawnislandk.ui.adapter.QuickAdapter
import com.laotoua.dawnislandk.ui.popup.ImageViewerPopup
import com.laotoua.dawnislandk.ui.popup.JumpPopup
import com.laotoua.dawnislandk.ui.popup.PostPopup
import com.laotoua.dawnislandk.ui.popup.QuotePopup
import com.laotoua.dawnislandk.ui.util.UIUtils.updateHeaderAndFooter
import com.laotoua.dawnislandk.viewmodel.ReplyViewModel
import com.laotoua.dawnislandk.viewmodel.SharedViewModel
import com.lxj.xpopup.XPopup
import com.lxj.xpopup.interfaces.SimpleCallback
import me.dkzwm.widget.srl.RefreshingListenerAdapter
import me.dkzwm.widget.srl.config.Constants
import me.dkzwm.widget.srl.extra.header.ClassicHeader
import me.dkzwm.widget.srl.indicator.IIndicator
import timber.log.Timber


class ReplyFragment : Fragment() {

    //TODO: maintain reply fragment when pressing back, such that progress can be remembered
    private var _binding: FragmentReplyBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ReplyViewModel by viewModels()
    private val sharedVM: SharedViewModel by activityViewModels()
    private val mAdapter =
        QuickAdapter(R.layout.list_item_reply).apply {
            setReferenceClickListener { quote ->
                // TODO: get Po based on Thread
                QuotePopup.showQuote(
                    this@ReplyFragment,
                    requireContext(),
                    quotePopup,
                    quote,
                    viewModel.po
                )
            }
        }


    private val imageLoader: ImageLoader by lazy { ImageLoader(requireContext()) }

    private val postPopup: PostPopup by lazy { PostPopup(this, requireContext()) }

    private val quotePopup: QuotePopup by lazy { QuotePopup(this, requireContext()) }

    private val jumpPopup: JumpPopup by lazy { JumpPopup(this, requireContext()) }

    private var isFabOpen = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        sharedVM.setFragment(this)

        _binding = FragmentReplyBinding.inflate(inflater, container, false)

        binding.refreshLayout.apply {
            setHeaderView(ClassicHeader<IIndicator>(context))
            setOnRefreshListener(object : RefreshingListenerAdapter() {
                override fun onRefreshing() {
                    viewModel.getPreviousPage()
                }
            })
        }

        binding.replysView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = mAdapter
            setHasFixedSize(true)
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    if (dy > 0) {
                        binding.fabMenu.hide()
                        hideMenu()
                    } else if (dy < 0) binding.fabMenu.show()
                }
            })
        }

        /*** connect SharedVm and adapter
         *  may have better way of getting runtime data
         */
        mAdapter.setSharedVM(sharedVM)

        // item click
        mAdapter.apply {
            // initial load
            if (data.size == 0) {
                viewModel.getNextPage()
                binding.refreshLayout.autoRefresh(Constants.ACTION_NOTHING, false)
            }

            setOnItemClickListener { _, _, _ ->
                hideMenu()
            }

            // image
            addChildClickViewIds(
                R.id.replyImage,
                R.id.replyId
            )

            setOnItemChildClickListener { adapter, view, position ->
                if (view.id == R.id.replyImage) {
                    hideMenu()
                    Timber.i("clicked on image at $position")

                    val url = (adapter.getItem(position) as Reply).getImgUrl()
                    // TODO support multiple image
                    val viewerPopup = ImageViewerPopup(this@ReplyFragment, requireContext(), url)
                    viewerPopup.setXPopupImageLoader(imageLoader)
                    viewerPopup.setSingleSrcView(view as ImageView?, url)
                    viewerPopup.setOnClickListener {
                        Timber.i("on click in thread")
                    }
                    XPopup.Builder(context)
                        .asCustom(viewerPopup)
                        .show()
                } else if (view.id == R.id.replyId) {
                    // TODO
                    val replyId = (view as TextView).text
                    Timber.i("replyId: $replyId")
                }
            }

            // load more
            loadMoreModule.setOnLoadMoreListener {
                viewModel.getNextPage()
            }

        }

        viewModel.loadingStatus.observe(viewLifecycleOwner, Observer {
            it.getContentIfNotHandled()?.run {
                updateHeaderAndFooter(binding.refreshLayout, mAdapter, this)
            }
        })

        viewModel.reply.observe(viewLifecycleOwner, Observer {
            if (viewModel.direction == ReplyViewModel.DIRECTION.PREVIOUS) {
                Timber.i("Inserting items to the top")
//                    val diffResult = DiffUtil.calculateDiff(DiffCallback(mAdapter.data, it), false)
//                    mAdapter.setDiffNewData(diffResult, it.toMutableList())
                // TODO: previous page & next page should be handled the same
                mAdapter.addData(0, viewModel.previousPage)
            } else {
                Timber.i("Inserting items to the end")
                mAdapter.setDiffNewData(it.toMutableList())
            }

            mAdapter.setPo(viewModel.po)
            Timber.i("Adapter will have ${it.size} threads")
        })

        sharedVM.selectedThread.observe(viewLifecycleOwner, Observer {
            if (viewModel.currentThread == null) {
                viewModel.setThread(it)
            } else if (viewModel.currentThread != null && viewModel.currentThread!!.id != it.id) {
                Timber.i("Thread has changed to ${it.id}. Clearing old data...")
                mAdapter.setList(ArrayList())
                viewModel.setThread(it)
            }

        })

        binding.fabMenu.setOnClickListener {
            toggleMenu()
        }

        binding.copyId.setOnClickListener {
            val clipboard = getSystemService(
                requireContext(),
                ClipboardManager::class.java
            )
            val clip: ClipData =
                ClipData.newPlainText("currentThreadId", ">>No.${viewModel.currentThread!!.id}")
            clipboard?.setPrimaryClip(clip)
            Toast.makeText(context, "串号已复制", Toast.LENGTH_SHORT).show()
            hideMenu()
        }

        binding.post.setOnClickListener {
            hideMenu()
            PostPopup.show(this, postPopup, viewModel.currentThread!!.id)
        }

        binding.jump.setOnClickListener {
            hideMenu()
            val pos = (binding.replysView.layoutManager as LinearLayoutManager)
                .findLastCompletelyVisibleItemPosition()
                .coerceAtLeast(0)
                .coerceAtMost(mAdapter.data.lastIndex)
            val page = (mAdapter.getItem(pos) as Reply).page!!
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
                        Timber.i("Jumping to ${jumpPopup.targetPage}...")
                        viewModel.jumpTo(jumpPopup.targetPage)
                    }
                }
        }

        binding.onlyPo.setOnClickListener {
            Timber.i("Clicked on onlyPo")
            Toast.makeText(context, "还没写嘿嘿嘿。。", Toast.LENGTH_SHORT).show()
            hideMenu()
        }

        binding.addFeed.setOnClickListener {
            hideMenu()
            viewModel.addFeed(AppState.feedId, viewModel.currentThread!!.id)
        }

        viewModel.addFeedResponse.observe(viewLifecycleOwner, Observer {
            it.getContentIfNotHandled()?.let { eventPayload ->
                Toast.makeText(context, eventPayload.message, Toast.LENGTH_SHORT).show()
            }
        })

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        Timber.d("Fragment View Destroyed")
    }

    private fun hideMenu() {
        val rotateBackward = AnimationUtils.loadAnimation(
            requireContext(),
            R.anim.rotate_backward
        )
        binding.fabMenu.startAnimation(rotateBackward)
        binding.post.hide()
        binding.jump.hide()
        binding.copyId.hide()
        binding.onlyPo.hide()
        binding.addFeed.hide()
        isFabOpen = false
    }

    private fun showMenu() {
        val rotateForward = AnimationUtils.loadAnimation(
            requireContext(),
            R.anim.rotate_forward
        )
        binding.fabMenu.startAnimation(rotateForward)
        binding.post.show()
        binding.jump.show()
        binding.copyId.show()
        binding.onlyPo.show()
        binding.addFeed.show()
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

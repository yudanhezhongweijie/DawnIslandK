package com.laotoua.dawnislandk


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.chad.library.adapter.base.listener.OnItemChildClickListener
import com.laotoua.dawnislandk.components.ImageViewerPopup
import com.laotoua.dawnislandk.components.JumpPopup
import com.laotoua.dawnislandk.components.PostPopup
import com.laotoua.dawnislandk.components.QuotePopup
import com.laotoua.dawnislandk.databinding.ReplyFragmentBinding
import com.laotoua.dawnislandk.entities.Reply
import com.laotoua.dawnislandk.network.ImageLoader
import com.laotoua.dawnislandk.util.QuickAdapter
import com.laotoua.dawnislandk.util.ToolbarUtil
import com.laotoua.dawnislandk.util.extractQuoteId
import com.laotoua.dawnislandk.viewmodels.ReplyViewModel
import com.laotoua.dawnislandk.viewmodels.SharedViewModel
import com.lxj.xpopup.XPopup
import com.lxj.xpopup.interfaces.SimpleCallback
import kotlinx.android.synthetic.main.activity_main.*
import me.dkzwm.widget.srl.RefreshingListenerAdapter
import me.dkzwm.widget.srl.extra.header.ClassicHeader
import me.dkzwm.widget.srl.indicator.IIndicator
import timber.log.Timber


class ReplyFragment : Fragment() {

    //TODO: maintain reply fragment when pressing back, such that progress can be remembered
    private var _binding: ReplyFragmentBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ReplyViewModel by viewModels()
    private val sharedVM: SharedViewModel by activityViewModels()
    private val mAdapter = QuickAdapter(R.layout.reply_list_item)

    private val imageLoader: ImageLoader by lazy { ImageLoader(requireContext()) }

    private val postPopup: PostPopup by lazy { PostPopup(this, requireContext()) }

    private val quotePopup: QuotePopup by lazy { QuotePopup(this, requireContext()) }

    private val jumpPopup: JumpPopup by lazy { JumpPopup(this, requireContext()) }

    private var isFabOpen = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        sharedVM.setFragment(this.javaClass.simpleName)

        _binding = ReplyFragmentBinding.inflate(inflater, container, false)

        binding.replysView.layoutManager = LinearLayoutManager(context)
        binding.replysView.adapter = mAdapter


        /*** connect SharedVm and adapter
         *  may have better way of getting runtime data
         */
        mAdapter.setSharedVM(sharedVM)

        // item click
        mAdapter.setOnItemClickListener {
            // TODO: needs reply popup
                adapter, view, position ->
            hideMenu()
            Timber.d("onItemClick $position")
        }

        // image
        mAdapter.addChildClickViewIds(R.id.replyImage, R.id.replyId)
        mAdapter.setOnItemChildClickListener { adapter, view, position ->
            if (view.id == R.id.replyImage) {
                hideMenu()
                Timber.i("clicked on image at $position")

                val url = (adapter.getItem(position) as Reply).getImgUrl()
                // TODO support multiple image
                val viewerPopup = ImageViewerPopup(this, requireContext(), url)
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
                Timber.i("replyid: $replyId")
            }
        }

        mAdapter.addCustomChildIds(R.id.quoteId)
        mAdapter.setCustomQuoteClickListener(
            OnItemChildClickListener { _, view, _ ->
                // TODO Loading animation
                val id = extractQuoteId(view.findViewById<TextView>(R.id.quoteId).text as String)
                // TODO: get Po based on Thread
                QuotePopup.showQuote(this, requireContext(), quotePopup, id, sharedVM.getPo())
            })

        // load more
        mAdapter.loadMoreModule.setOnLoadMoreListener {
            Timber.i("Fetching next page...")
            viewModel.getNextPage()
        }

        viewModel.loadEnd.observe(viewLifecycleOwner, Observer {
            if (it == true) {
                if (binding.refreshLayout.isRefreshing) {
                    binding.refreshLayout.refreshComplete(true)
                } else {
                    mAdapter.loadMoreModule.loadMoreEnd()
                }
                Timber.i("Finished loading data...")
            }
        })

        viewModel.loadFail.observe(viewLifecycleOwner, Observer {
            if (it == true) {
                if (binding.refreshLayout.isRefreshing) {
                    binding.refreshLayout.refreshComplete(false)
                } else {
                    mAdapter.loadMoreModule.loadMoreFail()
                }
                Timber.i("Failed to load new data...")
            }
        })

        viewModel.reply.observe(viewLifecycleOwner, Observer {
            if (binding.refreshLayout.isRefreshing) {
                // TODO notify might not be needed, cause inconsistency
                Timber.i("Inserting items to the top")
//                    val diffResult = DiffUtil.calculateDiff(DiffCallback(mAdapter.data, it), false)
//                    mAdapter.setDiffNewData(diffResult, it.toMutableList())
                // TODO: previous page & next page should be handled the same
                mAdapter.addData(0, viewModel.previousPage)
                binding.refreshLayout.refreshComplete()
            } else {
                Timber.i("Inserting items to the end")
                mAdapter.setDiffNewData(it.toMutableList())
                mAdapter.loadMoreModule.loadMoreComplete()
            }
            Timber.i("New data found. Adapter now have ${mAdapter.data.size} threads")
        })

        sharedVM.selectedThread.observe(viewLifecycleOwner, Observer {
            if (viewModel.currentThread == null) {
                viewModel.setThread(it)
                updateAppBar()
            } else if (viewModel.currentThread != null && viewModel.currentThread!!.id != it.id) {
                Timber.i("Thread has changed to ${it.id}. Clearing old data...")
                mAdapter.setList(ArrayList())
                viewModel.setThread(it)
                updateAppBar()
            }

        })
        binding.replysView.setOnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
            if (scrollY < oldScrollY) {
                binding.fabMenu.show()
            } else {
                binding.fabMenu.hide()
                hideMenu()
            }
        }

        binding.fabMenu.setOnClickListener {
            toggleMenu()
        }

        binding.copyId.setOnClickListener {
            Timber.i("Clicked on copy Id")
            hideMenu()
        }

        binding.create.setOnClickListener {
            hideMenu()

            XPopup.Builder(context)
                .asCustom(postPopup)
                .show()
        }

        binding.jump.setOnClickListener {
            hideMenu()
            val pos = 1.coerceAtLeast(
                (binding.replysView.layoutManager as LinearLayoutManager)
                    .findLastCompletelyVisibleItemPosition()
            )
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
                        Timber.i("Jumping to ${jumpPopup.targetPage}...")
                        viewModel.jumpTo(jumpPopup.targetPage)
                    }
                }
        }

        binding.onlyPo.setOnClickListener {
            Timber.i("Clicked on onlyPo")
            hideMenu()
        }


        binding.refreshLayout.setHeaderView(ClassicHeader<IIndicator>(context))
        binding.refreshLayout.setOnRefreshListener(object : RefreshingListenerAdapter() {
            override fun onRefreshing() {
                viewModel.getPreviousPage()
            }
        })

        updateAppBar()
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.d("Reply Fragment destroyed!")
        ToolbarUtil.enableCollapse(requireActivity(), "")
    }


    private fun hideMenu() {
        val rotateBackward = AnimationUtils.loadAnimation(requireContext(), R.anim.rotate_backward);
        binding.fabMenu.startAnimation(rotateBackward)
        binding.create.hide()
        binding.jump.hide()
        binding.copyId.hide()
        binding.onlyPo.hide()
        isFabOpen = false
    }

    private fun showMenu() {
        val rotateForward = AnimationUtils.loadAnimation(requireContext(), R.anim.rotate_forward);
        binding.fabMenu.startAnimation(rotateForward)
        binding.create.show()
        binding.jump.show()
        binding.copyId.show()
        binding.onlyPo.show()
        isFabOpen = true
    }

    private fun toggleMenu() {
        if (isFabOpen) {
            hideMenu()
        } else {
            showMenu()
        }
    }

    // TODO refresh click
    private fun updateAppBar() {

        requireActivity().run {
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
            val title =
                "A岛 • ${sharedVM.selectedForum.value!!.name} • ${sharedVM.selectedThread.value?.id}"
            ToolbarUtil.disableCollapse(this, title)
            toolbar.setNavigationIcon(R.drawable.ic_arrow_back)
            toolbar.setNavigationOnClickListener(null)
            toolbar.setNavigationOnClickListener {
                findNavController().popBackStack()
            }
        }
    }
}

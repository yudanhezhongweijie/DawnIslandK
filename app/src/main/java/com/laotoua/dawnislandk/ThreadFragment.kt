package com.laotoua.dawnislandk

import android.os.Bundle
import android.view.*
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.laotoua.dawnislandk.databinding.ThreadFragmentBinding
import com.laotoua.dawnislandk.util.ImageViewerPopup
import com.laotoua.dawnislandk.util.ImageViewerPopup.ImageLoader
import com.laotoua.dawnislandk.util.QuickAdapter
import com.laotoua.dawnislandk.util.ThreadList
import com.laotoua.dawnislandk.viewmodels.SharedViewModel
import com.laotoua.dawnislandk.viewmodels.ThreadViewModel
import com.lxj.xpopup.XPopup
import timber.log.Timber


class ThreadFragment : Fragment() {

    private var _binding: ThreadFragmentBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ThreadViewModel by viewModels()
    private val sharedVM: SharedViewModel by activityViewModels()
    private val mAdapter = QuickAdapter(R.layout.thread_list_item)

    private val imageLoader: ImageLoader by lazy {
        ImageLoader(requireContext())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {

        inflater.inflate(R.menu.menu_thread, menu);

        super.onCreateOptionsMenu(menu, inflater)

    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        return when (item.itemId) {
            R.id.send -> {

                val dialog = childFragmentManager.findFragmentByTag("dialog")
                Timber.i("frags: ${childFragmentManager.fragments}")
                if (dialog == null) {
                    Timber.i("making dialog")
                    childFragmentManager.beginTransaction().add(SendDialog(), "dialog").commit()
                } else {
                    Timber.i("showing dialog")
                    childFragmentManager.beginTransaction().show(dialog).commit()
                }
                true
            }

            else -> {
                Timber.e("Unhandled item click")
                true
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        sharedVM.setFragment(this.javaClass.simpleName)

        _binding = ThreadFragmentBinding.inflate(inflater, container, false)
        Timber.i("connected sharedVM instance: $sharedVM viewModel: $viewModel viewLifeCycleOwner $viewLifecycleOwner")
        viewModel.setForumDao(sharedVM.getDb()?.forumDao())

        binding.threadsView.layoutManager = LinearLayoutManager(context)
        binding.threadsView.adapter = mAdapter

        /*** connect SharedVm and adapter
         *  may have better way of getting runtime data
         */
        mAdapter.setSharedVM(sharedVM)

        // item click
        mAdapter.setOnItemClickListener { adapter, _, position ->
            sharedVM.setThreadList(adapter.getItem(position) as ThreadList)
            val action = PagerFragmentDirections.actionPagerFragmentToReplyFragment()
            requireParentFragment().findNavController().navigate(action)

        }

        // image
        mAdapter.addChildClickViewIds(R.id.threadImage)
        mAdapter.setOnItemChildClickListener { adapter, view, position ->
            if (view.id == R.id.threadImage) {
                Timber.i("clicked on image at $position")
                val url = (adapter.getItem(
                    position
                ) as ThreadList).getImgUrl()

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

//                val action = PagerFragmentDirections.actionPagerFragmentToImageViewerFragment(
//                    (adapter.getItem(
//                        position
//                    ) as ThreadList).getImgUrl()
//                )
//                requireParentFragment().findNavController().navigate(action)
            }
        }

        // load more
        mAdapter.loadMoreModule.setOnLoadMoreListener {
            Timber.i("Fetching new data...")
            viewModel.getThreads()
        }


        // TODO: fragment trasactions forces new viewlifecycleowner created, hence will trigger observe actions
        viewModel.loadFail.observe(viewLifecycleOwner, Observer {
            // TODO: can be either out of new Data or api error
            if (it == true) {
                mAdapter.loadMoreModule.loadMoreFail()
                Timber.i("Failed to load new data...")
            }
        })
        viewModel.thread.observe(viewLifecycleOwner, Observer {
            mAdapter.setDiffNewData(it as MutableList<Any>)
            mAdapter.loadMoreModule.loadMoreComplete()
            Timber.i("New data found or new observer added. Adapter now have ${mAdapter.data.size} threads")

        })

        sharedVM.selectedForum.observe(viewLifecycleOwner, Observer {
            Timber.i(
                "shared VM change observed in Thread Fragment $viewLifecycleOwner with data $it"
            )
            if (viewModel.currentForum == null || viewModel.currentForum!!.id != it.id) {
                Timber.i("Forum has changed. Cleaning old adapter data...")
                mAdapter.setList(ArrayList())
                viewModel.setForum(it)
            }
        })

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


}

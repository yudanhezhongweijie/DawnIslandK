package com.laotoua.dawnislandk

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.laotoua.dawnislandk.databinding.ThreadFragmentBinding
import com.laotoua.dawnislandk.util.QuickAdapter
import com.laotoua.dawnislandk.util.ThreadList
import com.laotoua.dawnislandk.viewmodels.SharedViewModel
import com.laotoua.dawnislandk.viewmodels.ThreadViewModel
import timber.log.Timber


class ThreadFragment : Fragment() {

    private var _binding: ThreadFragmentBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ThreadViewModel by viewModels()
    private val sharedVM: SharedViewModel by activityViewModels()
    private val mAdapter = QuickAdapter(R.layout.thread_list_item)


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = ThreadFragmentBinding.inflate(inflater, container, false)
        Timber.i("connected sharedVM instance: $sharedVM viewLifeCycleOwner $viewLifecycleOwner")
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
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, ReplyFragment())
                .addToBackStack(null)
                .commit()

        }

        // image
        mAdapter.addChildClickViewIds(R.id.threadImage)
        mAdapter.setOnItemChildClickListener { adapter, view, position ->
            if (view.id == R.id.threadImage) {
                Timber.i("clicked on image at $position")
                val dest = ImageViewerFragment()
                val bundle = Bundle()
                bundle.putString("imgUrl", (adapter.getItem(position) as ThreadList).getImgUrl())
                dest.arguments = bundle
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, dest)
                    .addToBackStack(null)
                    .commit()
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
        viewModel.newPage.observe(viewLifecycleOwner, Observer {
            mAdapter.addData(it)
            mAdapter.loadMoreModule.loadMoreComplete()
            Timber.i("New data found. Adapter now have ${mAdapter.data.size} threads")

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

package com.laotoua.dawnislandk

//import com.laotoua.dawnislandk.util.DiffCallback
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.laotoua.dawnislandk.databinding.ReplyFragmentBinding
import com.laotoua.dawnislandk.util.QuickAdapter
import com.laotoua.dawnislandk.util.Reply
import com.laotoua.dawnislandk.viewmodels.ReplyViewModel
import com.laotoua.dawnislandk.viewmodels.SharedViewModel


class ReplyFragment : Fragment() {

    companion object {
        fun newInstance() = ReplyFragment()
    }

    private var _binding: ReplyFragmentBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ReplyViewModel by viewModels()
    private val sharedVM: SharedViewModel by activityViewModels()
    private val TAG = "ReplyFragment"
    private val mAdapter = QuickAdapter(R.layout.reply_list_item)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = ReplyFragmentBinding.inflate(inflater, container, false)
        Log.i(TAG, "connected sharedVM instance: $sharedVM viewLifeCycleOwner $viewLifecycleOwner")

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
            Log.d(TAG, "onItemClick $position")

        }

        // image
        mAdapter.addChildClickViewIds(R.id.replyImage)
        mAdapter.setOnItemChildClickListener { adapter, view, position ->
            if (view.id == R.id.replyImage) {
                Log.i(TAG, "clicked on image at $position")
                val dest = ImageViewerFragment()
                val bundle = Bundle()
                bundle.putString("imgUrl", (adapter.getItem(position) as Reply).getImgUrl())
                dest.arguments = bundle
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, dest)
                    .addToBackStack(null)
                    .commit()
            }
        }

        // TODO: fragment trasactions forces new viewlifecycleowner created, hence will trigger observe actions
        // load more
        mAdapter.loadMoreModule.setOnLoadMoreListener {
            Log.i(TAG, "Fetching new data...")
            viewModel.getReplys()
        }

        viewModel.loadEnd.observe(viewLifecycleOwner, Observer {
            if (it == true) {
                mAdapter.loadMoreModule.loadMoreEnd()
                Log.i(TAG, "Finished loading data...")
            }
        })

        viewModel.loadFail.observe(viewLifecycleOwner, Observer {
            if (it == true) {
                mAdapter.loadMoreModule.loadMoreFail()
                Log.i(TAG, "Failed to load new data...")
            }
        })

        viewModel.newPage.observe(viewLifecycleOwner, Observer {
            mAdapter.addData(it)
            mAdapter.loadMoreModule.loadMoreComplete()
            Log.i(TAG, "New data found. Adapter now have ${mAdapter.data.size} threads")

        })

        sharedVM.selectedThreadList.observe(viewLifecycleOwner, Observer {
            Log.i(
                TAG,
                "shared VM change observed in Reply Fragment $viewLifecycleOwner with data $it"
            )
//            Log.i(TAG, "viewLifecycleOwner $viewLifecycleOwner, Observer $it")
            if (viewModel.currentThread == null || viewModel.currentThread!!.id != it.id) {
                Log.i(TAG, "Thread has changed.Cleaning old adapter data...")
                mAdapter.setList(ArrayList())
                viewModel.setThread(it)
            }

        })


        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

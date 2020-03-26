package com.laotoua.dawnislandk

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.laotoua.dawnislandk.databinding.ThreadFragmentBinding
import com.laotoua.dawnislandk.util.QuickAdapter
import com.laotoua.dawnislandk.viewmodels.ThreadViewModel


class ThreadFragment : Fragment() {

    private var _binding: ThreadFragmentBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: ThreadViewModel
    private val TAG: String = "ThreadFragment"

    companion object {
        fun newInstance() = ThreadFragment()
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = ThreadFragmentBinding.inflate(inflater, container, false)

        viewModel = ViewModelProvider(this).get(ThreadViewModel::class.java)
        val mAdapter = QuickAdapter(R.layout.thread_list_item)
        binding.threadsView.layoutManager = LinearLayoutManager(context)
        binding.threadsView.adapter = mAdapter

        // item click
        mAdapter.setOnItemClickListener {
            // TODO: needs jump
                adapter, view, position ->
            Log.d(TAG, "onItemClick $position")
//            Log.d(TAG, "Click Thread" + (adapter.getItem(position) as ThreadHead).id)

        }

        // load more
        mAdapter.loadMoreModule!!.setOnLoadMoreListener {
            viewModel.getThreads()
        }
//
        viewModel.newPage.observe(viewLifecycleOwner, Observer {
            mAdapter.addData(it)
            mAdapter.loadMoreModule!!.loadMoreComplete()
            Log.i(TAG, "Adapter now have ${mAdapter.data.size} threads")

        })

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}

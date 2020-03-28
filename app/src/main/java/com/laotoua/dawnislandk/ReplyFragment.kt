package com.laotoua.dawnislandk

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.laotoua.dawnislandk.databinding.ReplyFragmentBinding
import com.laotoua.dawnislandk.util.QuickAdapter
import com.laotoua.dawnislandk.viewmodels.ReplyViewModel
import com.laotoua.dawnislandk.viewmodels.SharedViewModel


class ReplyFragment : Fragment() {

    companion object {
        fun newInstance() = ReplyFragment()
    }

    private var _binding: ReplyFragmentBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: ReplyViewModel
    private val sharedVM: SharedViewModel by activityViewModels()
    private val TAG = "ReplyFragment"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = ReplyFragmentBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this).get(ReplyViewModel::class.java)

        val mAdapter = QuickAdapter(R.layout.reply_list_item)
        binding.replysView.layoutManager = LinearLayoutManager(context)
        binding.replysView.adapter = mAdapter

        // item click
        mAdapter.setOnItemClickListener {
            // TODO: needs reply popup
                adapter, view, position ->
            Log.d(TAG, "onItemClick $position")

        }

        // load more
        mAdapter.loadMoreModule!!.setOnLoadMoreListener {
            viewModel.getReplys()
        }

        viewModel.loadFail.observe(viewLifecycleOwner, Observer {
            // TODO: can be either out of new Data or api error
            if (it == true) {
                mAdapter.loadMoreModule!!.loadMoreFail()
            }
        })
//
        viewModel.newPage.observe(viewLifecycleOwner, Observer {
            mAdapter.addData(it)
            mAdapter.loadMoreModule!!.loadMoreComplete()
            Log.i(TAG, "Adapter now have ${mAdapter.data.size} threads")

        })

        sharedVM.selectedThreadList.observe(viewLifecycleOwner, Observer {
            Log.i(TAG, "shared VM change observed in Reply Fragment")
            Log.i(TAG, "Cleaning old adapter data...")
            mAdapter.replaceData(ArrayList())
            viewModel.setThread(it)

        })


        return binding.root
    }

}

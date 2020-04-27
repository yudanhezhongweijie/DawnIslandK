package com.laotoua.dawnislandk.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.laotoua.dawnislandk.R
import com.laotoua.dawnislandk.databinding.TrendFragmentBinding
import com.laotoua.dawnislandk.ui.adapter.QuickAdapter
import com.laotoua.dawnislandk.viewmodel.SharedViewModel
import com.laotoua.dawnislandk.viewmodel.TrendViewModel
import timber.log.Timber

class TrendFragment : Fragment() {

    private var _binding: TrendFragmentBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TrendViewModel by viewModels()
    private val sharedVM: SharedViewModel by activityViewModels()
    private val mAdapter = QuickAdapter(R.layout.trend_list_item)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = TrendFragmentBinding.inflate(inflater, container, false)

        binding.trendsView.layoutManager = LinearLayoutManager(context)
        binding.trendsView.adapter = mAdapter

        viewModel.getLatestTrend()
        // item click
        mAdapter.setOnItemClickListener { adapter, _, position ->
            Timber.i("clicked on item $position")
//            sharedVM.setThread(adapter.getItem(position) as Thread)
//            val action =
//                PagerFragmentDirections.actionPagerFragmentToReplyFragment()
//            findNavController().navigate(action)

        }

        viewModel.trendList.observe(viewLifecycleOwner, Observer { list ->
            mAdapter.setDiffNewData(list.toMutableList())
        })

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

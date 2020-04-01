package com.laotoua.dawnislandk

//import com.laotoua.dawnislandk.util.DiffCallback
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.laotoua.dawnislandk.databinding.ReplyFragmentBinding
import com.laotoua.dawnislandk.util.QuickAdapter
import com.laotoua.dawnislandk.util.Reply
import com.laotoua.dawnislandk.viewmodels.ReplyViewModel
import com.laotoua.dawnislandk.viewmodels.SharedViewModel
import timber.log.Timber


class ReplyFragment : Fragment() {

    companion object {
        fun newInstance() = ReplyFragment()
    }

    //TODO: maintain reply fragment when pressing back, such that progress can be remembered
    private var _binding: ReplyFragmentBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ReplyViewModel by viewModels()
    private val sharedVM: SharedViewModel by activityViewModels()
    private val mAdapter = QuickAdapter(R.layout.reply_list_item)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = ReplyFragmentBinding.inflate(inflater, container, false)
        Timber.i("connected sharedVM instance: $sharedVM viewModel: $viewModel viewLifeCycleOwner $viewLifecycleOwner")

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
            Timber.d("onItemClick $position")

        }

        // image
        mAdapter.addChildClickViewIds(R.id.replyImage)
        mAdapter.setOnItemChildClickListener { adapter, view, position ->
            if (view.id == R.id.replyImage) {
                Timber.i("clicked on image at $position")

                val action =
                    ReplyFragmentDirections.actionReplyFragmentToImageViewerFragment(
                        (adapter.getItem(
                            position
                        ) as Reply).getImgUrl()
                    )
                view.findNavController().navigate(action)
            }
        }


        // load more
        mAdapter.loadMoreModule.setOnLoadMoreListener {
            Timber.i("Fetching new data...")
            viewModel.getReplys()
        }

        viewModel.loadEnd.observe(viewLifecycleOwner, Observer {
            if (it == true) {
                mAdapter.loadMoreModule.loadMoreEnd()
                Timber.i("Finished loading data...")
            }
        })

        viewModel.loadFail.observe(viewLifecycleOwner, Observer {
            if (it == true) {
                mAdapter.loadMoreModule.loadMoreFail()
                Timber.i("Failed to load new data...")
            }
        })

        viewModel.reply.observe(viewLifecycleOwner, Observer { it ->
            mAdapter.setDiffNewData(it as MutableList<Any>)
            mAdapter.loadMoreModule.loadMoreComplete()
            Timber.i("New data found. Adapter now have ${mAdapter.data.size} threads")

        })

        sharedVM.selectedThreadList.observe(viewLifecycleOwner, Observer {
            Timber.i(
                "shared VM change observed in Reply Fragment $viewModel with owner $viewLifecycleOwner with data $it"
            )
            if (viewModel.currentThread == null || viewModel.currentThread!!.id != it.id) {
                Timber.i("Thread has changed or new observer added...")
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

    override fun onDestroy() {
        super.onDestroy()
        Timber.d("Reply Fragment destroyed!")
    }
}

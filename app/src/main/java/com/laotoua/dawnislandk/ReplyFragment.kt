package com.laotoua.dawnislandk

//import com.laotoua.dawnislandk.util.DiffCallback
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.laotoua.dawnislandk.components.CreatePopup
import com.laotoua.dawnislandk.components.ImageViewerPopup
import com.laotoua.dawnislandk.databinding.ReplyFragmentBinding
import com.laotoua.dawnislandk.entities.Reply
import com.laotoua.dawnislandk.util.QuickAdapter
import com.laotoua.dawnislandk.viewmodels.ReplyViewModel
import com.laotoua.dawnislandk.viewmodels.SharedViewModel
import com.lxj.xpopup.XPopup
import com.lxj.xpopup.core.BasePopupView
import timber.log.Timber


class ReplyFragment : Fragment() {

    //TODO: maintain reply fragment when pressing back, such that progress can be remembered
    private var _binding: ReplyFragmentBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ReplyViewModel by viewModels()
    private val sharedVM: SharedViewModel by activityViewModels()
    private val mAdapter = QuickAdapter(R.layout.reply_list_item)

    private val imageLoader: ImageViewerPopup.ImageLoader by lazy {
        ImageViewerPopup.ImageLoader(requireContext())
    }

    private val dialog: BasePopupView by lazy { CreatePopup(this, requireContext()) }

    private var isFabOpen = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        sharedVM.setFragment(this.javaClass.simpleName)

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

                val url = (adapter.getItem(
                            position
                        ) as Reply).getImgUrl()
                // TODO support multiple image
                val viewerPopup =
                    ImageViewerPopup(
                        this,
                        requireContext(),
                        url
                    )
                viewerPopup.setXPopupImageLoader(imageLoader)
                viewerPopup.setSingleSrcView(view as ImageView?, url)
                viewerPopup.setOnClickListener {
                    Timber.i("on click in thread")
                }
                XPopup.Builder(context)

                    .asCustom(viewerPopup)

                    .show()
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

        binding.replysView.setOnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
            if (scrollY < oldScrollY) {
                binding.fabMenu.show()
            } else {
                binding.fabMenu.hide()
            }
        }

        binding.fabMenu.setOnClickListener {
            toggleMenu()
        }

        binding.copyId.setOnClickListener {
            Timber.i("Clicked on copy Id")
            toggleMenu()
        }

        binding.create.setOnClickListener {
            toggleMenu()

            XPopup.Builder(context)
                .asCustom(dialog)
                .show()
        }

        binding.jump.setOnClickListener {
            Timber.i("Clicked on jump")
            toggleMenu()
        }

        binding.onlyPo.setOnClickListener {
            Timber.i("Clicked on onlyPo")
            toggleMenu()
        }


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

    private fun toggleMenu() {
        val rotateForward = AnimationUtils.loadAnimation(requireContext(), R.anim.rotate_forward);
        val rotateBackward = AnimationUtils.loadAnimation(requireContext(), R.anim.rotate_backward);
        if (isFabOpen) {
            binding.fabMenu.startAnimation(rotateBackward)

            binding.create.hide()
            binding.jump.hide()
            binding.copyId.hide()
            binding.onlyPo.hide()

            isFabOpen = false
        } else {
            binding.fabMenu.startAnimation(rotateForward)

            binding.create.show()
            binding.jump.show()
            binding.copyId.show()
            binding.onlyPo.show()
            isFabOpen = true
        }
    }
}

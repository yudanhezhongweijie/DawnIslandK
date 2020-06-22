package com.laotoua.dawnislandk.screens.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.chad.library.adapter.base.BaseBinderAdapter
import com.chad.library.adapter.base.binder.QuickItemBinder
import com.chad.library.adapter.base.util.getItemView
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.google.android.material.card.MaterialCardView
import com.laotoua.dawnislandk.R
import com.laotoua.dawnislandk.data.local.entity.Post
import com.laotoua.dawnislandk.databinding.FragmentBrowsingHistoryBinding
import com.laotoua.dawnislandk.screens.MainActivity
import com.laotoua.dawnislandk.screens.SharedViewModel
import com.laotoua.dawnislandk.screens.adapters.*
import com.laotoua.dawnislandk.screens.posts.PostCardFactory
import com.laotoua.dawnislandk.screens.util.ToolBar.immersiveToolbar
import com.laotoua.dawnislandk.util.ReadableTime
import dagger.android.support.DaggerFragment
import timber.log.Timber
import javax.inject.Inject

class BrowsingHistoryFragment : DaggerFragment() {

    private var _binding: FragmentBrowsingHistoryBinding? = null
    private val binding: FragmentBrowsingHistoryBinding get() = _binding!!

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val viewModel: BrowsingHistoryViewModel by viewModels { viewModelFactory }
    private val sharedVM: SharedViewModel by activityViewModels { viewModelFactory }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentBrowsingHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.apply {
            immersiveToolbar()
            setTitle(R.string.browsing_history)
            setSubtitle(R.string.toolbar_subtitle)
            setNavigationIcon(R.drawable.ic_arrow_back_white_24px)
            setNavigationOnClickListener {
                findNavController().popBackStack()
            }
        }

        val mAdapter = BaseBinderAdapter().apply {
            addItemBinder(DateStringBinder())
            addItemBinder(PostBinder(sharedVM))
        }

        binding.recyclerView.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            adapter = mAdapter
        }
        viewModel.browsingHistoryList.observe(viewLifecycleOwner, Observer { list ->
            var lastDate: Long? = null
            val data: MutableList<Any> = ArrayList()
            list.map {
                if (lastDate == null || it.browsingHistory.date != lastDate) {
                    data.add(
                        ReadableTime.getDateString(
                            it.browsingHistory.date,
                            ReadableTime.DATE_FORMAT_WITH_YEAR
                        )
                    )
                }
                data.add(it.post)
                lastDate = it.browsingHistory.date

            }
            mAdapter.setDiffNewData(data)
            Timber.i("${this.javaClass.simpleName} Adapter will have ${list.size} threads")
        })
    }

    private class DateStringBinder : QuickItemBinder<String>() {
        override fun convert(holder: BaseViewHolder, data: String) {
            holder.setText(R.id.date, data)
        }

        override fun getLayoutId(): Int = R.layout.list_item_browsing_history_date
    }

    private class PostBinder(private val sharedViewModel: SharedViewModel) :
        QuickItemBinder<Post>() {
        override fun convert(holder: BaseViewHolder, data: Post) {
            holder.convertUserId(data.userid, data.admin)
            holder.convertTitleAndName(data.getSimplifiedTitle(), data.getSimplifiedName())
            holder.convertRefId(context, data.id)
            holder.convertTimeStamp(data.now)
            holder.convertForumAndReplyCount(
                data.replyCount,
                sharedViewModel.getForumDisplayName(data.fid)
            )
            holder.convertSage(data.sage)
            holder.convertImage(data.getImgUrl())
            holder.convertContent(context, data.content)
        }

        override fun getLayoutId(): Int = R.layout.list_item_post

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
            val view = parent.getItemView(getLayoutId()).applyTextSizeAndLetterSpacing()
            PostCardFactory.applySettings(view as MaterialCardView)
            return BaseViewHolder(view)
        }

        override fun onClick(holder: BaseViewHolder, view: View, data: Post, position: Int) {
            sharedViewModel.setPost(data.id,data.fid)
            (context as MainActivity).showComment()
        }
    }
}
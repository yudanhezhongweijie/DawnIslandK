package com.laotoua.dawnislandk.screens.history

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.datetime.datePicker
import com.chad.library.adapter.base.BaseBinderAdapter
import com.chad.library.adapter.base.binder.QuickItemBinder
import com.chad.library.adapter.base.util.getItemView
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.google.android.material.animation.AnimationUtils
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
import com.laotoua.dawnislandk.util.lazyOnMainOnly
import dagger.android.support.DaggerFragment
import timber.log.Timber
import java.util.*
import javax.inject.Inject
import kotlin.collections.ArrayList

class BrowsingHistoryFragment : DaggerFragment() {

    private var _binding: FragmentBrowsingHistoryBinding? = null
    private val binding: FragmentBrowsingHistoryBinding get() = _binding!!

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private val viewModel: BrowsingHistoryViewModel by viewModels { viewModelFactory }
    private val sharedVM: SharedViewModel by activityViewModels { viewModelFactory }

    private var endDate = Calendar.getInstance()
    private var startDate = Calendar.getInstance().apply { add(Calendar.WEEK_OF_YEAR, -1) }

    enum class SCROLL_STATE {
        UP,
        DOWN
    }

    private var currentState: SCROLL_STATE? = null
    private var currentAnimatorSet: AnimatorSet? = null

    private val slideOutBottom by lazyOnMainOnly {
        ObjectAnimator.ofFloat(
            binding.bottomToolbar,
            "TranslationY",
            binding.bottomToolbar.height.toFloat()
        )
    }

    private val alphaOut by lazyOnMainOnly {
        ObjectAnimator.ofFloat(binding.bottomToolbar, "alpha", 0f)
    }

    private val slideInBottom by lazyOnMainOnly {
        ObjectAnimator.ofFloat(
            binding.bottomToolbar,
            "TranslationY",
            0f
        )
    }

    private val alphaIn by lazyOnMainOnly {
        ObjectAnimator.ofFloat(binding.bottomToolbar, "alpha", 1f)
    }

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
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    if (dy > 0) {
                        hideMenu()
                    } else if (dy < 0) {
                        showMenu()
                    }
                }
            })
        }

        viewModel.browsingHistoryList.observe(viewLifecycleOwner, Observer { list ->
            if (list.isEmpty()) {
                if (!mAdapter.hasEmptyView()) mAdapter.setEmptyView(R.layout.view_no_data)
                mAdapter.setDiffNewData(null)
                return@Observer
            }
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

        binding.startDate.text = ReadableTime.getDateString(startDate.time)
        binding.endDate.text = ReadableTime.getDateString(endDate.time)
        binding.startDate.setOnClickListener {
            MaterialDialog(requireContext()).show {
                cornerRadius(res = R.dimen.dp_10)
                datePicker(currentDate = startDate) { _, date ->
                    setStartDate(date)
                }
            }
        }

        binding.endDate.setOnClickListener {
            MaterialDialog(requireContext()).show {
                cornerRadius(res = R.dimen.dp_10)
                datePicker(currentDate = endDate) { _, date ->
                    setEndDate(date)
                }
            }
        }

        binding.confirmDate.setOnClickListener {
            if (startDate.before(endDate)) {
                viewModel.searchByDate()
            } else {
                Toast.makeText(context, R.string.data_range_selection_error, Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private fun setStartDate(date: Calendar) {
        startDate = date
        viewModel.setStartDate(date.time)
        binding.startDate.text = ReadableTime.getDateString(date.time)
    }

    private fun setEndDate(date: Calendar) {
        endDate = date
        viewModel.setEndDate(date.time)
        binding.endDate.text = ReadableTime.getDateString(date.time)
    }

    fun hideMenu() {
        if (currentState == SCROLL_STATE.DOWN) return
        if (currentAnimatorSet != null) {
            currentAnimatorSet!!.cancel()
        }
        currentState = SCROLL_STATE.DOWN
        currentAnimatorSet = AnimatorSet().apply {
            duration = 250
            interpolator = AnimationUtils.FAST_OUT_LINEAR_IN_INTERPOLATOR
            addListener(object : Animator.AnimatorListener {
                override fun onAnimationRepeat(animation: Animator?) {}
                override fun onAnimationEnd(animation: Animator?) {
                    currentAnimatorSet = null
                }

                override fun onAnimationCancel(animation: Animator?) {}
                override fun onAnimationStart(animation: Animator?) {}
            })
            playTogether(slideOutBottom, alphaOut)
            start()
        }
    }

    fun showMenu() {
        if (currentState == SCROLL_STATE.UP) return
        if (currentAnimatorSet != null) {
            currentAnimatorSet!!.cancel()
        }
        currentState = SCROLL_STATE.UP
        currentAnimatorSet = AnimatorSet().apply {
            duration = 250
            interpolator = AnimationUtils.LINEAR_OUT_SLOW_IN_INTERPOLATOR
            addListener(object : Animator.AnimatorListener {
                override fun onAnimationRepeat(animation: Animator?) {}
                override fun onAnimationEnd(animation: Animator?) {
                    currentAnimatorSet = null
                }

                override fun onAnimationCancel(animation: Animator?) {}
                override fun onAnimationStart(animation: Animator?) {}
            })
            playTogether(slideInBottom, alphaIn)
            start()
        }
    }

    private class DateStringBinder : QuickItemBinder<String>() {
        override fun convert(holder: BaseViewHolder, data: String) {
            holder.setText(R.id.text, data)
        }

        override fun getLayoutId(): Int = R.layout.list_item_simple_text
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
            sharedViewModel.setPost(data.id, data.fid)
            (context as MainActivity).showComment()
        }
    }
}
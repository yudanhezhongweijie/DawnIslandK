package com.laotoua.dawnislandk.unused

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import com.google.android.material.animation.AnimationUtils.FAST_OUT_LINEAR_IN_INTERPOLATOR
import com.google.android.material.animation.AnimationUtils.LINEAR_OUT_SLOW_IN_INTERPOLATOR
import com.laotoua.dawnislandk.R
import com.laotoua.dawnislandk.databinding.FragmentPagerBinding
import com.laotoua.dawnislandk.screens.MainActivity
import com.laotoua.dawnislandk.screens.SharedViewModel
import com.laotoua.dawnislandk.screens.feeds.FeedsFragment
import com.laotoua.dawnislandk.screens.feeds.TrendsFragment
import com.laotoua.dawnislandk.screens.posts.PostsFragment
import com.laotoua.dawnislandk.screens.util.ToolBar.immersiveToolbar
import com.laotoua.dawnislandk.screens.widget.popup.PostPopup
import com.laotoua.dawnislandk.util.lazyOnMainOnly
import com.zhpan.indicator.enums.IndicatorSlideMode
import com.zhpan.indicator.enums.IndicatorStyle
import dagger.android.support.DaggerFragment
import timber.log.Timber
import javax.inject.Inject


class PagerFragment : DaggerFragment() {

    private var _binding: FragmentPagerBinding? = null
    private val binding: FragmentPagerBinding get() = _binding!!

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    private val sharedVM: SharedViewModel by activityViewModels { viewModelFactory }
    private var mForumId: String? = null

    enum class SCROLL_STATE {
        UP,
        DOWN
    }

    private var currentState: SCROLL_STATE? = null
    private var currentAnimatorSet: AnimatorSet? = null

    private val titleUpdateCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            super.onPageSelected(position)
            when (position) {
                0 -> updateTitle()
                1 -> updateTitle(R.string.trend)
                2 -> updateTitle(R.string.my_feed)
            }
        }
    }


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


    private val slideInLeftAnimation by lazyOnMainOnly {
        AnimationUtils.loadAnimation(
            requireContext(),
            R.anim.slide_in_left
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentPagerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toolbar.apply {
            immersiveToolbar()
            setSubtitle(R.string.toolbar_subtitle)
        }
        /**
         *  fragment navigation within cause memory leak
         *  https://issuetracker.google.com/issues/154751401
         *  https://issuetracker.google.com/issues/151212195
         */
//        binding.viewPagerInterceptor.bindPager2(binding.viewPager2)
//        binding.viewPagerInterceptor.bindDrawerListener { drawerLayout.openDrawer(GravityCompat.START) }
        /** workaround for https://issuetracker.google.com/issues/134912610
         *  programmatically remove over scroll edge effect
         */
        (binding.viewPager2.getChildAt(0) as RecyclerView).overScrollMode = View.OVER_SCROLL_NEVER


        binding.viewPager2.adapter =
            object : FragmentStateAdapter(childFragmentManager, viewLifecycleOwner.lifecycle) {
                override fun getItemCount(): Int = 3
                override fun createFragment(position: Int): Fragment {
                    return when (position) {
                        0 -> PostsFragment()
                        1 -> TrendsFragment()
                        2 -> FeedsFragment()
                        else -> throw Exception("unhandled pager fragment creation")
                    }
                }
            }

        binding.viewPager2.registerOnPageChangeCallback(titleUpdateCallback)
//
//        binding.viewPager.bindDrawerListener { drawerLayout.open() }
//        binding.viewPager.adapter = object : FragmentPagerAdapter(childFragmentManager) {
//            override fun getCount(): Int = 3
//            override fun getItem(position: Int): Fragment {
//                return when (position) {
//                    0 -> ThreadsFragment()
//                    1 -> TrendsFragment()
//                    2 -> FeedsFragment()
//                    else -> throw Exception("unhandled pager fragment creation")
//                }
//            }
//        }

        binding.forumRule.setOnClickListener {
            MaterialDialog(requireContext()).show {
                val forumId = sharedVM.selectedForumId.value!!
                val biId = if (forumId.toInt() > 0) forumId.toInt() else 1
                val resourceId: Int = context.resources.getIdentifier(
                    "bi_$biId", "drawable",
                    context.packageName
                )
                icon(resourceId)
                title(text = sharedVM.getForumDisplayName(forumId))
                message(text = sharedVM.getForumMsg(forumId)) { html() }
                positiveButton(R.string.acknowledge)
            }
        }

        binding.pageIndicatorView
            .setSliderColor(
                requireContext().getColor(R.color.lime_500),
                requireContext().getColor(R.color.pure_light)
            )
            .setSliderWidth(requireContext().resources.getDimension(R.dimen.dp_10))
            .setSliderHeight(requireContext().resources.getDimension(R.dimen.dp_10))
            .setSliderGap(requireContext().resources.getDimension(R.dimen.dp_8))
            .setSlideMode(IndicatorSlideMode.WORM)
            .setIndicatorStyle(IndicatorStyle.CIRCLE)
            .setupWithViewPager(binding.viewPager2)

        sharedVM.selectedForumId.observe(viewLifecycleOwner, Observer {
            updateTitle()
            if (mForumId != it) {
                binding.viewPager2.currentItem = 0
                mForumId = it
            }
        })

        val postPopup: PostPopup by lazyOnMainOnly { PostPopup(this, requireContext(), sharedVM) }
        binding.post.setOnClickListener {
            postPopup.setupAndShow(
                sharedVM.selectedForumId.value,
                sharedVM.selectedForumId.value!!,
                true
            )
        }

        binding.search.setOnClickListener {
            MaterialDialog(requireContext()).show {
                title(R.string.search)
                customView(R.layout.dialog_search, noVerticalPadding = true).apply {
                    findViewById<Button>(R.id.search).setOnClickListener {
                        Toast.makeText(context, "还没做。。。", Toast.LENGTH_SHORT).show()
                    }

                    findViewById<Button>(R.id.jumpToPost).setOnClickListener {
                        val threadId = findViewById<TextView>(R.id.searchInputText).text
                            .filter { it.isDigit() }.toString()
                        if (threadId.isNotEmpty()) {
                            // Does not have fid here. fid will be generated when data comes back in reply
                            sharedVM.setPost(threadId, "")
                            dismiss()
                            (requireActivity() as MainActivity).showComment()
                        } else {
                            Toast.makeText(
                                context,
                                R.string.please_input_valid_text,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.viewPager2.unregisterOnPageChangeCallback(titleUpdateCallback)
//        binding.viewPagerInterceptor.clearPager2()
        _binding = null
        Timber.d("Fragment View Destroyed")
    }


    fun updateTitle(resId: Int? = null) {
        binding.toolbar.startAnimation(slideInLeftAnimation)
        if (resId != null) binding.toolbar.setTitle(resId)
        else binding.toolbar.title = sharedVM.getToolbarTitle()
    }

    fun setToolbarClickListener(listener: (View) -> Unit) {
        binding.toolbar.setOnClickListener {
            listener.invoke(it)
        }
    }


    fun hideMenu() {
        if (currentState == SCROLL_STATE.DOWN) return
        if (currentAnimatorSet != null) {
            currentAnimatorSet!!.cancel()
        }
        currentState =
            SCROLL_STATE.DOWN
        currentAnimatorSet = AnimatorSet().apply {
            duration = 250
            interpolator = FAST_OUT_LINEAR_IN_INTERPOLATOR
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
        currentState =
            SCROLL_STATE.UP
        currentAnimatorSet = AnimatorSet().apply {
            duration = 250
            interpolator = LINEAR_OUT_SLOW_IN_INTERPOLATOR
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
}

package com.laotoua.dawnislandk.screens

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentPagerAdapter
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.laotoua.dawnislandk.R
import com.laotoua.dawnislandk.databinding.FragmentPagerBinding
import com.laotoua.dawnislandk.screens.feeds.FeedsFragment
import com.laotoua.dawnislandk.screens.threads.ThreadsFragment
import com.laotoua.dawnislandk.screens.trend.TrendsFragment
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
    private val sharedVM: SharedViewModel by activityViewModels()
    private var mForumId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentPagerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val drawerLayout = requireActivity().findViewById<DrawerLayout>(R.id.drawerLayout)
        /**
         *  Downgrading ViewPager2 because of fragment navigation within cause memory leak
         *  https://issuetracker.google.com/issues/154751401
         */
//        val scaledMinimumFlingVelocity = ViewConfiguration.get(context).scaledMinimumFlingVelocity
//        val gestureListener = object : GestureDetector.SimpleOnGestureListener() {
//            override fun onFling(
//                event1: MotionEvent,
//                event2: MotionEvent,
//                velocityX: Float,
//                velocityY: Float
//            ): Boolean {
//                val res = (binding.viewPager.currentItem == 0
//                        && event2.x - event1.x > minSwipeDist
//                        && velocityX >= scaledMinimumFlingVelocity)
//                if (res) {
//                    drawerLayout.open()
//                }
//                return res
//            }
//        }
//
//        binding.viewPagerInterceptor.bindGestureDetector(gestureListener)
//        /** workaround for https://issuetracker.google.com/issues/134912610
//         *  programmatically remove over scroll edge effect
//         */
//        (binding.viewPager.getChildAt(0) as RecyclerView).overScrollMode = View.OVER_SCROLL_NEVER

        binding.viewPager.bindDrawerListener { drawerLayout.open() }
        binding.viewPager.adapter = object : FragmentPagerAdapter(childFragmentManager) {
            override fun getCount(): Int = 3
            override fun getItem(position: Int): Fragment {
                return when (position) {
                    0 -> ThreadsFragment()
                    1 -> TrendsFragment()
                    2 -> FeedsFragment()
                    else -> throw Exception("unhandled pager fragment creation")
                }
            }
        }

        binding.drawerIndicator.setColorFilter(requireContext().getColor(R.color.lime_500))
        binding.pageIndicatorView
            .setSliderColor(
                requireContext().getColor(R.color.lime_500),
                requireContext().getColor(R.color.teal_500)
            )
            .setSlideMode(IndicatorSlideMode.WORM)
            .setIndicatorStyle(IndicatorStyle.CIRCLE)
            .setupWithViewPager(binding.viewPager)

        sharedVM.selectedForum.observe(viewLifecycleOwner, Observer {
            if (mForumId != it.id) {
                binding.viewPager.currentItem = 0
                mForumId = it.id
            }
        })

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        Timber.d("Fragment View Destroyed")
    }

    private val fadeOut: Animation by lazyOnMainOnly {
        AnimationUtils.loadAnimation(
            requireContext(),
            R.anim.fade_out
        )
    }

    private val fadeIn: Animation by lazyOnMainOnly {
        AnimationUtils.loadAnimation(
            requireContext(),
            R.anim.fade_in
        )
    }
    private var indicatorHidden = false
    fun hidePageIndicator() {
        if (!indicatorHidden) {
            binding.drawerIndicator.startAnimation(fadeOut)
            binding.pageIndicatorView.startAnimation(fadeOut)
            binding.drawerIndicator.visibility = View.GONE
            binding.pageIndicatorView.visibility = View.GONE
            indicatorHidden = true
        }
    }

    fun showPageIndicator() {
        if (indicatorHidden) {
            binding.drawerIndicator.startAnimation(fadeIn)
            binding.pageIndicatorView.startAnimation(fadeIn)
            binding.drawerIndicator.visibility = View.VISIBLE
            binding.pageIndicatorView.visibility = View.VISIBLE
            indicatorHidden = false
        }
    }
}

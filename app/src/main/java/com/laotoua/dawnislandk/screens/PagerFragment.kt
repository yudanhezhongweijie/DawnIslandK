package com.laotoua.dawnislandk.screens

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.laotoua.dawnislandk.R
import com.laotoua.dawnislandk.databinding.FragmentPagerBinding
import com.laotoua.dawnislandk.screens.feeds.FeedsFragment
import com.laotoua.dawnislandk.screens.replys.ReplysFragment
import com.laotoua.dawnislandk.screens.threads.ThreadsFragment
import com.laotoua.dawnislandk.screens.trend.TrendsFragment
import com.laotoua.dawnislandk.screens.util.ToolBar.immersiveToolbar
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
        binding.toolbar.apply {
            immersiveToolbar()
            setSubtitle(R.string.adnmb)
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
            setNavigationIcon(R.drawable.ic_menu_white_24px)
            setNavigationOnClickListener {
                drawerLayout.openDrawer(GravityCompat.START)
            }
        }
        /**
         *  fragment navigation within cause memory leak
         *  https://issuetracker.google.com/issues/154751401
         *  https://issuetracker.google.com/issues/151212195
         */
        binding.viewPagerInterceptor.bindPager2(binding.viewPager2)
        binding.viewPagerInterceptor.bindDrawerListener { drawerLayout.open() }
        /** workaround for https://issuetracker.google.com/issues/134912610
         *  programmatically remove over scroll edge effect
         */
        (binding.viewPager2.getChildAt(0) as RecyclerView).overScrollMode = View.OVER_SCROLL_NEVER


        binding.viewPager2.adapter =
            object : FragmentStateAdapter(childFragmentManager, viewLifecycleOwner.lifecycle) {
                override fun getItemCount(): Int = 3
                override fun createFragment(position: Int): Fragment {
                    return when (position) {
                        0 -> ThreadsFragment()
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

        binding.settings.setOnClickListener {
            val action =
                PagerFragmentDirections.actionPagerFragmentToSettingsFragment()
            findNavController().navigate(action)
        }

        binding.pageIndicatorView
            .setSliderColor(
                requireContext().getColor(R.color.lime_500),
                requireContext().getColor(R.color.pure_light)
            )
            .setSliderWidth(requireContext().resources.getDimension(R.dimen.vp2_indicator_width))
            .setSliderHeight(requireContext().resources.getDimension(R.dimen.vp2_indicator_height))
            .setSliderGap(requireContext().resources.getDimension(R.dimen.vp2_indicator_gap))
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

    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.viewPager2.unregisterOnPageChangeCallback(titleUpdateCallback)
        binding.viewPagerInterceptor.clearPager2()
        _binding = null
        Timber.d("Fragment View Destroyed")
    }

    private val slideInLeftAnimation by lazyOnMainOnly {
        AnimationUtils.loadAnimation(
            requireContext(),
            R.anim.slide_in_left
        )
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
}

package com.laotoua.dawnislandk.screens.feeds

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import com.laotoua.dawnislandk.DawnApp
import com.laotoua.dawnislandk.R
import com.laotoua.dawnislandk.databinding.FragmentFeedPagerBinding
import com.laotoua.dawnislandk.screens.util.ToolBar.immersiveToolbar
import com.laotoua.dawnislandk.util.lazyOnMainOnly
import com.zhpan.indicator.enums.IndicatorSlideMode
import com.zhpan.indicator.enums.IndicatorStyle
import dagger.android.support.DaggerFragment
import timber.log.Timber

class FeedPagerFragment : DaggerFragment() {
    private var _binding: FragmentFeedPagerBinding? = null
    private val binding: FragmentFeedPagerBinding get() = _binding!!

    private val pageIndices = DawnApp.applicationDataStore.getFeedPagerPageIndices()
    private val titleUpdateCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            super.onPageSelected(position)
            when (position) {
                pageIndices.first -> updateTitle(R.string.my_feed)
                pageIndices.second -> updateTitle(R.string.trend)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentFeedPagerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toolbar.apply {
            immersiveToolbar()
            setSubtitle(R.string.toolbar_subtitle)
        }

        /** workaround for https://issuetracker.google.com/issues/134912610
         *  programmatically remove over scroll edge effect
         */
        (binding.viewPager2.getChildAt(0) as RecyclerView).overScrollMode = View.OVER_SCROLL_NEVER

        binding.viewPager2.adapter =
            object : FragmentStateAdapter(childFragmentManager, viewLifecycleOwner.lifecycle) {
                override fun getItemCount(): Int = 2
                override fun createFragment(position: Int): Fragment {
                    return when (position) {
                        pageIndices.first -> FeedsFragment()
                        pageIndices.second -> TrendsFragment()
                        else -> throw Exception("unhandled pager fragment creation")
                    }
                }
            }

        binding.viewPager2.registerOnPageChangeCallback(titleUpdateCallback)

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

        binding.pageEditor.setOnClickListener {
            val items = listOf(
                requireContext().getString(R.string.trend),
                requireContext().getString(R.string.my_feed)
            )
            MaterialDialog(requireContext()).show {
                title(R.string.edit_default_page)
                listItemsSingleChoice(items = items) { _, index, _ ->
                    DawnApp.applicationDataStore.setFeedPagerDefaultPage(1 - index, index)
                    Toast.makeText(context,R.string.restart_to_apply_setting, Toast.LENGTH_SHORT).show()
                }
                positiveButton(R.string.submit)
                negativeButton(R.string.cancel)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.viewPager2.unregisterOnPageChangeCallback(titleUpdateCallback)
        _binding = null
        Timber.d("Fragment View Destroyed")
    }

    private val slideInLeftAnimation by lazyOnMainOnly {
        AnimationUtils.loadAnimation(
            requireContext(),
            R.anim.slide_in_left
        )
    }

    fun updateTitle(resId: Int) {
        binding.toolbar.run {
            startAnimation(slideInLeftAnimation)
            setTitle(resId)
        }
    }

    fun setToolbarClickListener(listener: (View) -> Unit) {
        binding.toolbar.setOnClickListener {
            listener.invoke(it)
        }
    }

}
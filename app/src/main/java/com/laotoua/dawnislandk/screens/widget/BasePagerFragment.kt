package com.laotoua.dawnislandk.screens.widget

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.laotoua.dawnislandk.R
import com.laotoua.dawnislandk.databinding.FragmentBasePagerBinding
import com.laotoua.dawnislandk.screens.util.ToolBar.immersiveToolbar
import com.laotoua.dawnislandk.util.lazyOnMainOnly
import com.zhpan.indicator.enums.IndicatorSlideMode
import com.zhpan.indicator.enums.IndicatorStyle
import dagger.android.support.DaggerFragment
import timber.log.Timber

abstract class BasePagerFragment : DaggerFragment() {
    private var _binding: FragmentBasePagerBinding? = null
    private val binding: FragmentBasePagerBinding get() = _binding!!

    abstract val pageTitleResIds: Map<Int,Int>
    abstract val pageFragmentClass: Map<Int, Class<out BaseNavFragment>>
    abstract val pageEditorClickListener: View.OnClickListener

    private val titleUpdateCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            super.onPageSelected(position)
            updateTitle(pageTitleResIds[position] ?: error("Missing title ResIds"))
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        if (pageFragmentClass.size != pageTitleResIds.size) {
            throw Exception("Page Assertion failed")
        }
        _binding = FragmentBasePagerBinding.inflate(inflater, container, false)
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
                override fun getItemCount(): Int = pageFragmentClass.size
                override fun createFragment(position: Int): Fragment {
                    return pageFragmentClass[position]?.newInstance() ?: error("Missing Fragment Class")
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

        binding.pageEditor.setOnClickListener(pageEditorClickListener)
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

    fun setToolbarClickListener(listener: () -> Unit) {
        binding.toolbar.setOnClickListener(
            DoubleClickListener(callback = object : DoubleClickListener.DoubleClickCallBack {
                override fun doubleClicked() {
                    listener.invoke()
                }
            })
        )
    }

}
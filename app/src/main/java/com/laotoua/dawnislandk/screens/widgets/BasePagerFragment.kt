/*
 *  Copyright 2020 Fishballzzz
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package com.laotoua.dawnislandk.screens.widgets

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.laotoua.dawnislandk.R
import com.laotoua.dawnislandk.databinding.FragmentBasePagerBinding
import com.laotoua.dawnislandk.screens.MainActivity
import com.zhpan.indicator.IndicatorView
import com.zhpan.indicator.enums.IndicatorSlideMode
import com.zhpan.indicator.enums.IndicatorStyle
import dagger.android.support.DaggerFragment
import timber.log.Timber

abstract class BasePagerFragment : DaggerFragment() {
    private var _binding: FragmentBasePagerBinding? = null
    private val binding: FragmentBasePagerBinding get() = _binding!!

    abstract val pageTitleResIds: Map<Int, Int>
    abstract val pageFragmentClass: Map<Int, Class<out BaseNavFragment>>
    abstract val pageEditorClickListener: View.OnClickListener

    private val titleUpdateCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            super.onPageSelected(position)
            updateTitle(pageTitleResIds[position] ?: error("Missing title ResIds"))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_fragment_base_pager, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        menu.findItem(R.id.pageIndicator)?.actionView?.findViewById<IndicatorView>(R.id.pageIndicatorView)
            ?.apply {
                setSliderColor(
                    requireContext().getColor(R.color.lime_500),
                    requireContext().getColor(R.color.pure_light)
                )
                setSliderWidth(requireContext().resources.getDimension(R.dimen.dp_10))
                setSliderHeight(requireContext().resources.getDimension(R.dimen.dp_10))
                setSliderGap(requireContext().resources.getDimension(R.dimen.dp_8))
                setSlideMode(IndicatorSlideMode.WORM)
                setIndicatorStyle(IndicatorStyle.CIRCLE)
                setupWithViewPager(binding.viewPager2)
            }
        super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.settings -> {
                pageEditorClickListener.onClick(item.actionView)
                true
            }
            else -> {
                super.onOptionsItemSelected(item)
            }
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

        /** workaround for https://issuetracker.google.com/issues/134912610
         *  programmatically remove over scroll edge effect
         */
        (binding.viewPager2.getChildAt(0) as RecyclerView).overScrollMode = View.OVER_SCROLL_NEVER

        binding.viewPager2.adapter =
            object : FragmentStateAdapter(childFragmentManager, viewLifecycleOwner.lifecycle) {
                override fun getItemCount(): Int = pageFragmentClass.size
                override fun createFragment(position: Int): Fragment {
                    return pageFragmentClass[position]?.newInstance()
                        ?: error("Missing Fragment Class")
                }
            }

        binding.viewPager2.registerOnPageChangeCallback(titleUpdateCallback)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.viewPager2.unregisterOnPageChangeCallback(titleUpdateCallback)
        _binding = null
        Timber.d("Pager View Destroyed")
    }

    fun updateTitle(resId: Int) {
        (requireActivity() as MainActivity).setToolbarTitle(resId)
    }

}
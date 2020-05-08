package com.laotoua.dawnislandk.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.laotoua.dawnislandk.databinding.FragmentPagerBinding
import com.laotoua.dawnislandk.viewmodel.SharedViewModel
import timber.log.Timber


class PagerFragment : Fragment() {

    private var _binding: FragmentPagerBinding? = null
    private val binding: FragmentPagerBinding get() = _binding!!

    private val sharedVM: SharedViewModel by activityViewModels()

    private var mForumId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentPagerBinding.inflate(inflater, container, false)
        binding.viewPager.adapter = object : FragmentStateAdapter(this) {
            private val mFragmentList: MutableList<Fragment> = mutableListOf()

            fun getItem(position: Int): Fragment {
                return mFragmentList[position]
            }

            override fun getItemCount(): Int {
                return mFragmentList.size
            }

            override fun createFragment(position: Int): Fragment {
                return when (position) {
                    0 -> ThreadFragment()
                    1 -> TrendFragment()
                    2 -> FeedFragment()
                    else -> throw Exception("unhandled pager fragment creation")
                }
            }

            init {
                mFragmentList.add(ThreadFragment())
                mFragmentList.add(TrendFragment())
                mFragmentList.add(FeedFragment())
            }
        }.also { adapter ->
            binding.viewPager.registerOnPageChangeCallback(object :
                ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    sharedVM.setFragment(adapter.getItem(position))
                    super.onPageSelected(position)
                }
            })
        }

        sharedVM.selectedForum.observe(viewLifecycleOwner, Observer {
            if (mForumId != it.id) {
                binding.viewPager.currentItem = 0
                mForumId = it.id
            }
        })

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        Timber.d("Fragment View Destroyed")
    }
}

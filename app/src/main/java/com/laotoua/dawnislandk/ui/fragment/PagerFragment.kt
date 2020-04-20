package com.laotoua.dawnislandk.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.laotoua.dawnislandk.databinding.PagerFragmentBinding
import com.laotoua.dawnislandk.viewmodel.SharedViewModel
import timber.log.Timber


class PagerFragment : Fragment() {

    private var _binding: PagerFragmentBinding? = null
    private val binding: PagerFragmentBinding get() = _binding!!

    private val sharedVM: SharedViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = PagerFragmentBinding.inflate(inflater, container, false)
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
                    1 -> FeedFragment()
                    else -> throw Exception("unhandled pager fragment creation")
                }
            }

            init {
                mFragmentList.add(ThreadFragment())
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

        return binding.root
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.i("Pager fragment destroyed!")
    }


}

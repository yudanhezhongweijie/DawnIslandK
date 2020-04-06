package com.laotoua.dawnislandk

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.laotoua.dawnislandk.databinding.PagerFragmentBinding
import timber.log.Timber


class PagerFragment : Fragment() {

    private var _binding: PagerFragmentBinding? = null
    private val binding: PagerFragmentBinding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
//        sharedVM.setFragment(this.javaClass.simpleName)

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
                    1 -> SubscriptionFragment()
                    else -> throw Exception("unhandled pager fragment creation")
                }
            }

            fun getItemByTag(tag: String): Fragment {
                return when (tag) {
                    "Thread" -> getItem(0)
                    "Subscription" -> getItem(1)
                    else -> {
                        Timber.e("Unhandled get fragment by tag")
                        throw Exception("Unhandled get fragment by tag")
                    }
                }
            }

            init {
                mFragmentList.add(ThreadFragment())
                mFragmentList.add(SubscriptionFragment())
                Timber.i("Pager init~")
            }
        }


        return binding.root
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.i("Pager fragment destroyed!")
    }


}

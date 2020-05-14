package com.laotoua.dawnislandk.ui.fragment

import android.os.Bundle
import android.view.*
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.laotoua.dawnislandk.R
import com.laotoua.dawnislandk.databinding.FragmentPagerBinding
import com.laotoua.dawnislandk.viewmodel.SharedViewModel
import timber.log.Timber


class PagerFragment : Fragment() {

    private var _binding: FragmentPagerBinding? = null
    private val binding: FragmentPagerBinding get() = _binding!!

    private val sharedVM: SharedViewModel by activityViewModels()

    private var mForumId: String? = null

    private val minSwipeDist = 120

    private val viewConfiguration by lazy { ViewConfiguration.get(context) }

    private val drawerLayout by lazy { requireActivity().findViewById<DrawerLayout>(R.id.drawerLayout) }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentPagerBinding.inflate(inflater, container, false)

        val gestureListener = object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(
                event1: MotionEvent,
                event2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                val res = (binding.viewPager.currentItem == 0
                        && event2.x - event1.x > minSwipeDist
                        && velocityX >= viewConfiguration.scaledMinimumFlingVelocity)
                if (res) {
                    drawerLayout.open()
                }
                return res
            }
        }

        binding.viewPagerInterceptor.bindGestureDetector(gestureListener)
        /** workaround for https://issuetracker.google.com/issues/134912610
         *  programmatically remove over scroll edge effect
         */
        (binding.viewPager.getChildAt(0) as RecyclerView).overScrollMode = View.OVER_SCROLL_NEVER

        binding.viewPager.adapter = object : FragmentStateAdapter(this) {
            private val mFragmentList: MutableList<Fragment> = mutableListOf()

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

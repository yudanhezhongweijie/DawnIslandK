package com.laotoua.dawnislandk.screens.widget

import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.RecyclerView
import com.laotoua.dawnislandk.R
import com.laotoua.dawnislandk.di.DaggerViewModelFactory
import com.laotoua.dawnislandk.screens.MainActivity
import com.laotoua.dawnislandk.screens.SharedViewModel
import dagger.android.support.DaggerFragment
import javax.inject.Inject

open class BaseNavFragment:DaggerFragment() {
    @Inject
    lateinit var viewModelFactory: DaggerViewModelFactory
    protected val sharedVM: SharedViewModel by activityViewModels { viewModelFactory }

    private val navBarScrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            if (dy > 0) {
                (requireActivity() as MainActivity).hideNav()
            } else if (dy < 0) {
                (requireActivity() as MainActivity).showNav()
            }
        }
    }
    private var mRecyclerView:RecyclerView? = null

    override fun onStart() {
        super.onStart()
        view?.findViewById<RecyclerView>(R.id.recyclerView)?.run {
            mRecyclerView = this
            addOnScrollListener(navBarScrollListener)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mRecyclerView?.removeOnScrollListener(navBarScrollListener)
        mRecyclerView = null
    }
}
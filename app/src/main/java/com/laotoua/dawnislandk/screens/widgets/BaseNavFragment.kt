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
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.laotoua.dawnislandk.DawnApp
import com.laotoua.dawnislandk.R
import com.laotoua.dawnislandk.screens.MainActivity
import com.laotoua.dawnislandk.screens.SharedViewModel
import dagger.android.support.DaggerFragment
import javax.inject.Inject

open class BaseNavFragment : DaggerFragment() {
    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    protected val sharedVM: SharedViewModel by activityViewModels { viewModelFactory }

    private val navBarScrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            if (mRecyclerView == null) return
            if (dy > 0) {
                (requireActivity() as MainActivity).hideNav()
            } else if (dy < 0) {
                (requireActivity() as MainActivity).showNav()
            }
        }
    }
    private var mRecyclerView: RecyclerView? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mRecyclerView?.removeOnScrollListener(navBarScrollListener)
        view.findViewById<RecyclerView>(R.id.recyclerView)?.run {
            mRecyclerView = this
            addOnScrollListener(navBarScrollListener)
        }
    }

    override fun onResume() {
        super.onResume()
        if (activity != null && isAdded) {
            (requireActivity() as MainActivity).setToolbarClickListener {
                mRecyclerView?.layoutManager?.scrollToPosition(0)
                (requireActivity() as MainActivity).showNav()
            }
            (requireActivity() as MainActivity).showNav()
        }
    }

    override fun onPause() {
        super.onPause()
        mRecyclerView?.stopScroll()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (!DawnApp.applicationDataStore.getViewCaching()) {
            mRecyclerView?.removeOnScrollListener(navBarScrollListener)
            mRecyclerView = null
        }
    }
}
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
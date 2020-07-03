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

package com.laotoua.dawnislandk.screens.adapters

import com.chad.library.adapter.base.BaseBinderAdapter
import com.chad.library.adapter.base.module.LoadMoreModule
import com.laotoua.dawnislandk.DawnApp
import com.laotoua.dawnislandk.R
import com.laotoua.dawnislandk.screens.SharedViewModel
import com.laotoua.dawnislandk.screens.adapters.animators.CustomAnimation1
import com.laotoua.dawnislandk.screens.adapters.animators.CustomAnimation2

class QuickMultiBinder(sharedViewModel: SharedViewModel):BaseBinderAdapter(), LoadMoreModule {

    init {
        // 所有数据加载完成后，是否允许点击（默认为false）
        loadMoreModule.enableLoadMoreEndClick = true

        // 当数据不满一页时，是否继续自动加载（默认为true）
        loadMoreModule.isEnableLoadMoreIfNotFullPage = false

        when (DawnApp.applicationDataStore.animationOption) {
            0 -> {}
            1 -> setAnimationWithDefault(AnimationType.AlphaIn)
            2 -> setAnimationWithDefault(AnimationType.ScaleIn)
            3 -> setAnimationWithDefault(AnimationType.SlideInBottom)
            4 -> setAnimationWithDefault(AnimationType.SlideInLeft)
            5 -> setAnimationWithDefault(AnimationType.SlideInRight)
            6 -> adapterAnimation = CustomAnimation1()
            7 -> adapterAnimation = CustomAnimation2()
            else -> throw Exception("Unhandled Animation Option")
        }
        if (DawnApp.applicationDataStore.animationOption > 0) {
            isAnimationFirstOnly = DawnApp.applicationDataStore.animationFirstOnly
        }

        setEmptyView(R.layout.view_no_data)
        loadMoreModule.loadMoreView = QuickAdapter.DawnLoadMoreView(sharedViewModel)
    }
}
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

package com.laotoua.dawnislandk.screens.subscriptions

import androidx.lifecycle.ViewModel
import com.laotoua.dawnislandk.data.repository.TrendRepository
import timber.log.Timber
import javax.inject.Inject

class TrendsViewModel @Inject constructor(
    private val trendRepo: TrendRepository
) : ViewModel() {

    val maxPage: Int get() = trendRepo.maxPage

    val latestTrends = trendRepo.latestTrends

    fun getLatestTrend() {
        Timber.d("Refreshing Trend...")
        trendRepo.subscribeToRemote()
    }

}

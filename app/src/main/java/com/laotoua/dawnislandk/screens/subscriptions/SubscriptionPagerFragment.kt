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

import com.laotoua.dawnislandk.DawnApp
import com.laotoua.dawnislandk.R
import com.laotoua.dawnislandk.screens.widgets.BaseNavFragment
import com.laotoua.dawnislandk.screens.widgets.BasePagerFragment

class SubscriptionPagerFragment : BasePagerFragment() {
    private val feedIndex = DawnApp.applicationDataStore.getSubscriptionPagerFeedIndex()
    override val pageTitleResIds = mutableMapOf<Int, Int>().apply {
        put(1 - feedIndex, R.string.trend)
        put(feedIndex, R.string.my_feed)
    }

    override val pageFragmentClass = mutableMapOf<Int, Class<out BaseNavFragment>>().apply {
        put(1 - feedIndex, TrendsFragment::class.java)
        put(feedIndex, FeedsFragment::class.java)
    }

}
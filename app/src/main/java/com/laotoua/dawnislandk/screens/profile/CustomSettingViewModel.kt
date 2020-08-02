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

package com.laotoua.dawnislandk.screens.profile

import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import com.laotoua.dawnislandk.data.local.dao.BlockedIdDao
import com.laotoua.dawnislandk.data.local.entity.BlockedId
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import javax.inject.Inject

class CustomSettingViewModel @Inject constructor(
    private val blockedIdDao: BlockedIdDao
) :
    ViewModel() {

    val timelineBlockedForumIds =
        Transformations.map(blockedIdDao.getLiveAllBlockedIds()) { list ->
            list.filter { it.isTimelineBlockedForum() }.map { it.id }
        }

    fun blockForums(forumIds: List<BlockedId>){
        GlobalScope.launch {
            blockedIdDao.updateBlockedForumIds(forumIds)
        }
    }
}
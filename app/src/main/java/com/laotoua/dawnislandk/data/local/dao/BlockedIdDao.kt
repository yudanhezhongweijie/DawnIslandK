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

package com.laotoua.dawnislandk.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.laotoua.dawnislandk.data.local.entity.BlockedId

@Dao
interface BlockedIdDao {

    // returns both blocked post Ids & forum Ids
    @Query("SELECT * From BlockedId")
    suspend fun getAllBlockedIds(): List<BlockedId>

    @Query("SELECT * From BlockedId")
    fun getLiveAllBlockedIds(): LiveData<List<BlockedId>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(blockedId: BlockedId)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(blockedIds: List<BlockedId>)

    @Transaction
    suspend fun updateBlockedForumIds(blockedIds: List<BlockedId>){
        nukeTimelineForumIds()
        if (blockedIds.isNotEmpty()) insertAll(blockedIds)
    }

    @Query("DELETE FROM BlockedId WHERE type=0")
    suspend fun nukeTimelineForumIds()

    @Query("DELETE FROM BlockedId WHERE type=1")
    suspend fun nukeBlockedPostIds()

}
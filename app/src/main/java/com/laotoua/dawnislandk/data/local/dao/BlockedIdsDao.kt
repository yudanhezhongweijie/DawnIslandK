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

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.laotoua.dawnislandk.data.local.entity.BlockedIds

@Dao
interface BlockedIdsDao {

    // returns both blocked post Ids & forum Ids
    @Query("SELECT * From BlockedIds")
    suspend fun getAllBlockedIds(): List<BlockedIds>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(blockedIds: BlockedIds)

    @Query("DELETE FROM BlockedIds WHERE type=0")
    suspend fun nukeTimelineForumIds()

    @Query("DELETE FROM BlockedIds WHERE type=1")
    suspend fun nukeBlockedPostIds()

}
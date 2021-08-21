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
import com.laotoua.dawnislandk.DawnApp
import com.laotoua.dawnislandk.data.local.entity.Community

@Dao
interface CommunityDao {
    @Query("SELECT * FROM Community WHERE domain = :domain ORDER BY sort ASC")
    fun getAll(domain: String = DawnApp.currentDomain): LiveData<List<Community>>

    @Query("SELECT * FROM Community WHERE id=:id AND domain = :domain")
    suspend fun getCommunityById(id: String, domain: String = DawnApp.currentDomain): Community

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(community: Community)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(communityList: List<Community>)

    @Delete
    suspend fun delete(community: Community)

    @Query("DELETE FROM Community")
    fun nukeTable()
}
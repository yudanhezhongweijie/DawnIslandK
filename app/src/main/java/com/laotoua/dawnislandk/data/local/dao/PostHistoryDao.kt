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
import com.laotoua.dawnislandk.data.local.entity.PostHistory

@Dao
interface PostHistoryDao {
    @Query("SELECT * From PostHistory ORDER BY postDate DESC")
    fun getAllPostHistory(): LiveData<List<PostHistory>>

    @Transaction
    @Query("SELECT * From PostHistory WHERE postDate>=:startDate AND postDate<=:endDate ORDER BY postDate DESC ")
    fun getAllPostHistoryInDateRange(startDate:Long, endDate:Long): LiveData<List<PostHistory>>

    @Query("SELECT * From PostHistory WHERE postDate=:date")
    fun getPostHistoryByDate(date:Long): LiveData<List<PostHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPostHistory(browsingHistory: PostHistory)

    @Query("DELETE FROM PostHistory")
    suspend fun nukeTable()
}
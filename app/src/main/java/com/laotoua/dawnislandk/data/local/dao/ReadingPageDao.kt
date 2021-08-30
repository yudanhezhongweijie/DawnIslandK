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
import com.laotoua.dawnislandk.DawnApp
import com.laotoua.dawnislandk.data.local.entity.ReadingPage

@Dao
interface ReadingPageDao {
    @Query("SELECT * From ReadingPage WHERE id=:id AND domain=:domain")
    suspend fun getReadingPageById(id: String, domain: String = DawnApp.currentDomain): ReadingPage?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReadingPage(readingPage: ReadingPage)

    suspend fun insertReadingPageWithTimeStamp(readingPage: ReadingPage) {
        readingPage.setUpdatedTimestamp()
        insertReadingPage(readingPage)
    }

    @Query("DELETE FROM ReadingPage")
    suspend fun nukeTable()
}
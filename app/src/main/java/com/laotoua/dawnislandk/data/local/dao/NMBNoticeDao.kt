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
import com.laotoua.dawnislandk.data.local.entity.NMBNotice
import java.time.LocalDateTime

@Dao
interface NMBNoticeDao {
    @Query("SELECT * From NMBNotice ORDER BY id DESC LIMIT 1")
    suspend fun getLatestNMBNotice(): NMBNotice?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNMBNotice(notice: NMBNotice)

    suspend fun insertNMBNoticeWithTimestamp(notice: NMBNotice) {
        notice.setUpdatedTimestamp()
        insertNMBNotice(notice)
    }

    @Query("UPDATE NMBNotice SET content=:content, enable=:enable, read=:read,lastUpdatedAt=:lastUpdatedAt WHERE date=:date")
    suspend fun updateNMBNotice(
        content: String,
        enable: Boolean,
        read: Boolean,
        date: Long,
        lastUpdatedAt: LocalDateTime
    )

    suspend fun updateNMBNoticeWithTimestamp(
        content: String,
        enable: Boolean,
        read: Boolean,
        date: Long,
        lastUpdatedAt: LocalDateTime = LocalDateTime.now()
    ) {
        updateNMBNotice(content, enable, read, date, lastUpdatedAt)
    }

    @Query("DELETE FROM NMBNotice")
    suspend fun nukeTable()

}
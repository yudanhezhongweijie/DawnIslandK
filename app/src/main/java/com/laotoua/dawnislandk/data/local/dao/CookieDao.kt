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

import androidx.room.*
import com.laotoua.dawnislandk.DawnApp
import com.laotoua.dawnislandk.data.local.entity.Cookie
import java.time.LocalDateTime


@Dao
interface CookieDao {
    @Query("SELECT * FROM Cookie WHERE domain=:domain ORDER BY lastUsedAt DESC")
    suspend fun getAll(domain: String = DawnApp.currentDomain): List<Cookie>

    @Query("SELECT * FROM Cookie WHERE cookieHash=:cookieHash AND domain=:domain ")
    suspend fun getCookieByHash(cookieHash: String, domain: String = DawnApp.currentDomain): Cookie

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(cookie: Cookie)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(cookieList: List<Cookie>)

    @Transaction
    suspend fun resetCookies(cookieList: List<Cookie>) {
        nukeTable()
        insertAll(cookieList)
    }

    suspend fun setLastUsedCookie(cookie: Cookie){
        cookie.lastUsedAt = LocalDateTime.now()
        updateCookie(cookie)
    }
    @Update
    suspend fun updateCookie(cookie: Cookie)

    @Delete
    suspend fun delete(cookie: Cookie)

    @Query("DELETE FROM Cookie")
    fun nukeTable()
}
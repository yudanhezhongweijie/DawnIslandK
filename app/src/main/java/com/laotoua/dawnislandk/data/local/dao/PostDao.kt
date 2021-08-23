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
import androidx.lifecycle.distinctUntilChanged
import androidx.room.*
import com.laotoua.dawnislandk.DawnApp
import com.laotoua.dawnislandk.data.local.entity.Post
import java.time.LocalDateTime

@Dao
interface PostDao {
    @Query("SELECT * FROM Post WHERE domain=:domain")
    suspend fun getAll(domain: String = DawnApp.currentDomain): List<Post>

    @Query("SELECT * FROM Post WHERE id=:id AND domain=:domain LIMIT 1")
    fun findPostById(id: String, domain: String = DawnApp.currentDomain): LiveData<Post>

    @Query("SELECT * FROM Post WHERE id=:id AND domain=:domain LIMIT 1")
    suspend fun findPostByIdSync(id: String, domain: String = DawnApp.currentDomain): Post?

    fun findDistinctPostById(id: String, domain: String = DawnApp.currentDomain): LiveData<Post> = findPostById(id, domain).distinctUntilChanged()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(post: Post)

    suspend fun insertWithTimeStamp(post: Post) {
        post.setUpdatedTimestamp()
        insert(post)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(postList: List<Post>)

    suspend fun insertAllWithTimeStamp(postList: List<Post>) {
        val timestamp = LocalDateTime.now()
        val listWithTimeStamps = postList.apply { map { it.setUpdatedTimestamp(timestamp) } }
        insertAll(listWithTimeStamps)
    }

    @Delete
    suspend fun delete(post: Post)

    @Query("DELETE FROM Post")
    suspend fun nukeTable()
}
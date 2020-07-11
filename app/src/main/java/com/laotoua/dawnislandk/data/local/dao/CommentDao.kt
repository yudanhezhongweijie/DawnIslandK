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
import com.laotoua.dawnislandk.data.local.entity.Comment
import java.util.*

@Dao
interface CommentDao {

    @Query("SELECT * FROM Comment WHERE parentId=:parentId")
    suspend fun findAllByParentId(parentId: String): List<Comment>

    @Query("SELECT * FROM Comment WHERE parentId=:parentId AND page<=:page ORDER BY id ASC")
    suspend fun findByParentIdUntilPage(parentId: String, page: Int): List<Comment>

    @Query("SELECT * FROM Comment WHERE parentId=:parentId AND page=:page ORDER BY id ASC")
    fun findPageByParentId(parentId: String, page: Int): LiveData<List<Comment>>

    fun findDistinctPageByParentId(parentId: String, page: Int):
            LiveData<List<Comment>> = findPageByParentId(parentId, page).distinctUntilChanged()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(comment: Comment)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(commentList: List<Comment>)

    suspend fun insertAllWithTimeStamp(commentList: List<Comment>) {
        val timestamp = Date().time
        val listWithTimeStamps = commentList.apply { map { it.setUpdatedTimestamp(timestamp) } }
        insertAll(listWithTimeStamps)
    }

    suspend fun insertWithTimeStamp(comment: Comment) {
        comment.setUpdatedTimestamp(Date().time)
        insert(comment)
    }

    @Query("SELECT * FROM Comment WHERE id=:id LIMIT 1")
    suspend fun findCommentByIdSync(id: String): Comment?

    @Delete
    suspend fun delete(comment: Comment)

    @Query("DELETE FROM Comment")
    suspend fun nukeTable()
}
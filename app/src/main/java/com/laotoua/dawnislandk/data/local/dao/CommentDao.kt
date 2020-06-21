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

    @Query("SELECT * FROM Comment WHERE parentId=:parentId AND page<=:page")
    suspend fun findByParentIdUntilPage(parentId: String, page: Int): List<Comment>

    @Query("SELECT * FROM Comment WHERE parentId=:parentId AND page=:page")
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
    fun findCommentById(id: String): LiveData<Comment>

    fun findDistinctCommentById(id: String): LiveData<Comment> =
        findCommentById(id).distinctUntilChanged()

    @Delete
    suspend fun delete(comment: Comment)

    @Query("DELETE FROM Comment")
    suspend fun nukeTable()
}
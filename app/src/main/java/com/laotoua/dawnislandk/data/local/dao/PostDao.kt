package com.laotoua.dawnislandk.data.local.dao

import androidx.lifecycle.LiveData
import androidx.lifecycle.distinctUntilChanged
import androidx.room.*
import com.laotoua.dawnislandk.data.local.entity.Post
import java.util.*

@Dao
interface PostDao {
    @Query("SELECT * FROM Post")
    suspend fun getAll(): List<Post>

    @Query("SELECT * FROM Post WHERE id=:id")
    fun findPostById(id: String): LiveData<Post>

    @Query("SELECT * FROM Post WHERE id=:id")
    suspend fun findPostByIdSync(id: String): Post?

    fun findDistinctPostById(id: String): LiveData<Post> =
        findPostById(id).distinctUntilChanged()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(post: Post)

    suspend fun insertWithTimeStamp(post: Post) {
        post.setUpdatedTimestamp()
        insert(post)
    }

    @Update
    suspend fun updatePosts(vararg posts: Post)

    @Update
    suspend fun updatePostsWithTimeStamp(vararg posts: Post) {
        val timestamp = Date().time
        posts.map { it.setUpdatedTimestamp(timestamp) }
        updatePosts(*posts)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(postList: List<Post>)

    suspend fun insertAllWithTimeStamp(postList: List<Post>) {
        val timestamp = Date().time
        val listWithTimeStamps = postList.apply { map { it.setUpdatedTimestamp(timestamp) } }
        insertAll(listWithTimeStamps)
    }

    @Delete
    suspend fun delete(post: Post)

    @Query("DELETE FROM Post")
    fun nukeTable()
}
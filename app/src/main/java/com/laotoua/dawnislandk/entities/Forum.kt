package com.laotoua.dawnislandk.entities

import androidx.room.*
import com.google.gson.annotations.SerializedName


@Entity(primaryKeys = ["id"], tableName = "forum")
data class Forum(
    @ColumnInfo(name = "id")
    @SerializedName("id")
    val id: String,

    @ColumnInfo(name = "fgroup", defaultValue = "")
    @SerializedName("fgroup")
    val fgroup: String? = "",

    @ColumnInfo(name = "sort", defaultValue = "")
    @SerializedName("sort")
    val sort: String? = "",

    @ColumnInfo(name = "name", defaultValue = "")
    @SerializedName("name")
    val name: String = "",

    @ColumnInfo(name = "showName", defaultValue = "")
    @SerializedName("showName")
    val showName: String? = "",

    @ColumnInfo(name = "msg", defaultValue = "")
    @SerializedName("msg")
    val msg: String? = "",

    @ColumnInfo(name = "interval", defaultValue = "")
    @SerializedName("interval")
    val interval: String? = "",

    @ColumnInfo(name = "createdAt", defaultValue = "")
    @SerializedName("createdAt")
    val createdAt: String? = "",

    @ColumnInfo(name = "updateAt", defaultValue = "")
    @SerializedName("updateAt")
    val updateAt: String? = "",

    @ColumnInfo(name = "status", defaultValue = "")
    @SerializedName("status")
    val status: String? = ""
) {

    fun getDisplayName(): String {
        return if (showName != null && showName != "") showName else name
    }
}


@Dao
interface ForumDao {
    @Query("SELECT * FROM forum")
    suspend fun getAll(): List<Forum>

    @Query("SELECT * FROM forum WHERE id==:fid")
    suspend fun getForumById(fid: String): Forum

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(forumList: List<Forum>)

    @Delete
    suspend fun delete(forum: Forum)

    @Query("DELETE FROM forum")
    fun nukeTable()
}
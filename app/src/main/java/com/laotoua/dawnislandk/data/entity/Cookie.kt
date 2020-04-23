package com.laotoua.dawnislandk.data.entity

import androidx.room.*

@Entity(tableName = "cookie")
data class Cookie(

    @PrimaryKey
    val cookieHash: String,

    val cookieName: String
)


@Dao
interface CookieDao {
    @Query("SELECT * FROM cookie")
    suspend fun getAll(): List<Cookie>

    @Query("SELECT * FROM cookie WHERE cookieHash==:cookieHash")
    suspend fun getCookieByUserHash(cookieHash: String): Cookie

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(cookie: Cookie)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(cookieList: List<Cookie>)

    @Transaction
    suspend fun resetCookies(cookieList: List<Cookie>) {
        nukeTable()
        insertAll(cookieList)
    }

    @Update
    suspend fun updateCookie(cookie: Cookie)

    @Delete
    suspend fun delete(cookie: Cookie)

    @Query("DELETE FROM cookie")
    fun nukeTable()
}
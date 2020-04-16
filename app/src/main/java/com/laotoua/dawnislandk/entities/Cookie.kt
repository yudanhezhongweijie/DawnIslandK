package com.laotoua.dawnislandk.entities

import androidx.room.*

@Entity(tableName = "cookie")
data class Cookie(

    @PrimaryKey
    val userHash: String,

    val cookieName: String
)


@Dao
interface CookieDao {
    @Query("SELECT * FROM cookie")
    suspend fun getAll(): List<Cookie>

    @Query("SELECT * FROM cookie WHERE userhash==:userhash")
    suspend fun getCookieByUserHash(userhash: String): Cookie

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
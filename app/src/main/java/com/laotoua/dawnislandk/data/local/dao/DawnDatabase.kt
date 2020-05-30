package com.laotoua.dawnislandk.data.local.dao

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.laotoua.dawnislandk.data.local.Community
import com.laotoua.dawnislandk.data.local.Cookie
import com.laotoua.dawnislandk.data.local.Reply
import com.laotoua.dawnislandk.data.local.Thread

// TODO: export Schema
@Database(
    entities = [Community::class,
        Cookie::class,
        Reply::class,
        Thread::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converter::class)
abstract class DawnDatabase : RoomDatabase() {
    abstract fun cookieDao(): CookieDao
    abstract fun communityDao(): CommunityDao
    abstract fun replyDao(): ReplyDao
    abstract fun threadDao(): ThreadDao
}


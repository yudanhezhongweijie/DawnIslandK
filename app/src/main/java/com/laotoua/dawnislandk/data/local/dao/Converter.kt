package com.laotoua.dawnislandk.data.local.dao

import androidx.room.TypeConverter
import com.laotoua.dawnislandk.data.local.Forum
import com.laotoua.dawnislandk.data.local.Trend
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import java.util.*

class Converter {
    private val moshi = Moshi.Builder().build()

    @TypeConverter
    fun jsonToForumList(value: String): List<Forum> {
        return moshi.adapter<List<Forum>>(
            Types.newParameterizedType(
                List::class.java,
                Forum::class.java
            )
        ).fromJson(value)!!
    }

    @TypeConverter
    fun forumListToJson(list: List<Forum>): String {
        return moshi.adapter<List<Forum>>(
            Types.newParameterizedType(
                List::class.java,
                Forum::class.java
            )
        ).toJson(list)
    }

    @TypeConverter
    fun stringToTrendList(value: String): List<Trend> {
        return moshi.adapter<List<Trend>>(
            Types.newParameterizedType(
                List::class.java,
                Trend::class.java
            )
        ).fromJson(value)!!
    }

    @TypeConverter
    fun trendListToJson(list: List<Trend>): String {
        return moshi.adapter<List<Trend>>(
            Types.newParameterizedType(
                List::class.java,
                Trend::class.java
            )
        ).toJson(list)
    }


    @TypeConverter
    fun longToDate(dateLong: Long?): Date? {
        return dateLong?.let { Date(it) }
    }

    @TypeConverter
    fun dateTolOng(date: Date?): Long? {
        return date?.let { date.time }
    }

}
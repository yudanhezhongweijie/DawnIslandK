package com.laotoua.dawnislandk.data.local.dao

import androidx.room.TypeConverter
import com.laotoua.dawnislandk.data.local.entity.Forum
import com.laotoua.dawnislandk.data.local.entity.LuweiNotice
import com.laotoua.dawnislandk.data.local.entity.NoticeForum
import com.laotoua.dawnislandk.data.local.entity.Trend
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
    fun jsonToWhiteList(value: String): LuweiNotice.WhiteList {
        return moshi.adapter(LuweiNotice.WhiteList::class.java).fromJson(value)!!
    }

    @TypeConverter
    fun whiteListToJson(whiteList: LuweiNotice.WhiteList): String {
        return moshi.adapter(LuweiNotice.WhiteList::class.java).toJson(whiteList)
    }


    @TypeConverter
    fun jsonToTrendList(value: String): List<Trend> {
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
    fun jsonToNoticeForumList(value: String): List<NoticeForum> {
        return moshi.adapter<List<NoticeForum>>(
            Types.newParameterizedType(
                List::class.java,
                NoticeForum::class.java
            )
        ).fromJson(value)!!
    }

    @TypeConverter
    fun noticeForumListToJson(list: List<NoticeForum>): String {
        return moshi.adapter<List<NoticeForum>>(
            Types.newParameterizedType(
                List::class.java,
                NoticeForum::class.java
            )
        ).toJson(list)
    }

    @TypeConverter
    fun jsonToStringBooleanMap(value: String): Map<String, Boolean> {
        return moshi.adapter<Map<String, Boolean>>(
            Types.newParameterizedType(
                Map::class.java,
                String::class.java,
                Boolean::class.javaObjectType
            )
        ).fromJson(value)!!
    }

    @TypeConverter
    fun stringBooleanMapToJson(map: Map<String, Boolean>): String {
        return moshi.adapter<Map<String, Boolean>>(
            Types.newParameterizedType(
                Map::class.java,
                String::class.java,
                Boolean::class.javaObjectType
            )
        ).toJson(map)
    }

    @TypeConverter
    fun jsonToStringList(value: String): List<String> {
        return moshi.adapter<List<String>>(
            Types.newParameterizedType(
                List::class.java,
                String::class.java
            )
        ).fromJson(value)!!
    }

    @TypeConverter
    fun stringListToJson(list: List<String>): String {
        return moshi.adapter<List<String>>(
            Types.newParameterizedType(
                List::class.java,
                String::class.java
            )
        ).toJson(list)
    }

    @TypeConverter
    fun jsonToClientsInfoMap(value: String): Map<String, LuweiNotice.ClientInfo> {
        return moshi.adapter<Map<String, LuweiNotice.ClientInfo>>(
            Types.newParameterizedType(
                Map::class.java,
                String::class.java,
                LuweiNotice.ClientInfo::class.java
            )
        ).fromJson(value)!!
    }

    @TypeConverter
    fun clientsInfoMapToJson(map: Map<String, LuweiNotice.ClientInfo>): String {
        return moshi.adapter<Map<String, LuweiNotice.ClientInfo>>(
            Types.newParameterizedType(
                Map::class.java,
                String::class.java,
                LuweiNotice.ClientInfo::class.java
            )
        ).toJson(map)
    }

    @TypeConverter
    fun integerSetToString(set: MutableSet<Int>): String {
        return set.toString().removeSurrounding("[","]")
    }

    @TypeConverter
    fun stringToIntegerSet(s: String): MutableSet<Int> {
        return s.trim().splitToSequence(",").map { it.toInt() }.toMutableSet()
    }

    @TypeConverter
    fun longToDate(dateLong: Long?): Date? {
        return dateLong?.let { Date(it) }
    }

    @TypeConverter
    fun dateToLong(date: Date?): Long? {
        return date?.let { date.time }
    }

}
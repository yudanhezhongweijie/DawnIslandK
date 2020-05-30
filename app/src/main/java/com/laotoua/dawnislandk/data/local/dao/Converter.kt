package com.laotoua.dawnislandk.data.local.dao

import androidx.room.TypeConverter
import com.laotoua.dawnislandk.data.local.Forum
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types

class Converter {
    private val moshi = Moshi.Builder().build()
    @TypeConverter
    fun jsonToList(value: String): List<Forum> {
        val adapter: JsonAdapter<List<Forum>> = moshi.adapter(
            Types.newParameterizedType(
                List::class.java,
                Forum::class.java
            )
        )
        return adapter.fromJson(value)!!
    }

    @TypeConverter
    fun listToJson(list: List<Forum>): String {
        val adapter: JsonAdapter<List<Forum>> = moshi.adapter(
            Types.newParameterizedType(
                List::class.java,
                Forum::class.java
            )
        )
        return adapter.toJson(list)
    }
}
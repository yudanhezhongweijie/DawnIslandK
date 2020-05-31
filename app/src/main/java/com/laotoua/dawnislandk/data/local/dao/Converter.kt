package com.laotoua.dawnislandk.data.local.dao

import androidx.room.TypeConverter
import com.laotoua.dawnislandk.data.local.Forum
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types

class Converter {
    private val moshi = Moshi.Builder().build()

    @TypeConverter
    fun jsonToList(value: String): List<Forum> {
        return moshi.adapter<List<Forum>>(
            Types.newParameterizedType(
                List::class.java,
                Forum::class.java
            )
        ).fromJson(value)!!
    }

    @TypeConverter
    fun listToJson(list: List<Forum>): String {
        return moshi.adapter<List<Forum>>(
            Types.newParameterizedType(
                List::class.java,
                Forum::class.java
            )
        ).toJson(list)
    }
}
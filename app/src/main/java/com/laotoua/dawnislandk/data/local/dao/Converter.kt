package com.laotoua.dawnislandk.data.local.dao

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.laotoua.dawnislandk.data.local.Forum

class Converter {
    @TypeConverter
    fun listToJson(value: String): List<Forum> {
        return Gson().fromJson(value, object : TypeToken<List<Forum>>() {}.type)
    }

    @TypeConverter
    fun jsonToList(list: List<Forum>): String {
        return Gson().toJson(list)
    }
}
package com.laotoua.dawnislandk.data.local

import androidx.annotation.Keep
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
@Keep
data class Forum(
    @PrimaryKey
    val id: String,
    val fgroup: String = "",
    val sort: String = "",
    val name: String,
    val showName: String = "",
    val msg: String,
    val interval: String = "",
    val createdAt: String = "",
    val updateAt: String = "",
    val status: String = ""
) {
    fun getDisplayName(): String {
        return if (showName != "") showName else name
    }
}
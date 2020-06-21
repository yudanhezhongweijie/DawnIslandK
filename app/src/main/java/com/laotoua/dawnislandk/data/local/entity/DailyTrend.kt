package com.laotoua.dawnislandk.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass
import java.util.*

@JsonClass(generateAdapter = true)
@Entity
data class DailyTrend(
    @PrimaryKey val id: String, // id of the specific reply that generates the content
    val po: String, // userid of the poster
    val date: Long, // date when the trends posted
    val trends: List<Trend>,
    val lastReplyCount: Int,
    var lastUpdatedAt: Long = 0
){
    fun setUpdatedTimestamp(time: Long? = null) {
        lastUpdatedAt = time ?: Date().time
    }
}
package com.laotoua.dawnislandk.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity
class ReadingPage(
    @PrimaryKey val id:String,
    var page:Int,
    var lastUpdatedAt: Long = 0
){
    fun setUpdatedTimestamp(time: Long? = null) {
        lastUpdatedAt = time ?: Date().time
    }
}
package com.laotoua.dawnislandk.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
@Entity
data class NMBNotice(
    @PrimaryKey(autoGenerate = true)
    val id:Int? = null, // only used to keep track of version of notice
    val content: String,
    val date: Long,
    val enable: Boolean,
    var read: Boolean = false
)
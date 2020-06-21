package com.laotoua.dawnislandk.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
@Entity
data class Community(
    @PrimaryKey
    val id: String,
    val sort: String,
    val name: String,
    val status: String,
    val forums: List<Forum>
)
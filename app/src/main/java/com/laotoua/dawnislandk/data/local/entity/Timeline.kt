package com.laotoua.dawnislandk.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
@Entity
data class Timeline(
    @PrimaryKey
    val id: String,
    val name: String,
    val display_name: String,
    val notice: String,
    val max_page: Int,
)
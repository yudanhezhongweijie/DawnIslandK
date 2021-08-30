package com.laotoua.dawnislandk.data.local.entity

import androidx.room.Entity
import com.laotoua.dawnislandk.DawnApp
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
@Entity(primaryKeys=["id","domain"])
data class Timeline(
    val id: String,
    val name: String,
    val display_name: String,
    val notice: String,
    val max_page: Int,
    val domain:String = DawnApp.currentDomain
)
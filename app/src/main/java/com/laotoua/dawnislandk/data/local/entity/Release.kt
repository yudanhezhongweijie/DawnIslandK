package com.laotoua.dawnislandk.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Release(
    @PrimaryKey val id: Int = 1,
    val version: String,
    val downloadUrl: String,
    val message: String,
    val lastUpdatedAt:Long
){
    val versionCode get() = version.filter { it.isDigit() }.toInt()
}
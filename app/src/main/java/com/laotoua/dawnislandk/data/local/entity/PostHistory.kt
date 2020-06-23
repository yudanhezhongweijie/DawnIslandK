package com.laotoua.dawnislandk.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
class PostHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Int? = null,
    val postCookieName: String,
    val postTargetId: String, // do not have this when sending a newPost
    val postTargetPage: Int,
    val postTargetFid: String,
    val newPost: Boolean,// false if replying
    val imgPath: String,
    val content: String, //content
    val postDate: Long
)
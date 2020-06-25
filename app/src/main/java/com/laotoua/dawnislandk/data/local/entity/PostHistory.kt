package com.laotoua.dawnislandk.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
class PostHistory(
    @PrimaryKey
    val id: String,
    val cookieName: String,
    val postTargetId: String, // do not have this when sending a newPost
    val postTargetPage: Int,
    val postTargetFid: String,
    val newPost: Boolean,// false if replying
    val content: String, //content
    val img: String,
    val ext: String,
    val postDate: Long
) {
    constructor(id: String, targetPage: Int, img: String, ext: String, draft: Draft) : this(
        id,
        draft.cookieName,
        draft.postTargetId,
        targetPage,
        draft.postTargetFid,
        draft.newPost,
        draft.content,
        img,
        ext,
        draft.postDate
    )

    class Draft(
        val cookieName: String,
        val postTargetId: String, // do not have this when sending a newPost
        val postTargetFid: String,
        val newPost: Boolean,// false if replying
        var content: String, //content
        val postDate: Long
    )

    fun getImgUrl() = (img + ext)
}
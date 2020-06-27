package com.laotoua.dawnislandk.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
class PostHistory(
    @PrimaryKey
    val id: String, // actual id of post
    val newPost: Boolean,// false if replying
    val postTargetId: String, // equals postTargetFid when sending a new Post
    val postTargetFid: String,
    val postTargetPage: Int, // if replying, indicates posted page for jumps otherwise page 1
    val cookieName: String,
    val content: String, //content
    val img: String,
    val ext: String,
    val postDate: Long
) {
    constructor(id: String, targetPage: Int, img: String, ext: String, draft: Draft) : this(
        id,
        draft.newPost,
        draft.postTargetId,
        draft.postTargetFid,
        targetPage,
        draft.cookieName,
        draft.content,
        img,
        ext,
        draft.postDate
    )

    class Draft(
        val newPost: Boolean,// false if replying
        val postTargetId: String, // equals postTargetFid when sending a new Post
        val postTargetFid: String,
        val cookieName: String,
        var content: String, //content
        val postDate: Long
    )

    fun getImgUrl() = (img + ext)
}
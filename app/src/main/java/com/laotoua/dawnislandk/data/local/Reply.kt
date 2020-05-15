package com.laotoua.dawnislandk.data.local

import com.google.gson.annotations.SerializedName

data class Reply(
    @SerializedName("id")
    val id: String,
    @SerializedName("userid")
    val userid: String,
    @SerializedName("name")
    val name: String? = "",
    @SerializedName("sage")
    val sage: String? = "",
    @SerializedName("admin")
    val admin: String? = "1",
    @SerializedName("status")
    val status: String? = "n", //?
    @SerializedName("title")
    val title: String? = "",
    @SerializedName("email")
    val email: String,
    @SerializedName("now")
    val now: String,
    @SerializedName("content")
    val content: String,
    @SerializedName("img")
    val img: String,
    @SerializedName("ext")
    val ext: String,
    @Transient
    var page: Int? = 1
) {
    fun getImgUrl(): String {
        return img + ext
    }
}
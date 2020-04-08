package com.laotoua.dawnislandk.entities

import com.google.gson.annotations.SerializedName

class Community(
    @SerializedName("id")
    val id: String,
    @SerializedName("sort")
    val sort: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("status")
    val status: String,
    @SerializedName("forums")
    val forums: List<Forum>
)
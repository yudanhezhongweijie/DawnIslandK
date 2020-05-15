package com.laotoua.dawnislandk.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(tableName = "community")
data class Community(

    @PrimaryKey
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
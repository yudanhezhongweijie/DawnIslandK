package com.laotoua.dawnislandk.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cookie")
data class Cookie(

    @PrimaryKey
    val cookieHash: String,

    var cookieName: String
)
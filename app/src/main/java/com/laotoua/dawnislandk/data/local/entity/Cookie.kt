package com.laotoua.dawnislandk.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Cookie(
    @PrimaryKey val cookieHash: String,
    var cookieName: String
)
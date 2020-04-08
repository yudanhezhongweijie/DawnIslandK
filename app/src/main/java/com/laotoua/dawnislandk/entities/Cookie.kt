package com.laotoua.dawnislandk.entities

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(primaryKeys = ["userhash"], tableName = "cookie")
class Cookie(
    @ColumnInfo(name = "userhash")
    val userHash: String,

    @ColumnInfo(name = "cookiename")
    val cookieName: String
)
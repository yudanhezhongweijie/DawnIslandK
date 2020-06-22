package com.laotoua.dawnislandk.data.local.entity

import androidx.room.Entity

@Entity(primaryKeys = ["date", "postId"])
data class BrowsingHistory(
    val date: Long, // only records the date with month and year
    val postId: String,
    var postFid: String,
    var pages: MutableSet<Int> // number of pages read
)
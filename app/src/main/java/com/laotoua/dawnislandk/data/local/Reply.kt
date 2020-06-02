package com.laotoua.dawnislandk.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass
import java.util.*

@JsonClass(generateAdapter = true)
@Entity
data class Reply(
    @PrimaryKey val id: String,
    val userid: String,
    val name: String = "",
    val sage: String = "0",
    val admin: String = "0",
    val status: String = "n",
    val title: String,
    val email: String,
    val now: String,
    val content: String,
    val img: String,
    val ext: String,
    var page: Int = 1,
    var parentId: String = "",
    var lastUpdatedAt: Long = 0
) {
    fun getImgUrl(): String = (img + ext)

    fun isNotAd(): Boolean = (id != "9999999")

    fun equalsExceptTimestamp(target: Reply?): Boolean =
        if (target == null) false
        else id == target.id && userid == target.userid
                && name == target.name && sage == target.sage
                && admin == target.admin && status == target.status
                && title == target.title && email == target.email
                && now == target.now && content == target.content
                && img == target.img && ext == target.ext
                && page == target.page && parentId == target.parentId

    fun setUpdatedTimestamp(time: Long? = null) {
        lastUpdatedAt = time ?: Date().time
    }
}

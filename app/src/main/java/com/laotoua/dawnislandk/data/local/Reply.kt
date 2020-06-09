package com.laotoua.dawnislandk.data.local

import androidx.room.Entity
import androidx.room.Ignore
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
    // used for reply filtering
    @Ignore
    var visible: Boolean = true

    fun getSimplifiedTitle(): String = if (title.isNotBlank() && title != "无标题") "标题：$title" else ""
    fun getSimplifiedName(): String = if (name.isNotBlank() && name != "无名氏") "作者：$name" else ""

    fun getImgUrl(): String = (img + ext)
    fun isNotAd(): Boolean = (id != "9999999")
    fun isAd(): Boolean = !isNotAd()

    fun equalsWithServerData(target: Reply?): Boolean =
        if (target == null) false
        else id == target.id && userid == target.userid
                && name == target.name && sage == target.sage
                && admin == target.admin && status == target.status
                && title == target.title && email == target.email
                && now == target.now && content == target.content
                && img == target.img && ext == target.ext

    fun setUpdatedTimestamp(time: Long? = null) {
        lastUpdatedAt = time ?: Date().time
    }
}

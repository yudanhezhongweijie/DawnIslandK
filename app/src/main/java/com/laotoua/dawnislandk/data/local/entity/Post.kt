/*
 *  Copyright 2020 Fishballzzz
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package com.laotoua.dawnislandk.data.local.entity

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.util.*
import kotlin.math.ceil

@JsonClass(generateAdapter = true)
@Entity
data class Post(
    @PrimaryKey val id: String, //	该串的id
    var fid: String = "", //	该串的fid, 非时间线的串会被设置
    val img: String,//	该串的图片相对地址
    val ext: String, // 	该串图片的后缀
    val now: String,// 	该串的可视化发言时间
    val userid: String, // 	该串的饼干
    val name: String,
    val email: String,
    val title: String,
    val content: String,
    val sage: String = "",
    val admin: String = "0",//admin 	是否是酷炫红名，如果是酷炫红名则userid为红名id
    val status: String = "",
    @Json(name = "replys") @Ignore var comments: List<Comment> = emptyList(), //replys 	主页展示回复的帖子(5个）
    val replyCount: String = "",
    var lastUpdatedAt: Long = 0
) {
    // Room uses this
    constructor(
        id: String,
        fid: String,
        img: String,
        ext: String,
        now: String,
        userid: String,
        name: String,
        email: String,
        title: String,
        content: String,
        sage: String,
        admin: String,
        status: String,
        replyCount: String,
        lastUpdatedAt: Long
    ) : this(
        id,
        fid,
        img,
        ext,
        now,
        userid,
        name,
        email,
        title,
        content,
        sage,
        admin,
        status,
        emptyList(),
        replyCount,
        lastUpdatedAt
    )

    // convert threadList to Reply
    fun toComment() = Comment(
        id,
        userid,
        name,
        sage,
        admin,
        status,
        title,
        email,
        now,
        content,
        img,
        ext,
        1,
        id
    )

    // special handler for sticky top banner
    fun isStickyTopBanner(): Boolean = id == "14500641"

    fun getImgUrl() = (img + ext)
    fun getSimplifiedTitle(): String = if (title.isNotBlank() && title != "无标题") "标题：$title" else ""
    fun getSimplifiedName(): String = if (name.isNotBlank() && name != "无名氏") "名称：$name" else ""
    fun getMaxPage() =
        if (replyCount.isBlank()) 1 else 1.coerceAtLeast(ceil(replyCount.toDouble() / 19).toInt())

    // only compares by server fields
    override fun equals(other: Any?) =
        if (other is Post) {
            id == other.id && fid == other.fid && img == other.img
                    && ext == other.ext && now == other.now
                    && userid == other.userid && name == other.name
                    && email == other.email && title == other.title
                    && content == other.content && sage == other.sage
                    && admin == other.admin && status == other.status
                    && replyCount == other.replyCount
        } else false

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + fid.hashCode()
        result = 31 * result + img.hashCode()
        result = 31 * result + ext.hashCode()
        result = 31 * result + now.hashCode()
        result = 31 * result + userid.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + email.hashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + content.hashCode()
        result = 31 * result + sage.hashCode()
        result = 31 * result + admin.hashCode()
        result = 31 * result + status.hashCode()
        result = 31 * result + replyCount.hashCode()
        return result
    }

    fun setUpdatedTimestamp(time: Long? = null) {
        lastUpdatedAt = time ?: Date().time
    }

    fun stripCopy(): Post =
        copy(
            id = id,
            fid = fid,
            img = img,
            ext = ext,
            now = now,
            userid = userid,
            name = name,
            email = email,
            title = title,
            content = content,
            sage = sage,
            admin = admin,
            status = status,
            comments = emptyList(),
            replyCount = replyCount,
            lastUpdatedAt = lastUpdatedAt
        )

}
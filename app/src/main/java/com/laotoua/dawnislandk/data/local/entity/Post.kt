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
    var category: String = "",
    val img: String, //	该串的图片相对地址
    val ext: String, // 	该串图片的后缀
    val now: String, // 	该串的可视化发言时间
    val userid: String, //userid 	该串的饼干
    val name: String, //name 	你懂得
    val email: String, //email 	你懂得
    val title: String, //title 	你还是懂的(:з」∠)
    val content: String, //content 	....这个你也懂
    val sage: String = "", // sage
    val admin: String = "0", //admin 	是否是酷炫红名，如果是酷炫红名则userid为红名id
    val status: String = "", //
    @Json(name = "replys") @Ignore var comments: List<Comment> = emptyList(), //replys 	主页展示回复的帖子
    val replyCount: String = "", //replyCount 	总共有多少个回复
    var lastUpdatedAt: Long = 0
) {
    // Room uses this
    constructor(
        id: String,
        fid: String,
        category: String,
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
        category,
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
        id = id,
        userid = userid,
        name = name,
        sage = sage,
        admin = admin,
        status = status,
        title = title,
        email = email,
        now = now,
        content = content,
        img = img,
        ext = ext,
        page = 1
    )

    fun getImgUrl() = (img + ext)
    fun getSimplifiedTitle(): String = if (title.isNotBlank() && title != "无标题") "标题：$title" else ""
    fun getSimplifiedName(): String = if (name.isNotBlank() && name != "无名氏") "名称：$name" else ""
    fun getMaxPage() = if (replyCount.isBlank()) 1 else ceil(replyCount.toDouble() / 19).toInt()

    override fun equals(other: Any?) =
        if (other is Post) {
            id == other.id && fid == other.fid
                    && category == other.category && img == other.img
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
        result = 31 * result + category.hashCode()
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
            category = category,
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
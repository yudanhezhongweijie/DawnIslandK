package com.laotoua.dawnislandk.data.local

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
@Entity
data class Thread(
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
    val sage: String = "0", // sage
    val admin: String = "0", //admin 	是否是酷炫红名，如果是酷炫红名则userid为红名id
    val status: String = "n", //
    @Ignore var replys: List<Reply> = emptyList(), //replys 	主页展示回复的帖子
    val replyCount: String = "0", //replyCount 	总共有多少个回复
    var readingProgress: Int = 0, // 记录上次看到的进度
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
        readingProgress: Int,
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
        readingProgress,
        lastUpdatedAt
    )

    // convert threadList to Reply
    fun toReply() = Reply(
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

    fun isDataComplete(): Boolean = (userid.isNotBlank() && now.isNotBlank())

    fun getImgUrl() = (img + ext)
}
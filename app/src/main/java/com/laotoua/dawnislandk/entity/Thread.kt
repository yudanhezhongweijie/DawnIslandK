package com.laotoua.dawnislandk.entity

import androidx.room.Ignore
import com.google.gson.annotations.SerializedName

data class Thread(
    @SerializedName("id")
    val id: String, //	该串的id
    @SerializedName("fid")
    var fid: String? = "", //	该串的fid, 非时间线的串会被设置

    @Ignore
    var forumName: String? = "",// only used for displaying name

    @SerializedName("category")
    var category: String? = "",

    @SerializedName("img")
    val img: String, //	该串的图片相对地址
    @SerializedName("ext")
    val ext: String, // 	该串图片的后缀
    @SerializedName("now")
    val now: String, // 	该串的可视化发言时间
    @SerializedName("userid")
    val userid: String, //userid 	该串的饼干
    @SerializedName("name")
    val name: String, //name 	你懂得
    @SerializedName("email")
    val email: String, //email 	你懂得
    @SerializedName("title")
    val title: String, //title 	你还是懂的(:з」∠)
    @SerializedName("content")
    val content: String, //content 	....这个你也懂
    @SerializedName("sage")
    val sage: String? = null, // sage
    @SerializedName("admin")
    val admin: String, //admin 	是否是酷炫红名，如果是酷炫红名则userid为红名id
    @SerializedName("status")
    val status: String? = "n", //?
    @SerializedName("replys")
    val replys: List<Reply>? = null, //replys 	主页展示回复的帖子
    @SerializedName("replyCount")
    val replyCount: String? = null //replyCount 	总共有多少个回复
) {
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
        ext = ext
    )

    fun getImgUrl(): String {
        return img + ext
    }
}
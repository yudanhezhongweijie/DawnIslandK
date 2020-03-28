package com.laotoua.dawnislandk.util

import android.content.Context
import androidx.room.*
import com.google.gson.annotations.SerializedName


@Database(entities = arrayOf(Forum::class), version = 1)
abstract class DawnDatabase : RoomDatabase() {
    abstract fun forumDao(): ForumDao

    companion object {
        // Singleton prevents multiple instances of database opening at the
        // same time.
        @Volatile
        private var INSTANCE: DawnDatabase? = null

        fun getDatabase(context: Context): DawnDatabase {
            val tempInstance = INSTANCE
            if (tempInstance != null) {
                return tempInstance
            }
            synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DawnDatabase::class.java,
                    "dawn_database"
                ).build()
                INSTANCE = instance
                return instance
            }
        }
    }
}

@Dao
interface ForumDao {
    @Query("SELECT * FROM forum")
    suspend fun getAll(): List<Forum>

    @Query("SELECT * FROM forum WHERE id==:fid")
    suspend fun getForumById(fid: String): Forum

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(forumList: List<Forum>)

    @Delete
    suspend fun delete(forum: Forum)

    @Query("DELETE FROM forum")
    fun nukeTable()
}

@Entity(primaryKeys = arrayOf("id"), tableName = "forum")
data class Forum(
    @ColumnInfo(name = "id")
    @SerializedName("id")
    val id: String,

    @ColumnInfo(name = "fgroup", defaultValue = "")
    @SerializedName("fgroup")
    val fgroup: String? = "",

    @ColumnInfo(name = "sort", defaultValue = "")
    @SerializedName("sort")
    val sort: String? = "",

    @ColumnInfo(name = "name", defaultValue = "")
    @SerializedName("name")
    val name: String = "",

    @ColumnInfo(name = "showName", defaultValue = "")
    @SerializedName("showName")
    val showName: String? = "",

    @ColumnInfo(name = "msg", defaultValue = "")
    @SerializedName("msg")
    val msg: String? = "",

    @ColumnInfo(name = "interval", defaultValue = "")
    @SerializedName("interval")
    val interval: String? = "",

    @ColumnInfo(name = "createdAt", defaultValue = "")
    @SerializedName("createdAt")
    val createdAt: String? = "",

    @ColumnInfo(name = "updateAt", defaultValue = "")
    @SerializedName("updateAt")
    val updateAt: String? = "",

    @ColumnInfo(name = "status", defaultValue = "")
    @SerializedName("status")
    val status: String? = ""
) {

    fun getDisplayName(): String {
        return if (showName != null && showName != "") showName else name
    }
}

class Community(
    @SerializedName("id")
    val id: String,
    @SerializedName("sort")
    val sort: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("status")
    val status: String,
    @SerializedName("forums")
    val forums: List<Forum>
)

class ThreadList(
    @SerializedName("id")
    val id: String, //	该串的id
    @SerializedName("fid")
    var fid: String? = "", //	该串的fid, 非时间线的串会被设置

    @Ignore
    var forumName: String? = "",// only used for displaying name

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
    val sage: String, // sage
    @SerializedName("admin")
    val admin: String, //admin 	是否是酷炫红名，如果是酷炫红名则userid为红名id
    @SerializedName("status")
    val status: String? = "n", //?
    @SerializedName("replys")
    val replys: List<Reply>?, //replys 	主页展示回复的帖子
    @SerializedName("replyCount")
    val replyCount: String //replyCount 	总共有多少个回复
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
}

class Reply(
    @SerializedName("id")
    val id: String,
    @SerializedName("userid")
    val userid: String,
    @SerializedName("name")
    val name: String? = "",
    @SerializedName("sage")
    val sage: String? = "",
    @SerializedName("admin")
    val admin: String? = "1",
    @SerializedName("status")
    val status: String? = "n", //?
    @SerializedName("title")
    val title: String,
    @SerializedName("email")
    val email: String,
    @SerializedName("now")
    val now: String,
    @SerializedName("content")
    val content: String,
    @SerializedName("img")
    val img: String,
    @SerializedName("ext")
    val ext: String
)
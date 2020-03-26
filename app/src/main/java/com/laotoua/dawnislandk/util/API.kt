package com.laotoua.dawnislandk.util

import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.lang.reflect.Type

class API {
    private val TAG: String = "API calls"
    private val baseApi = "https://adnmb2.com/api/"
    private val suffix_forumList = "getForumList"
    private val suffix_threadList = "showf?"
    private val suffix_replyList = "thread?"
    private val cdn = " https://nmbimg.fastmirror.org/"
    private val client = OkHttpClient()

    private fun getRawResponse(data: String, params: String = ""): Response {
        val url = when (data) {
            "forums" -> baseApi + suffix_forumList
            "threads" -> baseApi + suffix_threadList + params
            "replys" -> baseApi + suffix_replyList + params
            else -> throw IOException("Unhandled api call $data")
        }

        Log.i(TAG, "Sending HTTP request at $url")
        val request = Request.Builder()
            .url(url)
            .build()
        val res = client.newCall(request).execute()
        if (!res.isSuccessful) throw IOException("Unexpected code $res")
        return res
    }

    private fun parseForums(response: Response): List<Forum> {
        val newType: Type = object : TypeToken<List<Community>>() {}.type
        val l: List<Community> = Gson().fromJson(response.body!!.string(), newType)
        if (l.isEmpty()) {
            Log.e(TAG, "Didn't get forums from API")
        }
        return l.flatMap { c -> c.forums }
    }

    private fun parseThreads(response: Response): List<ThreadList> {
        val newType: Type = object : TypeToken<List<ThreadList>>() {}.type
        val l: List<ThreadList> = Gson().fromJson(response.body!!.string(), newType)
        if (l.isEmpty()) {
            Log.e(TAG, "Didn't get threads from API")
        }
        return l
    }

    private fun parseReplys(response: Response): List<Reply> {
        val newType: Type = object : TypeToken<ThreadList>() {}.type
        val replys: List<Reply> =
            (Gson().fromJson(response.body!!.string(), newType) as ThreadList).replys!!
        if (replys.isEmpty()) {
            Log.e(TAG, "Didn't get thread reply from API")
        }
        return replys
    }

    suspend fun getForums(): List<Forum> {
        try {
            val rawResponse =
                withContext(Dispatchers.IO) {
                    Log.i(TAG, "downloading forums...")
                    getRawResponse("forums")
                }
            val forumsList =
                withContext(Dispatchers.Default) {
                    Log.i(TAG, "parsing forums...")
                    parseForums(rawResponse)
                }
            return forumsList
        } catch (e: Exception) {
            Log.e(TAG, e.stackTrace.toString())
            return emptyList()
        }
    }

    suspend fun getThreads(params: String): List<ThreadList> {
        try {
            val rawResponse =
                withContext(Dispatchers.IO) {
                    Log.i(TAG, "downloading threads...")
                    getRawResponse("threads", params)
                }

            val threadsList =
                withContext(Dispatchers.Default) {
                    Log.i(TAG, "parsing threads...")
                    parseThreads(rawResponse)
                }

            return threadsList
        } catch (e: Exception) {
            Log.e(TAG, e.stackTrace.toString())
            return emptyList()
        }
    }

    suspend fun getReplys(params: String): List<Reply> {
        try {
            val rawResponse =
                withContext(Dispatchers.IO) {
                    Log.i(TAG, "downloading replys...")
                    getRawResponse("threads", params)
                }

            val replysList =
                withContext(Dispatchers.Default) {
                    Log.i(TAG, "parsing replys...")
                    parseReplys(rawResponse)
                }

            return replysList
        } catch (e: Exception) {
            Log.e(TAG, e.stackTrace.toString())
            return emptyList()
        }
    }
}

class Forum(
    val id: String,
    val fgroup: String,
    val sort: String,
    val name: String,
    val showName: String,
    val msg: String,
    val interval: String,
    val createdAt: String,
    val updateAt: String,
    val status: String
)

class Community(
    val id: String,
    val sort: String,
    val name: String,
    val status: String,
    val forums: List<Forum>
)

// TODO: timeline has extra fid field
class ThreadList(
    val id: String, //	该串的id
    val img: String, //	该串的图片相对地址
    val ext: String, // 	该串图片的后缀
    val now: String, // 	该串的可视化发言时间
    val userid: String, //userid 	该串的饼干
    val name: String, //name 	你懂得
    val email: String, //email 	你懂得
    val title: String, //title 	你还是懂的(:з」∠)
    val content: String, //content 	....这个你也懂
    val sage: String, // sage
    val admin: String, //admin 	是否是酷炫红名，如果是酷炫红名则userid为红名id
    val status: String, //?
    val replys: List<Reply>?, //replys 	主页展示回复的帖子
    val replyCount: String //replyCount 	总共有多少个回复
)

class Reply(
    val id: String,
    val userid: String,
    val admin: String,
    val title: String,
    val email: String,
    val now: String,
    val content: String,
    val img: String,
    val ext: String
)

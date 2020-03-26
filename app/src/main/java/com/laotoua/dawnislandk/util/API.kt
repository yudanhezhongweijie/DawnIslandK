package com.laotoua.dawnislandk.util

import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.lang.reflect.Type

class API() {
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

    suspend fun getForums(scope: CoroutineScope): List<Forum> {
        val rawResponse = scope.async {
            withContext(Dispatchers.IO + CoroutineName("forum_download")) {
                Log.d(TAG, "downloading forums... in Coroutine: $coroutineContext")
                getRawResponse("forums")
            }
        }
        val forumsList = scope.async {
            withContext(Dispatchers.Default + CoroutineName("forum_parsing")) {
                Log.d(TAG, "parsing forums... in Coroutine: $coroutineContext")
                parseForums(rawResponse.await())
            }
        }
        return forumsList.await()
    }

    suspend fun getThreads(scope: CoroutineScope, params: String): List<ThreadList> {
        val rawResponse = scope.async {
            withContext(Dispatchers.IO + CoroutineName("forum_download")) {
                Log.d(TAG, "downloading threads... in Coroutine: $coroutineContext")
                getRawResponse("threads", params)
            }
        }
        val threadsList = scope.async {
            withContext(Dispatchers.Default + CoroutineName("forum_parsing")) {
                Log.d(TAG, "parsing threads... in Coroutine: $coroutineContext")
                parseThreads(rawResponse.await())
            }
        }
        return threadsList.await()
    }

    suspend fun getReplys(scope: CoroutineScope, params: String): List<Reply> {
        val rawResponse = scope.async {
            withContext(Dispatchers.IO + CoroutineName("forum_download")) {
                Log.d(TAG, "downloading replys... in Coroutine: $coroutineContext")
                getRawResponse("threads", params)
            }
        }
        val replysList = scope.async {
            withContext(Dispatchers.Default + CoroutineName("forum_parsing")) {
                Log.d(TAG, "parsing replys... in Coroutine: $coroutineContext")
                parseReplys(rawResponse.await())
            }
        }
        return replysList.await()
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

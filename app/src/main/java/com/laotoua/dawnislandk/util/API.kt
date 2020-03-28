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
    private val suffix_timeLine = "timeline?"
    private val suffix_replyList = "thread?"
    private val cdn = " https://nmbimg.fastmirror.org/"
    private val client = OkHttpClient()


    private fun getRawResponse(data: String, params: String = ""): Response {
        val url = when (data) {
            "forums" -> baseApi + suffix_forumList
            "threads" -> baseApi + suffix_threadList + params
            "timeline" -> baseApi + suffix_timeLine + params
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
            Log.e(TAG, "Failed to get forums")
            println(e.stackTrace)
            return emptyList()
        }
    }

    suspend fun getThreads(
        params: String,
        timeline: Boolean = false,
        fid: String = ""
    ): List<ThreadList> {
        try {
            val rawResponse =
                withContext(Dispatchers.IO) {
                    Log.i(TAG, "downloading threads...")
                    if (!timeline) getRawResponse("threads", params) else getRawResponse(
                        "timeline",
                        params
                    )
                }

            val threadsList =
                withContext(Dispatchers.Default) {
                    Log.i(TAG, "parsing threads...")
                    parseThreads(rawResponse)
                }
            // assign fid if not timeline
            if (!timeline) threadsList.map { it.fid = fid }

            return threadsList
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get threads")
            println(e.stackTrace)
            return emptyList()
        }
    }

    suspend fun getReplys(params: String): List<Reply> {
        try {
            val rawResponse =
                withContext(Dispatchers.IO) {
                    Log.i(TAG, "downloading replys...")
                    getRawResponse("replys", params)
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

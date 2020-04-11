package com.laotoua.dawnislandk.network

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.laotoua.dawnislandk.entities.Community
import com.laotoua.dawnislandk.entities.Forum
import com.laotoua.dawnislandk.entities.Reply
import com.laotoua.dawnislandk.entities.ThreadList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import retrofit2.Retrofit
import timber.log.Timber
import java.lang.reflect.Type

object NMBServiceClient {
    private val service: NMBService = Retrofit.Builder()
        .baseUrl("https://nmb.fastmirror.org/")
        .build()
        .create(NMBService::class.java)

    suspend fun getForums(): List<Forum> {
        try {
            val rawResponse =
                withContext(Dispatchers.IO) {
                    Timber.i("downloading forums...")
                    service.getNMBForumList().execute().body()!!
                }
            return withContext(Dispatchers.Default) {
                Timber.i("parsing forums...")
                parseForums(rawResponse)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to get forums")
            throw e
        }
    }

    private fun parseForums(response: ResponseBody): List<Forum> {
        val newType: Type = object : TypeToken<List<Community>>() {}.type
        val l: List<Community> = Gson().fromJson(response.string(), newType)
        if (l.isEmpty()) {
            Timber.e("Didn't get forums from API")
        }

        return l.flatMap { c -> c.forums }
    }

    private fun parseThreads(response: ResponseBody): List<ThreadList> {
        val newType: Type = object : TypeToken<List<ThreadList>>() {}.type
        val l: List<ThreadList> = Gson().fromJson(response.string(), newType)
        if (l.isEmpty()) {
            Timber.e("Didn't get threads from API")
        }
        return l
    }

    private fun parseReplys(response: ResponseBody): List<Reply> {
        val newType: Type = object : TypeToken<ThreadList>() {}.type
        val replys: List<Reply> =
            (Gson().fromJson(response.string(), newType) as ThreadList).replys!!
        if (replys.isEmpty()) {
            Timber.e("Didn't get thread reply from API")
        }
        return replys
    }

    private fun parseQuote(response: ResponseBody): Reply {
        return Gson().fromJson(response.string(), Reply::class.java)
    }


    // TODO: handle case where thread is deleted
    suspend fun getThreads(fid: String, page: Int): List<ThreadList> {
        try {
            val rawResponse =
                withContext(Dispatchers.IO) {
                    Timber.i("Downloading threads...")
                    if (fid == "-1") service.getNMBTimeLine(page).execute().body()!!
                    else service.getNMBThreads(fid, page).execute().body()!!
                }

            val threadsList =
                withContext(Dispatchers.Default) {
                    Timber.i("Parsing threads...")
                    parseThreads(rawResponse)
                }
            // assign fid if not timeline
            if (fid != "-1") threadsList.map { it.fid = fid }
            return threadsList
        } catch (e: Exception) {
            Timber.e(e, "Failed to get threads")
            throw e
        }
    }

    // TODO: handle case where thread is deleted
    suspend fun getReplys(id: String, page: Int): List<Reply> {
        try {
            val rawResponse =
                withContext(Dispatchers.IO) {
                    Timber.i("Downloading replys...")
                    service.getNMBReplys(id, page).execute().body()!!
                }

            return withContext(Dispatchers.Default) {
                Timber.i("Parsing replys...")
                parseReplys(rawResponse)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to get replys")
            throw e
        }
    }

    suspend fun getQuote(id: String): Reply {
        try {
            val rawResponse = withContext(Dispatchers.IO) {
                Timber.i("Downloading quote...")
                service.getNMBQuote(id).execute().body()!!
            }
            return withContext(Dispatchers.Default) {
                Timber.i("Parsing quote")
                parseQuote(rawResponse)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to get quote")
            throw e
        }
    }
}
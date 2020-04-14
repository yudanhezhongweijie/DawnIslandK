package com.laotoua.dawnislandk.network

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.laotoua.dawnislandk.entities.Community
import com.laotoua.dawnislandk.entities.Reply
import com.laotoua.dawnislandk.entities.ThreadList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import retrofit2.Retrofit
import timber.log.Timber

object NMBServiceClient {
    private val service: NMBService = Retrofit.Builder()
        .baseUrl("https://nmb.fastmirror.org/")
        .build()
        .create(NMBService::class.java)

    private val parser = Gson()

    suspend fun getCommunities(): List<Community> {
        try {
            val rawResponse =
                withContext(Dispatchers.IO) {
                    Timber.i("downloading forums...")
                    service.getNMBForumList().execute().body()!!
                }
            return withContext(Dispatchers.Default) {
                Timber.i("parsing forums...")
                parseCommunities(rawResponse)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to get forums")
            throw e
        }
    }

    private fun parseCommunities(response: ResponseBody): List<Community> {
        return parser.fromJson(response.string(), object : TypeToken<List<Community>>() {}.type)
    }

    private fun parseThreads(response: ResponseBody): List<ThreadList> {
        return parser.fromJson(response.string(), object : TypeToken<List<ThreadList>>() {}.type)
    }

    private fun parseReplys(response: ResponseBody): List<Reply> {
        return (parser.fromJson(
            response.string(),
            object : TypeToken<ThreadList>() {}.type
        ) as ThreadList).replys!!
    }

    private fun parseQuote(response: ResponseBody): Reply {
        return parser.fromJson(response.string(), Reply::class.java)
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
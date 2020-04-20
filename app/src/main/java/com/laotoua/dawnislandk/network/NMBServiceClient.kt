package com.laotoua.dawnislandk.network

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.laotoua.dawnislandk.entity.Community
import com.laotoua.dawnislandk.entity.Reply
import com.laotoua.dawnislandk.entity.Thread
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import org.jsoup.Jsoup
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import timber.log.Timber
import java.io.File


object NMBServiceClient {
    private val service: NMBService = Retrofit.Builder()
        .baseUrl("https://nmb.fastmirror.org/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(NMBService::class.java)

    private val parser = Gson()

    suspend fun getCommunities(): List<Community> {
        try {
            val rawResponse =
                withContext(Dispatchers.IO) {
                    Timber.i("downloading communities...")
                    service.getNMBForumList().execute().body()!!
                }
            return withContext(Dispatchers.Default) {
                Timber.i("parsing communities...")
                parseCommunities(rawResponse)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to get communities")
            throw e
        }
    }

    private fun parseCommunities(response: ResponseBody): List<Community> {
        return parser.fromJson(response.string(), object : TypeToken<List<Community>>() {}.type)
    }


    // TODO: handle case where thread is deleted
    suspend fun getThreads(fid: String, page: Int): List<Thread> {
        try {
            val rawResponse =
                withContext(Dispatchers.IO) {
                    Timber.i("Downloading threads on Forum $fid...")
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

    private fun parseThreads(response: ResponseBody): List<Thread> {
        return parser.fromJson(response.string(), object : TypeToken<List<Thread>>() {}.type)
    }

    // TODO: handle case where thread is deleted
    suspend fun getReplys(id: String, page: Int): Thread {
        try {
            val rawResponse =
                withContext(Dispatchers.IO) {
                    Timber.i("Downloading replys on Thread $id...")
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

    private fun parseReplys(response: ResponseBody): Thread {
        return parser.fromJson(response.string(), Thread::class.java)
    }

    suspend fun getQuote(id: String): Reply {
        try {
            val rawResponse = withContext(Dispatchers.IO) {
                Timber.i("Downloading quote $id...")
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

    private fun parseQuote(response: ResponseBody): Reply {
        return parser.fromJson(response.string(), Reply::class.java)
    }


    suspend fun sendPost(
        newPost: Boolean,
        targetId: String, name: String?,
        email: String?, title: String?,
        content: String?, water: String?,
        image: File?, userhash: String
    ): String {
        try {
            return withContext(Dispatchers.IO) {
                if (newPost) {
                    Timber.i("Posting New Thread to $targetId...")
                } else {
                    Timber.i("Positing Reply to $targetId...")
                }
                var imagePart: MultipartBody.Part? = null
                image?.run {
                    asRequestBody(("image/${image.extension}").toMediaTypeOrNull()).run {
                        imagePart = MultipartBody.Part.createFormData("image", image.name, this)
                    }
                }
                if (newPost) {
                    service.postThread(
                        targetId.toRequestBody(), name?.toRequestBody(),
                        email?.toRequestBody(), title?.toRequestBody(),
                        content?.toRequestBody(), water?.toRequestBody(),
                        imagePart,
                        "userhash=$userhash"
                    )
                } else {
                    service.postReply(
                        targetId.toRequestBody(), name?.toRequestBody(),
                        email?.toRequestBody(), title?.toRequestBody(),
                        content?.toRequestBody(), water?.toRequestBody(),
                        imagePart,
                        "userhash=$userhash"
                    )
                }.execute().body()!!.string().run {
                    Jsoup.parse(this).getElementsByClass("system-message")
                        .first().children().not(".jump").text()
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Reply did not succeeded...")
            throw e
        }
    }
}
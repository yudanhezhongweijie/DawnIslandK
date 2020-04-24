package com.laotoua.dawnislandk.data.network

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.laotoua.dawnislandk.data.entity.Community
import com.laotoua.dawnislandk.data.entity.Reply
import com.laotoua.dawnislandk.data.entity.Thread
import com.laotoua.dawnislandk.util.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import org.jsoup.Jsoup
import retrofit2.Retrofit
import timber.log.Timber
import java.io.File


object NMBServiceClient {
    private val service: NMBService = Retrofit.Builder()
        .baseUrl(Constants.baseCDN)
//        .addConverterFactory(GsonConverterFactory.create())
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

    private val parseThreads: (String) -> List<Thread> =
        { parser.fromJson(it, object : TypeToken<List<Thread>>() {}.type) }

    suspend fun getThreads(fid: String, page: Int): APIResponse<List<Thread>> {
        Timber.i("Downloading threads on Forum $fid...")
        val call =
            if (fid == "-1") service.getNMBTimeLine(page)
            else service.getNMBThreads(fid, page)
        return APIResponse.create(call, parseThreads)
    }

    private fun parseCommunities(response: ResponseBody): List<Community> {
        return parser.fromJson(response.string(), object : TypeToken<List<Community>>() {}.type)
    }

    private fun parseThreadsDeprecated(response: ResponseBody): List<Thread> {
        return parser.fromJson(response.string(), object : TypeToken<List<Thread>>() {}.type)
    }

    //     TODO: handle case where thread is deleted
    suspend fun getFeeds(uuid: String, page: Int): List<Thread> {
        try {
            val rawResponse =
                withContext(Dispatchers.IO) {
                    Timber.i("Downloading Feeds on page $page...")
                    service.getNMBFeeds(uuid, page).execute().body()!!
                }
            return withContext(Dispatchers.Default) {
                Timber.i("Parsing Feeds...")
                parseThreadsDeprecated(rawResponse)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to get Feeds")
            throw e
        }
    }

    suspend fun addFeed(uuid: String, tid: String): String {
        try {
            return withContext(Dispatchers.IO) {
                Timber.i("Adding Feed $tid...")
                service.addNMBFeed(uuid, tid).execute().body()!!.string()
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to add feed")
            throw e
        }
    }

    suspend fun delFeed(uuid: String, tid: String): String {
        try {
            return withContext(Dispatchers.IO) {
                Timber.i("Deleting Feed $tid...")
                service.delNMBFeed(uuid, tid).execute().body()!!.string()
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete feed")
            throw e
        }
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
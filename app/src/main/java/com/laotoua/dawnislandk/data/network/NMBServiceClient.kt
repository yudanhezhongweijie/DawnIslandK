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
import org.apache.commons.text.StringEscapeUtils
import org.jsoup.Jsoup
import retrofit2.Retrofit
import timber.log.Timber
import java.io.File


object NMBServiceClient {
    private val service: NMBService = Retrofit.Builder()
        .baseUrl(Constants.baseCDN)
        .build()
        .create(NMBService::class.java)

    private val parser = Gson()

    private val parseCommunities: (String) -> List<Community> = { response ->
        parser.fromJson(response, object : TypeToken<List<Community>>() {}.type)
    }

    private val parseThreads: (String) -> List<Thread> = { response ->
        parser.fromJson(response, object : TypeToken<List<Thread>>() {}.type)
    }

    private val parseReplys: (String) -> Thread = { response ->
        parser.fromJson(response, Thread::class.java)
    }

    private val parseQuote: (String) -> Reply = { response ->
        parser.fromJson(response, Reply::class.java)
    }

    private val parseStringResponse: (String) -> String = { response ->
        StringEscapeUtils.unescapeJava(
            response.replace("\"", "")
        )
    }

    suspend fun getCommunities(): APIResponse<List<Community>> {
        Timber.i("Downloading Communities and Forums...")
        return APIResponse.create(service.getNMBForumList(), parseCommunities)
    }

    suspend fun getThreads(fid: String, page: Int): APIResponse<List<Thread>> {
        Timber.i("Downloading Threads on Forum $fid...")
        val call =
            if (fid == "-1") service.getNMBTimeLine(page)
            else service.getNMBThreads(fid, page)
        return APIResponse.create(call, parseThreads)
    }

    // TODO get with cookie
    suspend fun getReplys(id: String, page: Int): APIResponse<Thread> {
        Timber.i("Downloading Replys on Thread $id on Page $page...")
        return APIResponse.create(service.getNMBReplys(id, page), parseReplys)
    }

    suspend fun getFeeds(uuid: String, page: Int): APIResponse<List<Thread>> {
        Timber.i("Downloading Feeds on Page $page...")
        return APIResponse.create(service.getNMBFeeds(uuid, page), parseThreads)
    }

    suspend fun getQuote(id: String): APIResponse<Reply> {
        Timber.i("Downloading Quote $id...")
        return APIResponse.create(service.getNMBQuote(id), parseQuote)
    }

    suspend fun addFeed(uuid: String, tid: String): APIResponse<String> {
        Timber.i("Adding Feed $tid...")

        return APIResponse.create(service.addNMBFeed(uuid, tid), parseStringResponse)
    }

    suspend fun delFeed(uuid: String, tid: String): APIResponse<String> {
        Timber.i("Deleting Feed $tid...")
        return APIResponse.create(service.delNMBFeed(uuid, tid), parseStringResponse)
    }

    // TODO: use APIResponse
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
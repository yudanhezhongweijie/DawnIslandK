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

    suspend fun getCommunities(): APIDataResponse<List<Community>> {
        Timber.i("Downloading Communities and Forums...")
        return APIDataResponse.create(service.getNMBForumList(), parseCommunities)
    }

    suspend fun getThreads(fid: String, page: Int): APIDataResponse<List<Thread>> {
        Timber.i("Downloading Threads on Forum $fid...")
        val call =
            if (fid == "-1") service.getNMBTimeLine(page)
            else service.getNMBThreads(fid, page)
        return APIDataResponse.create(call, parseThreads)
    }

    // TODO get with cookie
    suspend fun getReplys(id: String, page: Int): APIDataResponse<Thread> {
        Timber.i("Downloading Replys on Thread $id on Page $page...")
        return APIDataResponse.create(service.getNMBReplys(id, page), parseReplys)
    }

    suspend fun getFeeds(uuid: String, page: Int): APIDataResponse<List<Thread>> {
        Timber.i("Downloading Feeds on Page $page...")
        return APIDataResponse.create(service.getNMBFeeds(uuid, page), parseThreads)
    }

    suspend fun getQuote(id: String): APIDataResponse<Reply> {
        Timber.i("Downloading Quote $id...")
        return APIDataResponse.create(service.getNMBQuote(id), parseQuote)
    }

    suspend fun addFeed(uuid: String, tid: String): APIMessageResponse {
        Timber.i("Adding Feed $tid...")
        return APIMessageResponse.create(service.addNMBFeed(uuid, tid))
    }

    suspend fun delFeed(uuid: String, tid: String): APIMessageResponse {
        Timber.i("Deleting Feed $tid...")
        return APIMessageResponse.create(service.delNMBFeed(uuid, tid))
    }

    suspend fun sendPost(
        newPost: Boolean,
        targetId: String, name: String?,
        email: String?, title: String?,
        content: String?, water: String?,
        image: File?, userhash: String
    ): APIMessageResponse {
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
            val call = if (newPost) {
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
            }
            APIMessageResponse.create(call)
        }
    }


}
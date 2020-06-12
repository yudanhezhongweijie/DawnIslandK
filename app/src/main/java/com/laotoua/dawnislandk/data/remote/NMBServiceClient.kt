package com.laotoua.dawnislandk.data.remote

import com.laotoua.dawnislandk.data.local.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import timber.log.Timber
import java.io.File
import javax.inject.Inject

class NMBServiceClient @Inject constructor(private val service: NMBService) {

    private val moshi: Moshi = Moshi.Builder().build()

    private val parseNMBNotice: (String) -> NMBNotice = { response ->
        moshi.adapter(NMBNotice::class.java).fromJson(response)!!
    }

    private val parseLuweiNotice: (String) -> LuweiNotice = { response ->
        moshi.adapter(LuweiNotice::class.java).fromJson(response)!!
    }

    private val parseCommunities: (String) -> List<Community> = { response ->
        moshi.adapter<List<Community>>(
            Types.newParameterizedType(
                List::class.java,
                Community::class.java
            )
        ).fromJson(response)!!
    }

    private val parseThreads: (String) -> List<Thread> = { response ->
        moshi.adapter<List<Thread>>(
            Types.newParameterizedType(
                List::class.java,
                Thread::class.java
            )
        ).fromJson(response)!!
    }

    private val parseReplys: (String) -> Thread = { response ->
        moshi.adapter(Thread::class.java).fromJson(response)!!
    }

    private val parseQuote: (String) -> Reply = { response ->
        moshi.adapter(Reply::class.java).fromJson(response)!!
    }

    suspend fun getNMBNotice(): APIDataResponse<NMBNotice> {
        Timber.i("Downloading Notice...")
        return APIDataResponse.create(service.getNMBNotice(), parseNMBNotice)
    }

    suspend fun getLuweiNotice(): APIDataResponse<LuweiNotice>{
        Timber.i("Downloading LuWeiNotice...")
        return APIDataResponse.create(service.getLuweiNotice(), parseLuweiNotice)
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

    suspend fun getReplys(userhash: String?, id: String, page: Int): APIDataResponse<Thread> {
        Timber.i("Downloading Replys on Thread $id on Page $page...")
        val hash = userhash?.let { "userhash=$it" }
        return APIDataResponse.create(service.getNMBReplys(hash, id, page), parseReplys)
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
            Timber.d("Posting to $targetId...")
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
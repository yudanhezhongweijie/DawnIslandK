package com.laotoua.dawnislandk.data.remote

import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.*

interface NMBService {
    @GET("https://cover.acfunwiki.org/nmb-notice.json")
    fun getNMBNotice(): Call<ResponseBody>

    @GET("https://cover.acfunwiki.org/luwei.json")
    fun getLuweiNotice(): Call<ResponseBody>

    @GET("Api/getForumList")
    fun getNMBForumList(): Call<ResponseBody>

    @GET("Api/showf")
    fun getNMBThreads(@Query("id") fid: String, @Query("page") page: Int): Call<ResponseBody>

    @GET("Api/feed")
    fun getNMBFeeds(@Query("uuid") uuid: String, @Query("page") page: Int): Call<ResponseBody>

    @GET("Api/addFeed")
    fun addNMBFeed(@Query("uuid") uuid: String, @Query("tid") tid: String): Call<ResponseBody>

    @GET("Api/delFeed")
    fun delNMBFeed(@Query("uuid") uuid: String, @Query("tid") tid: String): Call<ResponseBody>

    @GET("Api/timeline")
    fun getNMBTimeLine(@Query("page") page: Int): Call<ResponseBody>

    @GET("Api/thread")
    fun getNMBReplys(
        @Header("Cookie") hash: String?,
        @Query("id") id: String,
        @Query("page") page: Int
    ): Call<ResponseBody>

    @GET("Api/ref")
    fun getNMBQuote(@Query("id") id: String): Call<ResponseBody>

    @Multipart
    @POST("Home/Forum/doReplyThread.html")
    fun postReply(
        @Part("resto") resto: RequestBody, @Part("name") name: RequestBody?,
        @Part("email") email: RequestBody?, @Part("title") title: RequestBody?,
        @Part("content") content: RequestBody?, @Part("water") water: RequestBody?,
        @Part image: MultipartBody.Part?, @Header("Cookie") hash: String
    ): Call<ResponseBody>

    @Multipart
    @POST("Home/Forum/doPostThread.html")
    fun postThread(
        @Part("fid") fid: RequestBody, @Part("name") name: RequestBody?,
        @Part("email") email: RequestBody?, @Part("title") title: RequestBody?,
        @Part("content") content: RequestBody?, @Part("water") water: RequestBody?,
        @Part image: MultipartBody.Part?, @Header("Cookie") hash: String
    ): Call<ResponseBody>
}


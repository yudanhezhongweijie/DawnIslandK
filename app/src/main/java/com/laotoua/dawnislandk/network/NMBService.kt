package com.laotoua.dawnislandk.network

import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.*

interface NMBService {
    @GET("Api/getForumList")
    fun getNMBForumList(): Call<ResponseBody>

    @GET("Api/showf")
    fun getNMBThreads(@Query("id") fid: String, @Query("page") page: Int): Call<ResponseBody>

    @GET("Api/feed")
    fun getNMBFeeds(@Query("uuid") fid: String, @Query("page") page: Int): Call<ResponseBody>

    @GET("Api/timeline")
    fun getNMBTimeLine(@Query("page") page: Int): Call<ResponseBody>

    @GET("Api/thread")
    fun getNMBReplys(@Query("id") id: String, @Query("page") page: Int): Call<ResponseBody>

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


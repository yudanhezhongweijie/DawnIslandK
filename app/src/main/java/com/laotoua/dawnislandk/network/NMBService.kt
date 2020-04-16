package com.laotoua.dawnislandk.network

import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.*

interface NMBService {
    @GET("api/getForumList")
    fun getNMBForumList(): Call<ResponseBody>

    @GET("api/showf")
    fun getNMBThreads(@Query("id") fid: String, @Query("page") page: Int): Call<ResponseBody>

    @GET("api/timeline")
    fun getNMBTimeLine(@Query("page") page: Int): Call<ResponseBody>

    @GET("api/thread")
    fun getNMBReplys(@Query("id") id: String, @Query("page") page: Int): Call<ResponseBody>

    @GET("api/ref")
    fun getNMBQuote(@Query("id") id: String): Call<ResponseBody>

    @Multipart
    @POST("Home/Forum/doReplyThread.html")
    fun sendReply(
        @Part("resto") resto: RequestBody, @Part("name") name: RequestBody?,
        @Part("email") email: RequestBody?, @Part("title") title: RequestBody?,
        @Part("content") content: RequestBody?, @Part("water") water: RequestBody?,
        @Part image: MultipartBody.Part?, @Header("Cookie") hash: String
    ): Call<ResponseBody>

}


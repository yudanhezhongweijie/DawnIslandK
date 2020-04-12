package com.laotoua.dawnislandk.network

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface NMBService {
    @GET("/api/getForumList")
    fun getNMBForumList(): Call<ResponseBody>

    @GET("/api/showf")
    fun getNMBThreads(@Query("id") fid: String, @Query("page") page: Int): Call<ResponseBody>

    @GET("/api/timeline")
    fun getNMBTimeLine(@Query("page") page: Int): Call<ResponseBody>

    @GET("/api/thread")
    fun getNMBReplys(@Query("id") id: String, @Query("page") page: Int): Call<ResponseBody>

    @GET("/api/ref")
    fun getNMBQuote(@Query("id") id: String): Call<ResponseBody>
}


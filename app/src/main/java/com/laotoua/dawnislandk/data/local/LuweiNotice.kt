package com.laotoua.dawnislandk.data.local

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class LuweiNotice(
    @Json(name = "appstore")
    val appVersion: Map<String, Boolean>,
    @Json(name = "beitaiForum")
    val beitaiForums: List<NoticeForum>,
    @Json(name = "forum")
    val nmbForums: List<NoticeForum>,
    @Json(name = "loading")
    val loadingMsgs: List<String>,
    @Json(name = "update")
    val clientsInfo: Map<String, ClientInfo>,
    @Json(name = "whitelist")
    val whitelist: WhiteList
) {
    @JsonClass(generateAdapter = true)
    data class ClientInfo(
        val description: String="",
        val url: String="",
        val version: String="",
        @Json(name = "versionid")
        val versionId: Int=0
    )
    @JsonClass(generateAdapter = true)
    data class WhiteList(val date: Long, val threads: List<String>)
}
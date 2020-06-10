package com.laotoua.dawnislandk.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.util.*

@JsonClass(generateAdapter = true)
@Entity
data class LuweiNotice(
    @PrimaryKey(autoGenerate = true)
    val id: Int? = null, // only used to keep track of versions
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
    val whitelist: WhiteList,
    var lastUpdatedAt: Long = 0
) {

    override fun equals(other: Any?) =
        if (other is LuweiNotice) {
            appVersion == other.appVersion && beitaiForums == other.beitaiForums
                    && nmbForums == other.nmbForums && loadingMsgs == other.loadingMsgs
                    && clientsInfo == other.clientsInfo && whitelist == other.whitelist
        } else false

    override fun hashCode(): Int {
        var result = appVersion.hashCode()
        result = 31 * result + beitaiForums.hashCode()
        result = 31 * result + nmbForums.hashCode()
        result = 31 * result + loadingMsgs.hashCode()
        result = 31 * result + clientsInfo.hashCode()
        result = 31 * result + whitelist.hashCode()
        return result
    }

    fun setUpdatedTimestamp(time: Long? = null) {
        lastUpdatedAt = time ?: Date().time
    }

    @JsonClass(generateAdapter = true)
    data class ClientInfo(
        val description: String = "",
        val url: String = "",
        val version: String = "",
        @Json(name = "versionid")
        val versionId: Int = 0
    )

    @JsonClass(generateAdapter = true)
    data class WhiteList(val date: Long, val threads: List<String>)
}
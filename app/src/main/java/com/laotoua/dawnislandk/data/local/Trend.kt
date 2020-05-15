package com.laotoua.dawnislandk.data.local

data class Trend(
    val rank: String,
    val hits: String,
    val forum: String,
    val id: String,
    val content: String
) {
    fun toThread(fid: String): Thread {
        return Thread(
            id = id,
            fid = fid,
            img = "",
            ext = "",
            now = "",
            userid = "",
            name = "",
            email = "",
            title = "",
            content = "",
            admin = ""
        )

    }
}
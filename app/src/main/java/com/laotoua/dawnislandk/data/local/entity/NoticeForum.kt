package com.laotoua.dawnislandk.data.local.entity

import com.squareup.moshi.JsonClass

// from http://cover.acfunwiki.org/luwei.json, differs from Forum
@JsonClass(generateAdapter = true)
data class NoticeForum(
    val id: String,
    val sort: String = "",
    val name: String,
    val showName: String = "",
    val fgroup: String,
    val rule: String = "" // default rule
){
    fun getDisplayName():String = if (showName.isNotBlank()) showName else name

    fun getPostRule():String = if (rule.isNotBlank()) rule else "请遵守总版规"
}

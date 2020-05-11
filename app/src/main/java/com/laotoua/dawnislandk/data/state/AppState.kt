package com.laotoua.dawnislandk.data.state

import com.laotoua.dawnislandk.data.entity.Cookie
import com.laotoua.dawnislandk.data.entity.DawnDatabase

object AppState {
    private var mDb: DawnDatabase? = null
    val DB: DawnDatabase get() = mDb!!

    fun setDB(db: DawnDatabase) {
        mDb = db
    }

    private var mCookies = mutableListOf<Cookie>()
    val cookies get() = mCookies

    suspend fun loadCookies() {
        mCookies = DB.cookieDao().getAll().toMutableList()
    }

    suspend fun addCookie(cookie: Cookie) {
        mCookies.add(cookie)
        DB.cookieDao().insert(cookie)
    }

    suspend fun deleteCookies(cookie: Cookie) {
        mCookies.remove(cookie)
        DB.cookieDao().delete(cookie)
    }

    private var mFeedId: String? = null
    val feedId get() = mFeedId!!

    fun setFeedId(string: String) {
        mFeedId = string
    }
}
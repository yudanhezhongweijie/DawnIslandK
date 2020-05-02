package com.laotoua.dawnislandk.data.state

import android.content.Context
import com.laotoua.dawnislandk.data.entity.Cookie
import com.laotoua.dawnislandk.data.entity.DawnDatabase
import com.laotoua.dawnislandk.ui.viewfactory.ThreadCardFactory

object AppState {

    private var threadCardFactory: ThreadCardFactory? = null

    fun getThreadCardFactory(context: Context): ThreadCardFactory {
        if (threadCardFactory == null) threadCardFactory = ThreadCardFactory(context)
        return threadCardFactory!!
    }

    private var mDb: DawnDatabase? = null
    val DB: DawnDatabase get() = mDb!!

    fun setDB(db: DawnDatabase) {
        mDb = db
    }

    private var mCookies: List<Cookie>? = null
    val cookies get() = mCookies

    suspend fun loadCookies() {
        mCookies = DB.cookieDao().getAll()
    }

    private var mFeedId: String? = null
    val feedId get() = mFeedId!!

    fun setFeedId(string: String) {
        mFeedId = string
    }

}
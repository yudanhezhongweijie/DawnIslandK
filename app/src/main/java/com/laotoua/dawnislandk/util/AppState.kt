package com.laotoua.dawnislandk.util

import android.content.Context
import com.laotoua.dawnislandk.entity.Cookie
import com.laotoua.dawnislandk.entity.DawnDatabase
import com.laotoua.dawnislandk.ui.viewfactory.ThreadCardFactory

object AppState {
    private var threadCardFactory: ThreadCardFactory? = null

    fun getThreadCardFactory(context: Context): ThreadCardFactory {
        if (threadCardFactory == null) threadCardFactory =
            ThreadCardFactory(context)
        return threadCardFactory!!
    }

    private var _DB: DawnDatabase? = null
    val DB: DawnDatabase get() = _DB!!

    fun setDB(db: DawnDatabase) {
        this._DB = db
    }

    private var _cookies: List<Cookie>? = null
    val cookies get() = _cookies

    suspend fun loadCookies() {
        _cookies = DB.cookieDao().getAll()
    }

}
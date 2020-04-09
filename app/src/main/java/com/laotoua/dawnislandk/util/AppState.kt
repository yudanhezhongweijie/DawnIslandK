package com.laotoua.dawnislandk.util

import android.content.Context
import com.laotoua.dawnislandk.components.ThreadCardFactory
import com.laotoua.dawnislandk.entities.DawnDatabase

object AppState {
    private var threadCardFactory: ThreadCardFactory? = null

    fun getThreadCardFactory(context: Context): ThreadCardFactory {
        if (threadCardFactory == null) threadCardFactory = ThreadCardFactory(context)
        return threadCardFactory!!
    }

    private var _DB: DawnDatabase? = null
    val DB: DawnDatabase get() = _DB!!

    fun setDB(db: DawnDatabase) {
        this._DB = db
    }
}
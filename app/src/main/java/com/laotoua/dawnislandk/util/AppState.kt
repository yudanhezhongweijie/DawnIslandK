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

    private var db: DawnDatabase? = null

    fun setDB(db: DawnDatabase) {
        this.db = db
    }

    fun getDB(): DawnDatabase {
        return this.db!!
    }
}
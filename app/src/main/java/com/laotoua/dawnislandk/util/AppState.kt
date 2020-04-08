package com.laotoua.dawnislandk.util

import android.content.Context
import com.laotoua.dawnislandk.components.ThreadCardFactory

object AppState {
    private var threadCardFactory: ThreadCardFactory? = null

    fun getThreadCardFactory(context: Context): ThreadCardFactory {
        if (threadCardFactory == null) threadCardFactory = ThreadCardFactory(context)
        return threadCardFactory!!
    }
}
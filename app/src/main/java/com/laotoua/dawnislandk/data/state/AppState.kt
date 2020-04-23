package com.laotoua.dawnislandk.data.state

import android.content.Context
import androidx.preference.PreferenceManager
import com.laotoua.dawnislandk.data.entity.Cookie
import com.laotoua.dawnislandk.data.entity.DawnDatabase
import com.laotoua.dawnislandk.ui.viewfactory.ThreadCardFactory
import java.util.*

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

    private var mFeedsId: String? = null
    val feedsId get() = mFeedsId!!

    fun loadFeedsId(context: Context): String {
        if (mFeedsId == null) {
            PreferenceManager.getDefaultSharedPreferences(context).run {
                mFeedsId = getString("feedId", null)
                if (mFeedsId == null) {
                    mFeedsId = UUID.randomUUID().toString()
                    edit().putString(
                        "feedId",
                        mFeedsId
                    ).apply()
                }

            }
        }
        return mFeedsId!!
    }

}
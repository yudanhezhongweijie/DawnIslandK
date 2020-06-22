package com.laotoua.dawnislandk.screens.history

import androidx.lifecycle.ViewModel
import com.laotoua.dawnislandk.data.local.dao.BrowsingHistoryDao
import javax.inject.Inject

class BrowsingHistoryViewModel @Inject constructor(browsedPostDaoBrowsing: BrowsingHistoryDao) :
    ViewModel() {

    val browsingHistoryList = browsedPostDaoBrowsing.getAllBrowsingHistoryAndPost()

}
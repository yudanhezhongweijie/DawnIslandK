package com.laotoua.dawnislandk.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laotoua.dawnislandk.entity.Thread
import com.laotoua.dawnislandk.network.NMBServiceClient
import com.laotoua.dawnislandk.util.AppState
import kotlinx.coroutines.launch
import timber.log.Timber

class FeedViewModel : ViewModel() {
    // TODO: Implement the ViewModel
    private val feedsList = mutableListOf<Thread>()
    private val feedsIds = mutableSetOf<String>()
    private var _feeds = MutableLiveData<List<Thread>>()
    val feeds: LiveData<List<Thread>> get() = _feeds
    private var pageCount = 1
    private var _loadFail = MutableLiveData(false)
    val loadFail: LiveData<Boolean>
        get() = _loadFail

    init {
        getFeeds()
    }

    fun getFeeds() {
        viewModelScope.launch {
            try {
                Timber.i("Getting Feeds...")
                val list = NMBServiceClient.getFeeds(AppState.feedsId, pageCount)
                val noDuplicates = list.filterNot { feedsIds.contains(it.id) }
                if (noDuplicates.isNotEmpty()) {
                    feedsIds.addAll(noDuplicates.map { it.id })
                    feedsList.addAll(noDuplicates)
                    Timber.i(
                        "feedsList now have ${feedsList.size} feeds"
                    )
                    _feeds.postValue(feedsList)
                    _loadFail.postValue(false)
                    pageCount += 1
                } else {
                    Timber.i("feedsList has no new feeds.")
                    _loadFail.postValue(true)
                }
            } catch (e: Exception) {
                Timber.e(e, "failed to get feeds")
                _loadFail.postValue(true)
            }
        }
    }
}

package com.laotoua.dawnislandk.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laotoua.dawnislandk.data.entity.Thread
import com.laotoua.dawnislandk.data.network.NMBServiceClient
import com.laotoua.dawnislandk.data.state.AppState
import com.laotoua.dawnislandk.data.util.SingleLiveEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.apache.commons.text.StringEscapeUtils
import timber.log.Timber

class FeedViewModel : ViewModel() {
    // TODO: Implement the ViewModel
    private val feedsList = mutableListOf<Thread>()
    private val feedsIds = mutableSetOf<String>()
    private var _feeds = MutableLiveData<List<Thread>>()
    val feeds: LiveData<List<Thread>> get() = _feeds
    private var page = 1
    private var _loadFail = MutableLiveData<Boolean>()
    val loadFail: LiveData<Boolean>
        get() = _loadFail

    private val _delFeedResponse = MutableLiveData<SingleLiveEvent<Pair<String, Int>>>()
    val delFeedResponse: LiveData<SingleLiveEvent<Pair<String, Int>>> get() = _delFeedResponse

    init {
        getFeeds()
    }

    fun getFeeds() {
        viewModelScope.launch {
            Timber.i("Downloading Feeds on page $page")
            DataResource.create(NMBServiceClient.getFeeds(AppState.feedsId, page)).run {
                when (this) {
                    is DataResource.Error -> {
                        Timber.e(message)
                        _loadFail.postValue(true)
                    }
                    is DataResource.Success -> {
                        convertFeedData(data!!)
                        _loadFail.postValue(false)
                    }
                }
            }
        }
    }

    private fun convertFeedData(data: List<Thread>) {
        val noDuplicates = data.filterNot { feedsIds.contains(it.id) }
        if (noDuplicates.isNotEmpty()) {
            feedsIds.addAll(noDuplicates.map { it.id })
            feedsList.addAll(noDuplicates)
            Timber.i(
                "feedsList now have ${feedsList.size} feeds"
            )
            _feeds.postValue(feedsList)
            _loadFail.postValue(false)
            if (feedsList.size % 10 == 0) page += 1
        } else {
            Timber.i("feedsList has no new feeds.")
            _loadFail.postValue(true)
        }
    }

    fun deleteFeed(id: String, position: Int) {
        Timber.i("Deleting Feed $id")
        viewModelScope.launch(Dispatchers.IO) {
            NMBServiceClient.delFeed(AppState.feedsId, id).run {
                // TODO: check failure response
                /** res:
                 *  "\u53d6\u6d88\u8ba2\u9605\u6210\u529f!"
                 */
                val msg = StringEscapeUtils.unescapeJava(this.replace("\"", ""))
                SingleLiveEvent(
                    Pair(
                        msg,
                        position
                    )
                )
                    .run {
                        _delFeedResponse.postValue(this)
                    }

            }
        }
    }

    fun refresh() {
        feedsList.clear()
        feedsIds.clear()
        page = 1
        getFeeds()
    }
}

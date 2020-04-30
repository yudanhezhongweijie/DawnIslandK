package com.laotoua.dawnislandk.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laotoua.dawnislandk.data.entity.Thread
import com.laotoua.dawnislandk.data.network.APISuccessMessageResponse
import com.laotoua.dawnislandk.data.network.NMBServiceClient
import com.laotoua.dawnislandk.data.state.AppState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

class FeedViewModel : ViewModel() {
    private val feedsList = mutableListOf<Thread>()
    private val feedsIds = mutableSetOf<String>()
    private var _feeds = MutableLiveData<List<Thread>>()
    val feeds: LiveData<List<Thread>> get() = _feeds
    private var nextPage = 1
    private var _loadingStatus = MutableLiveData<SingleLiveEvent<EventPayload<Nothing>>>()
    val loadingStatus: LiveData<SingleLiveEvent<EventPayload<Nothing>>>
        get() = _loadingStatus

    private val _delFeedResponse = MutableLiveData<SingleLiveEvent<EventPayload<Int>>>()
    val delFeedResponse: LiveData<SingleLiveEvent<EventPayload<Int>>> get() = _delFeedResponse

    /** server returns variable amount of feeds on pages,
     *  requesting two pages at the same time less likely to miss data
     */
    fun get2PagesFeeds() {
        getFeedOnPage(nextPage)
        getFeedOnPage(nextPage + 1)
    }

    private fun getFeedOnPage(page: Int) {
        viewModelScope.launch {
            _loadingStatus.postValue(SingleLiveEvent.create(LoadingStatus.LOADING))
            Timber.i("Downloading Feeds on page $page...")
            DataResource.create(NMBServiceClient.getFeeds(AppState.feedsId, page)).run {
                when (this) {
                    is DataResource.Error -> {
                        Timber.e(message)
                        _loadingStatus.postValue(
                            SingleLiveEvent.create(
                                LoadingStatus.FAILED,
                                "无法读取订阅...\n$message"
                            )
                        )
                    }
                    is DataResource.Success -> {
                        convertFeedData(data!!)
                    }
                }
            }
        }
    }

    private fun convertFeedData(data: List<Thread>) {
        if (data.isNotEmpty()) nextPage += 1 else nextPage -= 1
        val noDuplicates = data.filterNot { feedsIds.contains(it.id) }
        if (noDuplicates.isNotEmpty()) {
            feedsIds.addAll(noDuplicates.map { it.id })
            feedsList.addAll(noDuplicates)
            Timber.i(
                "feedsList now has ${feedsList.size} feeds"
            )
            _feeds.postValue(feedsList)
            _loadingStatus.postValue(SingleLiveEvent.create(LoadingStatus.SUCCESS))
        } else {
            Timber.i("feedsList not updated, still has ${feedsList.size} feeds.")
            _loadingStatus.postValue(SingleLiveEvent.create(LoadingStatus.NODATA))
        }
    }

    fun deleteFeed(id: String, position: Int) {
        Timber.i("Deleting Feed $id")
        viewModelScope.launch(Dispatchers.IO) {
            NMBServiceClient.delFeed(AppState.feedsId, id).run {
                when (this) {
                    is APISuccessMessageResponse -> {
                        feedsList.removeAt(position)
                        feedsIds.remove(id)
                        _delFeedResponse.postValue(
                            SingleLiveEvent.create(
                                LoadingStatus.SUCCESS,
                                message,
                                position
                            )
                        )
                    }
                    else -> {
                        Timber.e("Response type: ${this.javaClass.simpleName}")
                        Timber.e(message)
                        _delFeedResponse.postValue(
                            SingleLiveEvent.create(
                                LoadingStatus.FAILED,
                                "删除订阅失败"
                            )
                        )
                    }
                }
            }
        }
    }

    fun refresh() {
        feedsList.clear()
        feedsIds.clear()
        nextPage = 1
        get2PagesFeeds()
    }
}

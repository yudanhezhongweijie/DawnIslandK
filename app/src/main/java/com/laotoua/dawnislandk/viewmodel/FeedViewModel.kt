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
    private var page = 1
    private var _loadingStatus = MutableLiveData<SingleLiveEvent<EventPayload<Nothing>>>()
    val loadingStatus: LiveData<SingleLiveEvent<EventPayload<Nothing>>>
        get() = _loadingStatus

    private val _delFeedResponse = MutableLiveData<SingleLiveEvent<EventPayload<Int>>>()
    val delFeedResponse: LiveData<SingleLiveEvent<EventPayload<Int>>> get() = _delFeedResponse

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
        val noDuplicates = data.filterNot { feedsIds.contains(it.id) }
        if (noDuplicates.isNotEmpty()) {
            feedsIds.addAll(noDuplicates.map { it.id })
            feedsList.addAll(noDuplicates)
            Timber.i(
                "feedsList now have ${feedsList.size} feeds"
            )
            _feeds.postValue(feedsList)

            if (feedsList.size % 10 == 0) page += 1
        } else {
            Timber.i("feedsList has no new feeds.")
            _loadingStatus.postValue(
                SingleLiveEvent.create(
                    LoadingStatus.NODATA,
                    "feedsList has no new feeds."
                )

            )
        }
    }

    fun deleteFeed(id: String, position: Int) {
        Timber.i("Deleting Feed $id")
        viewModelScope.launch(Dispatchers.IO) {
            NMBServiceClient.delFeed(AppState.feedsId, id).run {
                when (this) {
                    is APISuccessMessageResponse -> {
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
        page = 1
        getFeeds()
    }
}

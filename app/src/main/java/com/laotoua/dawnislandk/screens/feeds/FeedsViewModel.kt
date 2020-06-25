package com.laotoua.dawnislandk.screens.feeds

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laotoua.dawnislandk.DawnApp.Companion.applicationDataStore
import com.laotoua.dawnislandk.data.local.entity.Post
import com.laotoua.dawnislandk.data.remote.APIDataResponse
import com.laotoua.dawnislandk.data.remote.APIMessageResponse
import com.laotoua.dawnislandk.data.remote.NMBServiceClient
import com.laotoua.dawnislandk.util.EventPayload
import com.laotoua.dawnislandk.util.LoadingStatus
import com.laotoua.dawnislandk.util.SingleLiveEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

class FeedsViewModel @Inject constructor(private val webService: NMBServiceClient) : ViewModel() {
    private val feedsList = mutableListOf<Post>()
    private val feedsIds = mutableSetOf<String>()
    private var _feeds = MutableLiveData<List<Post>>()
    val feeds: LiveData<List<Post>> get() = _feeds
    private val nextPage get() = feedsList.size / 10 + 1
    private var _loadingStatus = MutableLiveData<SingleLiveEvent<EventPayload<Nothing>>>()
    val loadingStatus: LiveData<SingleLiveEvent<EventPayload<Nothing>>>
        get() = _loadingStatus

    private val _delFeedResponse = MutableLiveData<SingleLiveEvent<EventPayload<Int>>>()
    val delFeedResponse: LiveData<SingleLiveEvent<EventPayload<Int>>> get() = _delFeedResponse

    fun getNextPage() {
        getFeedOnPage(nextPage)
    }

    private fun getFeedOnPage(page: Int) {
        viewModelScope.launch {
            _loadingStatus.postValue(SingleLiveEvent.create(LoadingStatus.LOADING))
            Timber.i("Downloading Feeds on page $page...")
            webService.getFeeds(applicationDataStore.feedId, page).run {
                when (this) {
                    is APIDataResponse.APIErrorDataResponse -> {
                        Timber.e(message)
                        _loadingStatus.postValue(
                            SingleLiveEvent.create(LoadingStatus.FAILED, "无法读取订阅...\n$message")
                        )
                    }
                    is APIDataResponse.APISuccessDataResponse -> {
                        convertFeedData(data)
                    }
                }
            }
        }
    }

    private fun convertFeedData(data: List<Post>) {
        val noDuplicates = data.filterNot { feedsIds.contains(it.id) }
        if (noDuplicates.isEmpty()) {
            _loadingStatus.postValue(SingleLiveEvent.create(LoadingStatus.NODATA))
            return
        }
        feedsIds.addAll(noDuplicates.map { it.id })
        feedsList.addAll(noDuplicates)
        Timber.i("feedsList now has ${feedsList.size} feeds")
        _feeds.postValue(feedsList)
        if (noDuplicates.size < 10){
            _loadingStatus.postValue(SingleLiveEvent.create(LoadingStatus.NODATA))
        }else {
            _loadingStatus.postValue(SingleLiveEvent.create(LoadingStatus.SUCCESS))
        }
    }

    fun deleteFeed(id: String, position: Int) {
        Timber.i("Deleting Feed $id")
        viewModelScope.launch(Dispatchers.IO) {
            webService.delFeed(applicationDataStore.feedId, id).run {
                when (this) {
                    is APIMessageResponse.APISuccessMessageResponse -> {
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
        getFeedOnPage(nextPage)
    }
}

package com.laotoua.dawnislandk.screens.trend

import androidx.lifecycle.*
import com.laotoua.dawnislandk.data.local.DailyTrend
import com.laotoua.dawnislandk.data.local.Trend
import com.laotoua.dawnislandk.data.repository.TrendRepository
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

class TrendsViewModel @Inject constructor(
    private val trendRepo: TrendRepository
) : ViewModel() {

    val trends = Transformations.map(trendRepo.dailyTrend){
        it.trends
    }

    val loadingStatus = trendRepo.loadingStatus

    fun getLatestTrend() {
        Timber.i("Refreshing Trend...")
        viewModelScope.launch {
            trendRepo.getLatestTrend()
        }
    }
}

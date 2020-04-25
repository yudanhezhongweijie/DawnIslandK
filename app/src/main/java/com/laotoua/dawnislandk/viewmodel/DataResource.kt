package com.laotoua.dawnislandk.viewmodel

import com.laotoua.dawnislandk.data.network.APIDataResponse
import com.laotoua.dawnislandk.data.network.APISuccessDataResponse
import timber.log.Timber

sealed class DataResource<T>(
    val message: String,
    val data: T?
) {
    companion object {
        fun <T> create(dataResponse: APIDataResponse<T>): DataResource<T> {
            return when (dataResponse) {
                is APISuccessDataResponse -> {
                    Success(dataResponse.message, dataResponse.data)
                }
                else -> {
                    Timber.e("${dataResponse.javaClass.simpleName}: ${dataResponse.message}")
                    Error(dataResponse.message, dataResponse.data)
                }
            }
        }
    }

    class Success<T>(message: String, data: T) : DataResource<T>(message, data)
    class Error<T>(message: String, data: T? = null) : DataResource<T>(message, data)
}



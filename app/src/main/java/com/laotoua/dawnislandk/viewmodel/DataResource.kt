package com.laotoua.dawnislandk.viewmodel

import com.laotoua.dawnislandk.data.network.APIResponse
import com.laotoua.dawnislandk.data.network.APISuccessResponse
import timber.log.Timber

sealed class DataResource<T>(
    val message: String,
    val data: T?
) {
    companion object {
        fun <T> create(response: APIResponse<T>): DataResource<T> {
            return when (response) {
                is APISuccessResponse -> {
                    Success(response.message, response.data)
                }
                else -> {
                    Timber.e("${response.javaClass.simpleName}: ${response.message}")
                    Error(response.message, response.data)
                }
            }
        }
    }

    class Success<T>(message: String, data: T) : DataResource<T>(message, data)
    class Error<T>(message: String, data: T? = null) : DataResource<T>(message, data)
}



package com.laotoua.dawnislandk.data.repository

import com.laotoua.dawnislandk.data.remote.APIDataResponse
import com.laotoua.dawnislandk.data.remote.APISuccessDataResponse

sealed class DataResource<T>(
    val message: String,
    val data: T?
) {
    companion object {
        fun <T> create(dataResponse: APIDataResponse<T>): DataResource<T> {
            return when (dataResponse) {
                is APISuccessDataResponse -> Success(dataResponse.message, dataResponse.data)
                else -> Error(dataResponse.message, dataResponse.data)
            }
        }

        fun <T> create(data: T): DataResource<T> = Success("Room data", data)
    }

    class Success<T>(message: String, data: T) : DataResource<T>(message, data)
    class Error<T>(message: String, data: T? = null) : DataResource<T>(message, data)
}



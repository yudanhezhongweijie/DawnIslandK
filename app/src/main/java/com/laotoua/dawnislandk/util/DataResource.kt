/*
 *  Copyright 2020 Fishballzzz
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package com.laotoua.dawnislandk.util

import com.laotoua.dawnislandk.data.remote.APIDataResponse

sealed class DataResource<T> {
    abstract val status: LoadingStatus
    abstract val data: T?
    abstract var message: String

    data class SuccessDataResource<T>(
        override val data: T
    ) : DataResource<T>() {
        override var message: String = ""
        override val status: LoadingStatus = LoadingStatus.SUCCESS
    }

    data class ErrorDataResource<T>(
        override var message: String
    ) : DataResource<T>() {
        override val status: LoadingStatus = LoadingStatus.ERROR
        override val data: T? = null
    }

    data class NoDataResource<T>(
        override var message: String
    ) : DataResource<T>() {
        override val status: LoadingStatus = LoadingStatus.NO_DATA
        override val data: T? = null
    }

    data class LoadingDataResource<T>(override val status: LoadingStatus = LoadingStatus.LOADING) :
        DataResource<T>() {
        override val data: T? = null
        override var message: String = ""
    }

    companion object {
        fun <T> create(
            status: LoadingStatus = LoadingStatus.LOADING,
            data: T? = null,
            message: String = ""
        ): DataResource<T> {
            return if (status == LoadingStatus.SUCCESS && data != null) {
                SuccessDataResource(data)
            } else if (status == LoadingStatus.SUCCESS || status == LoadingStatus.NO_DATA) {
                NoDataResource(message)
            } else if (status == LoadingStatus.LOADING) {
                LoadingDataResource()
            } else {
                ErrorDataResource(
                    message
                )
            }
        }

        fun <T> create(response: APIDataResponse<T>): DataResource<T> {
            return create(
                response.status,
                response.data,
                response.message
            )
        }
    }
}
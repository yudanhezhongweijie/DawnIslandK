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

sealed class DataResource<T>(
    val status: LoadingStatus,
    val data: T? = null,
    var message: String
) {

    class Success<T>(data: T) : DataResource<T>(LoadingStatus.SUCCESS, data, "")

    class Error<T>(message: String) : DataResource<T>(LoadingStatus.ERROR, null, message)

    class NoData<T>(message: String) : DataResource<T>(LoadingStatus.NO_DATA, null, message)

    class Loading<T>(message: String) : DataResource<T>(LoadingStatus.LOADING, null, message)

    companion object {
        fun <T> create(
            status: LoadingStatus = LoadingStatus.LOADING,
            data: T? = null,
            message: String = ""
        ): DataResource<T> {
            return if (status == LoadingStatus.SUCCESS && data != null) {
                Success(data)
            } else if (status == LoadingStatus.SUCCESS || status == LoadingStatus.NO_DATA) {
                NoData(message)
            } else if (status == LoadingStatus.LOADING) {
                Loading(message)
            } else {
                Error(message)
            }
        }

        fun <T> create(response: APIDataResponse<T>): DataResource<T> {
            return create(response.status, response.data, response.message)
        }
    }
}
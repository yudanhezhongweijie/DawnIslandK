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


class SingleLiveEvent<out T> private constructor(private val content: T) {
    var hasBeenHandled = false
        private set // Allow external read but not write

    /**
     * Returns the content and prevents its use again.
     */
    fun getContentIfNotHandled(): T? {
        return if (hasBeenHandled) {
            null
        } else {
            hasBeenHandled = true
            content
        }
    }

    /**
     * Returns the content, even if it's already been handled.
     */
    fun peekContent(): T = content

    companion object {
        fun <T> create(
            loadingStatus: LoadingStatus,
            message: String? = null,
            payload: T? = null
        ): SingleLiveEvent<EventPayload<T>> {
            return SingleLiveEvent(
                EventPayload(
                    loadingStatus,
                    message,
                    payload
                )
            )
        }
    }
}

class EventPayload<out T>(
    val loadingStatus: LoadingStatus,
    val message: String? = null,
    val payload: T? = null
)

enum class LoadingStatus {
    SUCCESS,
    NODATA,
    LOADING,
    FAILED,
}
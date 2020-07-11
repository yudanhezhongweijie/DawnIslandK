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

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import com.laotoua.dawnislandk.data.local.entity.Comment

fun <T> lazyOnMainOnly(initializer: () -> T): Lazy<T> = lazy(LazyThreadSafetyMode.NONE, initializer)

/**
 * returns true if list has been modified
 */
fun <E> MutableList<E>.addOrSet(ind: Int, element: E): Boolean {
    return if (ind >= size) add(element)
    else set(ind, element) == element
}

fun List<Comment>?.equalsWithServerComments(targetList: List<Comment>?): Boolean {
    return if (this == null || targetList == null) false
    else if (this.size != targetList.size) false
    else {
        this.zip(targetList).all { (r1, r2) ->
            r1.equalsWithServerData(r2)
        }
    }
}

// combines local and remote data then emit
// when requesting remote data only, simply pass null as cache
fun <T> getCombinedLiveData(
    cache: LiveData<DataResource<T>>?,
    remote: LiveData<DataResource<T>>
): LiveData<DataResource<T>> {
    val result = MediatorLiveData<DataResource<T>>()
    result.value = DataResource.create()
    if (cache != null) {
        result.addSource(cache) {
            if (it.status != LoadingStatus.NO_DATA) {
                result.value = combineCacheAndRemoteData(result.value, it, false)
            }
        }
    }
    result.addSource(remote) {
        result.value = combineCacheAndRemoteData(result.value, it, true)
    }
    return result
}

private fun <T> combineCacheAndRemoteData(
    old: DataResource<T>?,
    new: DataResource<T>?,
    isRemoteData: Boolean
): DataResource<T>? {
    return if (new?.status == LoadingStatus.ERROR && old?.status != new.status && !isRemoteData) {
        old
    } else {
        new
    }
}
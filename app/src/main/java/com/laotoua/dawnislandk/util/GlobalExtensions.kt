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

import androidx.arch.core.util.Function
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.liveData
import com.laotoua.dawnislandk.data.local.entity.Comment
import timber.log.Timber

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


fun <T> getLocalDataResource(cache: LiveData<T>): LiveData<DataResource<T>> {
    return Transformations.map(cache) {
        Timber.d("Got ${if (it == null) "NO" else ""} data from database")
        val status: LoadingStatus =
            if (it == null) LoadingStatus.NO_DATA else LoadingStatus.SUCCESS
        DataResource.create(status, it)
    }
}

fun <T> getLocalListDataResource(cache: LiveData<List<T>>): LiveData<DataResource<List<T>>> {
    return Transformations.map(cache) {
        Timber.d("Got ${it.size} rows from database")
        val status: LoadingStatus =
            if (it.isNullOrEmpty()) LoadingStatus.NO_DATA else LoadingStatus.SUCCESS
        DataResource.create(status, it)
    }
}

fun <X, Y> getRemoteDataResource(
    response: DataResource<X>,
    conversion: Function<DataResource<X>, DataResource<Y>>
) = liveData<DataResource<Y>> {
    if (response.status == LoadingStatus.SUCCESS) {
        emit(conversion.apply(response))
    } else {
        emit(DataResource.create(response.status, null, response.message!!))
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
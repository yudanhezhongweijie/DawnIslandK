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

import android.app.Activity
import android.content.Intent
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Parcelable
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Transformations
import com.laotoua.dawnislandk.BuildConfig
import com.laotoua.dawnislandk.data.local.entity.Comment
import timber.log.Timber

fun <T> lazyOnMainOnly(initializer: () -> T): Lazy<T> = lazy(LazyThreadSafetyMode.NONE, initializer)

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

// data only in local cache, but remote acts as a message transmitter
fun <T> getLocalLiveDataAndRemoteResponse(
    cache: LiveData<DataResource<T>>,
    remote: LiveData<DataResource<T>>
): LiveData<DataResource<T>> {
    val result = MediatorLiveData<DataResource<T>>()
    result.value = DataResource.create()
    result.addSource(cache) {
        result.value = it
    }
    result.addSource(remote) {
        if (it.status == LoadingStatus.NO_DATA || it.status == LoadingStatus.ERROR) {
            result.value = it
        }
    }
    return result
}

fun openLinksWithOtherApps(uri: String, activity: Activity) {
    val activities: List<ResolveInfo> =
        activity.packageManager.queryIntentActivities(Intent(Intent.ACTION_VIEW, Uri.parse(uri)), 0)
    val packageNameToHide = BuildConfig.APPLICATION_ID
    val targetIntents: ArrayList<Intent> = ArrayList()
    for (currentInfo in activities) {
        val packageName: String = currentInfo.activityInfo.packageName
        if (packageNameToHide != packageName) {
            val targetIntent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
            targetIntent.setPackage(packageName)
            targetIntents.add(targetIntent)
        }
    }

    if (targetIntents.isNotEmpty()) {
        val chooserIntent: Intent = Intent.createChooser(targetIntents.removeAt(0), "请使用以下软件打开链接")
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, targetIntents.toArray(arrayOf<Parcelable>()))
        activity.startActivity(chooserIntent)
    } else {
        Toast.makeText(activity, "没有找到可以打开链接的软件，请和开发者联系", Toast.LENGTH_SHORT).show()
    }
}


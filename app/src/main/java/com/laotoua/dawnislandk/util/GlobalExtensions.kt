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
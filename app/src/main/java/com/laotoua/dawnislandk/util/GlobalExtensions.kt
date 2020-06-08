package com.laotoua.dawnislandk.util

import com.laotoua.dawnislandk.data.local.Reply
import com.laotoua.dawnislandk.data.local.Thread

fun <T> lazyOnMainOnly(initializer: () -> T): Lazy<T> = lazy(LazyThreadSafetyMode.NONE, initializer)

/**
 * returns true if list has been modified
 */
fun <E> MutableList<E>.addOrSet(ind: Int, element: E): Boolean {
    return if (ind >= size) add(element)
    else set(ind, element) == element
}

fun List<Reply>?.equalsExceptTimestamp(targetList: List<Reply>?): Boolean {
    return if (this == null || targetList == null) false
    else if (this.size != targetList.size) false
    else {
        this.zip(targetList).all { (r1, r2) ->
            r1.equalsExceptTimestamp(r2)
        }
    }
}

fun List<Thread>?.equalsExceptTimeStampAndReply(targetList: List<Thread>?): Boolean {
    return if (this == null || targetList == null) false
    else if (this.size != targetList.size) false
    else {
        this.zip(targetList).all { (t1, t2) ->
            t1.equalsWithServerData(t2)
        }
    }
}
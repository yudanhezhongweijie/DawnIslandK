package com.laotoua.dawnislandk.viewmodel


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
            return SingleLiveEvent(EventPayload<T>(loadingStatus, message, payload))
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
package com.laotoua.dawnislandk.util

fun <T> lazyOnMainOnly(initializer: () -> T): Lazy<T> = lazy(LazyThreadSafetyMode.NONE, initializer)
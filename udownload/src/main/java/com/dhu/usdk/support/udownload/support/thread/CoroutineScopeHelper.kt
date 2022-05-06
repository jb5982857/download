package com.dhu.usdk.support.udownload.support.thread

import com.dhu.usdk.support.udownload.utils.ULog
import kotlinx.coroutines.*

private val errorHandle by lazy {
    CoroutineExceptionHandler { _, error ->
        ULog.e("coroutine error $error")
    }
}

val coroutineScope by lazy { CoroutineScope(errorHandle) }
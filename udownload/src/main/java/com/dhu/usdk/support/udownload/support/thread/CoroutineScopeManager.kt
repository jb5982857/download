package com.dhu.usdk.support.udownload.support.thread

import com.dhu.usdk.support.udownload.utils.ULog
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope

private val errorHandle = CoroutineExceptionHandler { _, error ->
    ULog.e("coroutine error $error")
}

val COROUTINE_SCOPE = CoroutineScope(errorHandle)
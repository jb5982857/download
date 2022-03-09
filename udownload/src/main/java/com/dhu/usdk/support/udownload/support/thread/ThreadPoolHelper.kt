package com.dhu.usdk.support.udownload.support.thread

import com.dhu.usdk.support.udownload.modules.ConfigCenter
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory


val TASK_POOL: ExecutorService = Executors.newFixedThreadPool(ConfigCenter.TASK_COUNT)

val DOWNLOAD_POOL_THREAD_FACTORY = threadFactory("udownload_pool", false)

fun threadFactory(name: String?, daemon: Boolean): ThreadFactory {
    return ThreadFactory { runnable: Runnable? ->
        val result = Thread(runnable, name)
        result.isDaemon = daemon
        result
    }
}

val DOWNLOAD_POOL: ExecutorService = Executors.newFixedThreadPool(
    ConfigCenter.THREAD_COUNT,
    DOWNLOAD_POOL_THREAD_FACTORY
)
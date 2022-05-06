package com.dhu.usdk.support.udownload.support.thread

import com.dhu.usdk.support.udownload.common.ConfigCenter
import com.dhu.usdk.support.udownload.utils.ULog
import java.util.concurrent.*


val DOWNLOAD_POOL_TASK_THREAD_FACTORY = threadFactory("udownload_task_pool", false)
val DOWNLOAD_POOL_THREAD_FACTORY = threadFactory("udownload_item_pool", false)

val TASK_POOL: ExecutorService = ThreadPoolExecutor(
    ConfigCenter.TASK_COUNT, ConfigCenter.TASK_COUNT, 0, TimeUnit.SECONDS,
    SynchronousQueue(), DOWNLOAD_POOL_TASK_THREAD_FACTORY
)

fun threadFactory(name: String, daemon: Boolean): ThreadFactory {
    return ThreadFactory { runnable: Runnable? ->
        val result = Thread(runnable, name)
        result.isDaemon = daemon
        result
    }
}

val DOWNLOAD_POOL: ExecutorService = ThreadPoolExecutor(
    ConfigCenter.THREAD_COUNT, ConfigCenter.THREAD_COUNT,
    0, TimeUnit.SECONDS,
    LinkedBlockingQueue<Runnable>(), DOWNLOAD_POOL_THREAD_FACTORY
) { r, executor -> ULog.d("reject") }
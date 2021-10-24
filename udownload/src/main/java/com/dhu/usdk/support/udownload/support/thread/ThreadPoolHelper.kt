package com.dhu.usdk.support.udownload.support.thread

import com.dhu.usdk.support.udownload.modules.ConfigCenter
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


val DOWNLOAD_POOL: ExecutorService = Executors.newFixedThreadPool(ConfigCenter.THREAD_COUNT)
val TASK_POOL: ExecutorService = Executors.newFixedThreadPool(ConfigCenter.TASK_COUNT)
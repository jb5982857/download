package com.dhu.usdk.support.udownload.modules

import com.dhu.usdk.support.udownload.support.io.AbIoManager
import com.dhu.usdk.support.udownload.support.io.OkioManager
import com.dhu.usdk.support.udownload.support.network.IHttpDownload
import com.dhu.usdk.support.udownload.support.network.OkhttpFactory

object ConfigCenter {
    //总下载启动5个线程
    const val THREAD_COUNT = 5

    //可以同时处理的 TASK 个数
    const val TASK_COUNT = 3

    val HTTP_IMPL: IHttpDownload = OkhttpFactory()

    val IO_IMPL: AbIoManager = OkioManager()
}
package com.dhu.usdk.support.udownload.modules

import com.dhu.usdk.support.udownload.Item
import com.dhu.usdk.support.udownload.modules.download.UInternalTask
import com.dhu.usdk.support.udownload.support.io.AbIoManager
import com.dhu.usdk.support.udownload.support.io.RandomAccessFileManager
import com.dhu.usdk.support.udownload.support.network.IHttpDownload
import com.dhu.usdk.support.udownload.support.network.OkhttpFactory
import com.dhu.usdk.support.udownload.support.network.UrlConnectionFactory

object ConfigCenter {
    //总下载启动5个线程
    const val THREAD_COUNT = 9

    //可以同时处理的 TASK 个数
    const val TASK_COUNT = 3

    val HTTP: IHttpDownload get() = UrlConnectionFactory()

    fun getIO(item: Item): AbIoManager = RandomAccessFileManager(item)
}
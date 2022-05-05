package com.dhu.usdk.support.udownload.modules.download

import com.dhu.usdk.support.udownload.Item
import com.dhu.usdk.support.udownload.UDownloadService
import com.dhu.usdk.support.udownload.modules.ConfigCenter
import com.dhu.usdk.support.udownload.support.thread.DOWNLOAD_POOL
import com.dhu.usdk.support.udownload.utils.ULog
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

class DownloadItemManager {
    private val lock = Object()
    private val itemQueue = Vector<VectorData>()

    fun init() {
        Thread {
            for (index in 0 until ConfigCenter.THREAD_COUNT) {
                startNextRunnable()
            }
        }.start()
    }

    @Volatile
    var lastChoiceDataIndex = 0

    /**
     * 让所有的 Task 都有概率来下载
     */
    private fun next(): ItemTaskData {
        synchronized(lock) {
            while (itemQueue.isEmpty()) {
                lock.wait()
            }

            if (lastChoiceDataIndex >= itemQueue.size - 1) {
                lastChoiceDataIndex = 0
            } else {
                lastChoiceDataIndex++
            }

            ULog.d("index $lastChoiceDataIndex , queue size ${itemQueue.size}")
            val vectorData = itemQueue[lastChoiceDataIndex]
            val item = vectorData.queue.poll()!!
            if (vectorData.queue.isEmpty()) {
                itemQueue.remove(vectorData)
            }
            return item
        }
    }

    fun set(uInternalTask: UInternalTask, item: ItemTaskData) {
        synchronized(lock) {
            val vectorData = itemQueue.find { it.uInternalTask == uInternalTask }
            if (vectorData != null) {
                vectorData.queue.add(item)
            } else {
                val newVectorData = VectorData(ConcurrentLinkedQueue<ItemTaskData>().apply {
                    this.add(item)
                }, uInternalTask)
                itemQueue.add(newVectorData)
            }
            lock.notifyAll()
        }
    }

    private fun startNextRunnable() {
        val itemData = next()
        val item = itemData.item
        val callback = itemData.downloadingListener
        DOWNLOAD_POOL.submit {
            callback(ItemDownloadState.START_DOWNLOAD)
            ConfigCenter.HTTP.download(
                item.url,
                item.ioManager.getTempFileSize()
            ).apply {
                if (this.inputStream == null) {
                    ULog.i("$item 网络下载失败")
                    if (callback(ItemDownloadState.RESULT_FAILED)) {
                        return@submit
                    }
                } else {
                    if (callback(ItemDownloadState.HTTP_CONNECT_SUCCESS.let {
                            it.value = this.isSupportRange
                            return@let it
                        })) {
                        return@submit
                    }
                    if (item.ioManager.writeFile(
                            this.inputStream,
                            this.isSupportRange,
                            item.md5
                        )
                    ) {
                        ULog.i(
                            "$item 下载成功"
                        )
                        if (callback(ItemDownloadState.RESULT_SUCCESS)) {
                            return@submit
                        }
                    } else {
                        ULog.i("$item 文件读写失败")
                        if (callback(ItemDownloadState.RESULT_FAILED)) {
                            return@submit
                        }
                    }
                }
                if (callback(ItemDownloadState.FINISH)) {
                    return@submit
                }
                startNextRunnable()
            }

        }
    }

    fun releaseData() {
        itemQueue.clear()
    }

    data class VectorData(
        val queue: ConcurrentLinkedQueue<ItemTaskData>,
        val uInternalTask: UInternalTask
    )

    data class ItemTaskData(val item: Item, val downloadingListener: (ItemDownloadState) -> Boolean)

    enum class ItemDownloadState(var value: Any? = null) {
        START_DOWNLOAD, HTTP_CONNECT_SUCCESS, RESULT_FAILED, RESULT_SUCCESS, FINISH
    }
}

package com.dhu.usdk.support.udownload.modules.download

import com.dhu.usdk.support.udownload.Item
import com.dhu.usdk.support.udownload.UDownloadService
import com.dhu.usdk.support.udownload.modules.ConfigCenter
import com.dhu.usdk.support.udownload.support.thread.DOWNLOAD_POOL
import com.dhu.usdk.support.udownload.utils.ULog
import java.util.concurrent.ConcurrentLinkedQueue

class DownloadItemManager {
    private val lock = Object()
    private val itemQueue = ConcurrentLinkedQueue<ItemTaskData>()

    fun init() {
        Thread {
            for (index in 0 until ConfigCenter.THREAD_COUNT) {
                startNextRunnable()
            }
        }.start()
    }

    private fun next(): ItemTaskData {
        synchronized(lock) {
            while (itemQueue.isEmpty()) {
                lock.wait()
            }
            return itemQueue.poll()!!
        }
    }

    fun set(item: ItemTaskData) {
        synchronized(lock) {
            itemQueue.add(item)
            lock.notifyAll()
        }
    }

    private fun startNextRunnable() {
        ULog.d("startNextRunnable ${UDownloadService.isAlive}")
        if (!UDownloadService.isAlive) {
            return
        }
        val itemData = next()
        val item = itemData.item
        val callback = itemData.downloadingListener
        DOWNLOAD_POOL.submit {
            callback(ItemDownloadState.START_DOWNLOAD)
            ConfigCenter.HTTP.download(
                item.url,
                item.ioManager.getTempFileSize(item.path)
            ).apply {
                if (this == null) {
                    ULog.i("$item 11111下载失败 null")
                    callback(ItemDownloadState.RESULT_FAILED)
                } else {
                    callback(ItemDownloadState.HTTP_CONNECT_SUCCESS)
                    if (item.ioManager.writeFile(item.path, this, item.md5)
                    ) {
                        ULog.i(
                            "$item 11111下载成功"
                        )
                        callback(ItemDownloadState.RESULT_SUCCESS)
                    } else {
                        ULog.i("$item 11111下载失败")
                        callback(ItemDownloadState.RESULT_FAILED)
                    }
                }
                callback(ItemDownloadState.FINISH)
            }

            startNextRunnable()
        }
    }

    fun releaseData() {
        itemQueue.clear()
    }

    data class ItemTaskData(val item: Item, val downloadingListener: (ItemDownloadState) -> Unit)

    enum class ItemDownloadState {
        START_DOWNLOAD, HTTP_CONNECT_SUCCESS, RESULT_FAILED, RESULT_SUCCESS, FINISH
    }
}

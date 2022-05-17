package com.dhu.usdk.support.udownload.modules.download

import com.dhu.usdk.support.udownload.Item
import com.dhu.usdk.support.udownload.ResultState
import com.dhu.usdk.support.udownload.common.ConfigCenter
import com.dhu.usdk.support.udownload.common.StateCode
import com.dhu.usdk.support.udownload.support.thread.DOWNLOAD_POOL
import com.dhu.usdk.support.udownload.utils.ULog
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * 单个文件的下载实际操作管理类
 * 这里和外面的 Task 管理器一样，维护类一个生产者消费者模式，当有item进来的时候，在线程池充裕的情况下，进行下载u
 * 采用生产者消费者的原因是因为，不能一次性的扔给线程池，不然不方便操作停止
 */
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

    private fun downloadOnce(itemData: ItemTaskData) {
        val item = itemData.item
        val callback = itemData.downloadingListener
        itemData.item.ioManager.isWriteFinish = false
        DOWNLOAD_POOL.submit {
            callback(ItemDownloadState.START_DOWNLOAD)
            //如果调用停止了，就直接不用下载，返回失败
            if (item.task.isStop()) {
                callback(ItemDownloadState.RESULT_FAILED.let {
                    it.value = ResultState(StateCode.CANCEL)
                    return@let it
                })
                callback(ItemDownloadState.FINISH)
                startNextRunnable()
                return@submit
            }
            //如果文件本身就存在，则跳过下载
            val checkResult = item.ioManager.checkFileIfExist()
            if (checkResult.exist) {
                callback(ItemDownloadState.HTTP_CONNECT_SUCCESS)
                callback(ItemDownloadState.RESULT_SUCCESS.let {
                    it.value = ResultState()
                    return@let it
                })
                callback(ItemDownloadState.FINISH)
                startNextRunnable()
                return@submit
            }
            //开始下载
            ConfigCenter.HTTP.download(
                item.url,
                item.ioManager.getTempFileSize()
            ).apply {
                if (this.inputStream == null) {
                    var failedResultState = this.resultState
                    if (item.task.isStop()) {
                        failedResultState = ResultState(StateCode.CANCEL)
                    } else if (item.needRetry()) {
                        ULog.w("$item  网络问题重试 ${item.task.networkState}")
                        downloadOnce(itemData)
                        return@submit
                    }
                    ULog.d("$item 网络下载失败 $failedResultState")
                    callback(ItemDownloadState.RESULT_FAILED.let {
                        it.value = failedResultState
                        return@let it
                    })
                } else {
                    callback(ItemDownloadState.HTTP_CONNECT_SUCCESS.let {
                        it.value = this.isSupportRange
                        return@let it
                    })
                    val writeResult = item.ioManager.writeFile(
                        this.inputStream,
                        this.isSupportRange,
                        item.md5
                    )
                    if (writeResult.isSuccessful()) {
                        ULog.i(
                            "$item 下载成功"
                        )
                        callback(ItemDownloadState.RESULT_SUCCESS.let {
                            it.value = writeResult
                            return@let it
                        })
                    } else {
                        if (!writeResult.isCancel() && item.needRetry()) {
                            ULog.w("$item  重试")
                            downloadOnce(itemData)
                            return@submit
                        }
                        ULog.i("$item 文件读写失败")
                        callback(ItemDownloadState.RESULT_FAILED.let {
                            it.value = writeResult
                            return@let it
                        })
                    }
                }
                callback(ItemDownloadState.FINISH)
                startNextRunnable()
            }

        }
    }

    private fun startNextRunnable() {
        downloadOnce(next())
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

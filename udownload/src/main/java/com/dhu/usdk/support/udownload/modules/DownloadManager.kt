package com.dhu.usdk.support.udownload.modules

import android.app.Notification
import android.content.Context
import com.dhu.usdk.support.udownload.State
import com.dhu.usdk.support.udownload.UDownloadService
import com.dhu.usdk.support.udownload.UTask
import com.dhu.usdk.support.udownload.support.thread.DOWNLOAD_POOL
import com.dhu.usdk.support.udownload.support.thread.coroutineScope
import com.dhu.usdk.support.udownload.utils.ULog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentLinkedQueue

class DownloadManager private constructor() {
    val downloadTasks = ConcurrentLinkedQueue<UInternalTask>()

    companion object {
        val instance = Holder.hold
    }

    object Holder {
        val hold = DownloadManager()
    }

    @Volatile
    var downLoadCount = 0
    fun add(service: UDownloadService, task: UTask) {
        val uInternalTask = UInternalTask(task)
        downloadTasks.add(uInternalTask)
        if (task.isShowNotification) {
            uInternalTask.notification = NotificationModule.createNotification(service)
            uInternalTask.notificationId = NotificationModule.getNotificationId()
            service.startForeground(
                uInternalTask.notificationId,
                uInternalTask.notification
            )
        }

        coroutineScope.launch {
            withContext(Dispatchers.IO) {
                var successLen = 0L
                var totalLen = 0L
                task.downloadQueue.forEach {
                    totalLen += it.size
                    //有标记下载成功的，直接放置到成功队列
                    if (it.state == State.SUCCESS) {
                        task.successTasks.addSuccessItem(it)
                        successLen += it.size
                        return@forEach
                    }
                    //检查本地是否存在同名文件，并且 md5 是否一致
                    val fileCheckData = ConfigCenter.IO.checkFileIfExist(it.path, it.md5)
                    successLen += fileCheckData.len
                    if (fileCheckData.exist) {
                        //本地已经存在，且 md5 匹配
                        task.successTasks.addSuccessItem(it)
                        ULog.i("$it 下载成功")
                        return@forEach
                    }
                }

                if (finishTaskIfNeeded(service, uInternalTask)) {
                    return@withContext
                }

                uInternalTask.scheduleModule.init(
                    successLen,
                    totalLen,
                    uInternalTask.notificationId
                )

                task.pendingQueue.addAll(task.downloadQueue)
                task.successTasks.forEach {
                    task.pendingQueue.remove(it)
                }

                uInternalTask.scheduleModule.start(service.applicationContext)
                //开始下载
                task.pendingQueue.forEach {
                    //下载线程池
                    DOWNLOAD_POOL.submit {
                        ULog.d("$it 11111正在下载 , 当前下载数 ${++downLoadCount}")
                        ConfigCenter.HTTP.download(
                            it.url,
                            ConfigCenter.IO.getTempFileSize(it.path)
                        )
                            .apply {
                                if (this == null) {
                                    downLoadCount--
                                    ULog.i("$it 11111下载失败 null")
                                    task.failedTasks.add(it)
                                } else {
                                    val io = ConfigCenter.IO
                                    uInternalTask.scheduleModule.add(io)
                                    if (io.writeFile(it.path, this, it.md5)
                                    ) {
                                        downLoadCount--
                                        ULog.i(
                                            "$it 11111下载成功"
                                        )
                                        task.successTasks.addSuccessItem(
                                            it
                                        )
                                    } else {
                                        downLoadCount--
                                        ULog.i("$it 11111下载失败")
                                        task.failedTasks.add(it)
                                    }
                                }
                                ULog.i("有结束了的，总数 ${task.downloadQueue.size}, 成功数 ${task.successTasks.size}, 失败数 ${task.failedTasks.size}")
                                finishTaskIfNeeded(service, uInternalTask)
                            }
                    }
                }

            }
        }
    }

    fun restart(context: Context, task: UTask) {

    }

    fun pause(context: Context, task: UTask) {

    }

    fun stop(context: Context, task: UTask) {

    }

    private fun finishTaskIfNeeded(
        service: UDownloadService,
        uInternalTask: UInternalTask
    ): Boolean {
        if (uInternalTask.uTask.downloadQueue.size == uInternalTask.uTask.successTasks.size + uInternalTask.uTask.failedTasks.size) {
            uInternalTask.scheduleModule.stop()
            //下载完成
            if (uInternalTask.uTask.failedTasks.size == 0) {
                NotificationModule.showSuccessNotification(
                    service.applicationContext,
                    uInternalTask.notificationId
                )
            } else {
                NotificationModule.showFailedNotification(
                    service.applicationContext,
                    uInternalTask.notificationId
                )
            }
            service.stopSelf()
            return true
        }
        return false
    }
}

data class UInternalTask(
    val uTask: UTask,
    var notification: Notification? = null,
    var notificationId: Int = NotificationModule.DEFAULT_ID,
    val scheduleModule: DownloadScheduleModule = DownloadScheduleModule()
)
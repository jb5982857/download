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
                task.downloadQueue.forEach {
                    //有标记下载成功的，直接放置到成功队列
                    if (it.state == State.SUCCESS) {
                        task.successTasks.addSuccessItem(it)
                        return@forEach
                    }
                    //检查本地是否存在同名文件，并且 md5 是否一致
                    if (ConfigCenter.IO.checkFileIfExist(it.path, it.md5)) {
                        //本地已经存在，且 md5 匹配
                        task.successTasks.addSuccessItem(it)
                        ULog.i("$it 下载成功")
                        return@forEach
                    }
                }

                task.pendingQueue.addAll(task.downloadQueue)
                task.successTasks.forEach {
                    task.pendingQueue.remove(it)
                }

                uInternalTask.speedModule.start()
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
                                    uInternalTask.speedModule.add(io)
                                    if (io.writeFile(it.path, this)
                                    ) {
                                        downLoadCount--
                                        ULog.i(
                                            "$it 11111下载成功"
                                        )
                                        task.successTasks.addSuccessItem(it)
                                    } else {
                                        downLoadCount--
                                        ULog.i("$it 11111下载失败")
                                        task.failedTasks.add(it)
                                    }
                                }
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
}

data class UInternalTask(
    val uTask: UTask,
    var notification: Notification? = null,
    var notificationId: Int = NotificationModule.DEFAULT_ID,
    val speedModule: DownloadSpeedModule = DownloadSpeedModule()
)
package com.dhu.usdk.support.udownload.modules

import android.app.Notification
import android.content.Context
import com.dhu.usdk.support.udownload.State
import com.dhu.usdk.support.udownload.UDownloadService
import com.dhu.usdk.support.udownload.UTask
import com.dhu.usdk.support.udownload.modules.ConfigCenter.TASK_COUNT
import com.dhu.usdk.support.udownload.modules.download.DownLoadTaskManager
import com.dhu.usdk.support.udownload.support.thread.DOWNLOAD_POOL
import com.dhu.usdk.support.udownload.support.thread.TASK_POOL
import com.dhu.usdk.support.udownload.support.thread.coroutineScope
import com.dhu.usdk.support.udownload.utils.ULog
import com.dhu.usdk.support.udownload.utils.application
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentLinkedQueue

class DownloadManager private constructor() {
    private val taskManager = DownLoadTaskManager()

    companion object {
        val instance = Holder.hold
    }

    object Holder {
        val hold = DownloadManager()
    }

    init {
        for (i in 0 until TASK_COUNT) {
            launchNextTasks()
        }
    }

    private fun launchNextTasks() {
        TASK_POOL.submit {
            startTask()
        }
    }

    @Volatile
    var downLoadCount = 0
    fun add(service: UDownloadService, task: UTask) {
        val uInternalTask = UInternalTask(task)
        if (task.isShowNotification) {
            uInternalTask.notification = NotificationModule.createNotification(service)
            uInternalTask.notificationId = NotificationModule.getNotificationId()
            service.startForeground(uInternalTask.notificationId, uInternalTask.notification)
        }
        taskManager.addTask(uInternalTask)
    }

    fun restart(context: Context, task: UTask) {

    }

    fun pause(context: Context, task: UTask) {

    }

    fun stop(context: Context, task: UTask) {

    }

    private fun startTask() {
        val task = taskManager.getTask()
        GlobalScope.launch {
            withContext(Dispatchers.IO) {
                var successLen = 0L
                var totalLen = 0L
                task.uTask.downloadQueue.forEach {
                    totalLen += it.size
                    //有标记下载成功的，直接放置到成功队列
                    if (it.state == State.SUCCESS) {
                        task.uTask.successTasks.addSuccessItem(it)
                        successLen += it.size
                        return@forEach
                    }
                    //检查本地是否存在同名文件，并且 md5 是否一致
                    val fileCheckData = ConfigCenter.IO.checkFileIfExist(it.path, it.md5)
                    successLen += fileCheckData.len
                    if (fileCheckData.exist) {
                        //本地已经存在，且 md5 匹配
                        task.uTask.successTasks.addSuccessItem(it)
                        ULog.i("$it 下载成功")
                        return@forEach
                    }
                }

                if (finishTaskIfNeeded(task)) {
                    return@withContext
                }

                task.scheduleModule.init(
                    successLen,
                    totalLen,
                    task.notificationId
                )

                task.uTask.pendingQueue.addAll(task.uTask.downloadQueue)
                task.uTask.successTasks.forEach {
                    task.uTask.pendingQueue.remove(it)
                }

                task.scheduleModule.start(application!!)
                //开始下载
                task.uTask.pendingQueue.forEach {
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
                                    task.uTask.failedTasks.add(it)
                                } else {
                                    val io = ConfigCenter.IO
                                    task.scheduleModule.add(io)
                                    if (io.writeFile(it.path, this, it.md5)
                                    ) {
                                        downLoadCount--
                                        ULog.i(
                                            "$it 11111下载成功"
                                        )
                                        task.uTask.successTasks.addSuccessItem(
                                            it
                                        )
                                    } else {
                                        downLoadCount--
                                        ULog.i("$it 11111下载失败")
                                        task.uTask.failedTasks.add(it)
                                    }
                                }
                                ULog.i("有结束了的，总数 ${task.uTask.downloadQueue.size}, 成功数 ${task.uTask.successTasks.size}, 失败数 ${task.uTask.failedTasks.size}")
                                finishTaskIfNeeded(task)
                            }
                    }
                }

            }
        }
    }

    private fun finishTaskIfNeeded(
        uInternalTask: UInternalTask
    ): Boolean {
        if (uInternalTask.uTask.downloadQueue.size == uInternalTask.uTask.successTasks.size + uInternalTask.uTask.failedTasks.size) {
            uInternalTask.scheduleModule.stop()
            //下载完成
            if (uInternalTask.uTask.failedTasks.size == 0) {
                NotificationModule.showSuccessNotification(
                    application!!,
                    uInternalTask.notificationId
                )
            } else {
                NotificationModule.showFailedNotification(
                    application!!,
                    uInternalTask.notificationId
                )
            }
            launchNextTasks()
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
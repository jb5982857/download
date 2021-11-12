package com.dhu.usdk.support.udownload.modules.download

import android.app.Notification
import android.content.Context
import com.dhu.usdk.support.udownload.Item
import com.dhu.usdk.support.udownload.State
import com.dhu.usdk.support.udownload.UDownloadService
import com.dhu.usdk.support.udownload.UTask
import com.dhu.usdk.support.udownload.modules.ConfigCenter
import com.dhu.usdk.support.udownload.modules.ConfigCenter.TASK_COUNT
import com.dhu.usdk.support.udownload.modules.ConfigCenter.getIO
import com.dhu.usdk.support.udownload.modules.DownloadScheduleModule
import com.dhu.usdk.support.udownload.modules.NotificationModule
import com.dhu.usdk.support.udownload.support.io.AbIoManager
import com.dhu.usdk.support.udownload.support.thread.DOWNLOAD_POOL_THREAD_FACTORY
import com.dhu.usdk.support.udownload.support.thread.TASK_POOL
import com.dhu.usdk.support.udownload.utils.ULog
import com.dhu.usdk.support.udownload.utils.application
import com.dhu.usdk.support.udownload.utils.mainHandler
import com.dhu.usdk.support.udownload.utils.switchUiThreadIfNeeded
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.collections.ArrayList

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
    fun add(uInternalTask: UInternalTask) {
        taskManager.addTask(uInternalTask)
    }

    fun restart(context: Context, task: UTask) {

    }

    fun pause(context: Context, task: UTask) {

    }

    fun stop(context: Context, task: UTask) {

    }

    private fun startTask() {
        val task = taskManager.next()
        ULog.d("task $task 开始了")
        switchUiThreadIfNeeded {
            task.uTask.downloadStateChangeListener(State.DOWNLOADING)
        }

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
                    val fileCheckData = it.ioManager.checkFileIfExist(
                        it.path,
                        it.md5
                    )
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
                    task.notificationId, task.uTask
                )

                task.uTask.pendingQueue.addAll(task.uTask.downloadQueue)
                task.uTask.successTasks.forEach {
                    task.uTask.pendingQueue.remove(it)
                }

                task.scheduleModule.start(application!!)
                //开始下载
                task.uTask.pendingQueue.forEach {
                    startDownloadItem(task, it)
                }

            }
        }
    }

    private fun startDownloadItem(task: UInternalTask, it: Item) {
        //下载线程池
        task.downloadPool.submit {
            ULog.d("$it 11111正在下载 , 当前下载数 ${++downLoadCount}")
            task.uTask.lockItemTaskIfNeeded()
            ConfigCenter.HTTP.download(
                it.url,
                it.ioManager.getTempFileSize(it.path)
            )
                .apply {
                    task.uTask.lockItemTaskIfNeeded()
                    if (this == null) {
                        downLoadCount--
                        ULog.i("$it 11111下载失败 null")
                        task.uTask.failedTasks.add(it)
                    } else {
                        task.scheduleModule.add(it.ioManager)
                        if (it.ioManager.writeFile(it.path, this, it.md5)
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

    @Synchronized
    private fun finishTaskIfNeeded(
        uInternalTask: UInternalTask
    ): Boolean {
        if (uInternalTask.uTask.downloadQueue.size == uInternalTask.uTask.successTasks.size + uInternalTask.uTask.failedTasks.size) {
            if (!retryTaskIfNeeded(uInternalTask)) {
                uInternalTask.scheduleModule.stop()
                switchUiThreadIfNeeded {
                    uInternalTask.uTask.downloadFinishListener(
                        uInternalTask.uTask.downloadQueue,
                        uInternalTask.uTask.successTasks, uInternalTask.uTask.failedTasks
                    )
                }
                switchUiThreadIfNeeded {
                    //下载完成
                    uInternalTask.downloadFinish(uInternalTask)
                }
                launchNextTasks()
                switchUiThreadIfNeeded {
                    uInternalTask.uTask.downloadStateChangeListener(State.ON_FINISH)
                }
                return true
            } else {
                return false
            }
        }
        return false
    }

    private fun retryTaskIfNeeded(uInternalTask: UInternalTask): Boolean {
        if (!uInternalTask.retry && !uInternalTask.uTask.failedTasks.isEmpty()) {
            uInternalTask.retry = true
            val tempFailedTasks = ArrayList<Item>(uInternalTask.uTask.failedTasks)
            uInternalTask.uTask.failedTasks.clear()
            tempFailedTasks.forEach {
                ULog.d("retry $it")
                startDownloadItem(uInternalTask, it)
            }

            return true
        }

        return false
    }
}

data class UInternalTask(
    val uTask: UTask,
    var notification: Notification? = null,
    val downloadFinish: (UInternalTask) -> Unit = {},
    var notificationId: Int? = null,
    val scheduleModule: DownloadScheduleModule = DownloadScheduleModule(),
    val downloadPool: ExecutorService = Executors.newFixedThreadPool(
        ConfigCenter.THREAD_COUNT,
        DOWNLOAD_POOL_THREAD_FACTORY
    ),
    var retry: Boolean = false
)
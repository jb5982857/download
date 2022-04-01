package com.dhu.usdk.support.udownload.modules.download

import android.app.Notification
import android.content.Context
import com.dhu.usdk.support.udownload.Item
import com.dhu.usdk.support.udownload.State
import com.dhu.usdk.support.udownload.UTask
import com.dhu.usdk.support.udownload.modules.ConfigCenter.TASK_COUNT
import com.dhu.usdk.support.udownload.modules.DownloadScheduleModule
import com.dhu.usdk.support.udownload.support.thread.TASK_POOL
import com.dhu.usdk.support.udownload.utils.ULog
import com.dhu.usdk.support.udownload.utils.application
import com.dhu.usdk.support.udownload.utils.switchUiThreadIfNeeded
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.collections.ArrayList

class DownloadManager private constructor() {
    private val taskManager = DownLoadTaskManager()
    private val itemManager by lazy {
        DownloadItemManager().apply {
            init()
        }
    }

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

    fun add(uInternalTask: UInternalTask) {
        DownloadTaskLifecycle.instance.add(uInternalTask)
        taskManager.addTask(uInternalTask)
    }

    fun restart(context: Context, task: UTask) {

    }

    fun pause(context: Context, task: UTask) {
    }

    fun stop(context: Context, task: UTask) {
        release()
    }

    fun releaseAll(context: Context) {
        release()
    }

    private fun release() {
        ULog.d("release all")
        itemManager.releaseData()
        taskManager.releaseData()
    }

    private fun initPoolIfNeeded() {
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
                    task.uTask.lockItemTaskIfNeeded()
                    if (task.uTask.isFinished()) {
                        return@withContext
                    }
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

                if (task.uTask.isFinished()) {
                    return@withContext
                }
                task.scheduleModule.start(application!!)
                //开始下载
                task.uTask.pendingQueue.forEach {
                    if (task.uTask.isFinished()) {
                        return@withContext
                    }
                    startDownloadItem(task, it)
                }

            }
        }
    }

    private fun startDownloadItem(task: UInternalTask, item: Item?) {
        item ?: return
        itemManager.set(task, DownloadItemManager.ItemTaskData(item) {
            when (it) {
                DownloadItemManager.ItemDownloadState.START_DOWNLOAD -> {
                    task.uTask.lockItemTaskIfNeeded()
                }
                DownloadItemManager.ItemDownloadState.HTTP_CONNECT_SUCCESS -> {
                    task.uTask.lockItemTaskIfNeeded()
                    task.scheduleModule.add(item.ioManager)
                }

                DownloadItemManager.ItemDownloadState.RESULT_SUCCESS -> {
                    task.uTask.successTasks.addSuccessItem(item)
                }

                DownloadItemManager.ItemDownloadState.RESULT_FAILED -> {
                    task.uTask.failedTasks.add(item)
                }

                DownloadItemManager.ItemDownloadState.FINISH -> {
                    ULog.i("item 下载，${item.url},有结束了的，总数 ${task.uTask.downloadQueue.size}, 成功数 ${task.uTask.successTasks.size}, 失败数 ${task.uTask.failedTasks.size}")
                    finishTaskIfNeeded(task)
                }
            }
            return@ItemTaskData task.uTask.isFinished()

        })
    }

    /**
     * 判断是否下载完成了
     */
    @Synchronized
    private fun finishTaskIfNeeded(
        uInternalTask: UInternalTask
    ): Boolean {
        val taskState = uInternalTask.isFinished()
        if (taskState == UInternalTask.UInternalTaskState.DOWNLOADING) {
            return false
        }

        if (taskState == UInternalTask.UInternalTaskState.SUCCESS || taskState == UInternalTask.UInternalTaskState.FAILED) {
            launchNextTasks()
            return true
        }

        if (taskState == UInternalTask.UInternalTaskState.NEED_RETRY) {
            retryTaskIfNeeded(uInternalTask)
        }
        return false
    }

    /**
     * 重试失败的下载
     */
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
    val downloadFinish: (UInternalTask, Boolean) -> Unit = { _: UInternalTask, _: Boolean -> },
    var notificationId: Int? = null,
    val scheduleModule: DownloadScheduleModule = DownloadScheduleModule(),
    var retry: Boolean = false
) {
    fun isFinished(): UInternalTaskState {
        return if (uTask.downloadQueue.size == uTask.successTasks.size + uTask.failedTasks.size) {
            if (uTask.failedTasks.size != 0) {
                if (!retry) {
                    UInternalTaskState.NEED_RETRY
                } else {
                    scheduleModule.stop()
                    downloadFinish(this, false)
                    UInternalTaskState.FAILED
                }
            } else {
                scheduleModule.stop()
                switchUiThreadIfNeeded {
                    downloadFinish(this, true)
                    uTask.downloadFinishListener(
                        uTask.downloadQueue,
                        uTask.successTasks, uTask.failedTasks
                    )
                    uTask.downloadStateChangeListener(State.ON_FINISH)

                }
                UInternalTaskState.SUCCESS
            }
        } else {
            UInternalTaskState.DOWNLOADING
        }
    }

    enum class UInternalTaskState {
        DOWNLOADING, SUCCESS, FAILED, NEED_RETRY
    }
}
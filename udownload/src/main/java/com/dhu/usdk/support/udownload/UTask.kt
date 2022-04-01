package com.dhu.usdk.support.udownload

import android.app.Activity
import android.content.Context
import com.dhu.usdk.support.udownload.modules.ConfigCenter
import com.dhu.usdk.support.udownload.modules.download.DownloadManager
import com.dhu.usdk.support.udownload.modules.download.UInternalTask
import com.dhu.usdk.support.udownload.support.io.AbIoManager
import com.dhu.usdk.support.udownload.support.queue.SuccessTasks
import com.dhu.usdk.support.udownload.utils.ULog
import com.dhu.usdk.support.udownload.utils.switchUiThreadIfNeeded
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.collections.ArrayList

@Volatile
var taskIndex = 0

class UTask(
    val isShowNotification: Boolean = true,
    val name: String = taskIndex++.toString()
) {
    private var isStart = false
    val downloadQueue = ConcurrentLinkedQueue<Item>()
    val pendingQueue = ConcurrentLinkedQueue<Item>()
    val successTasks = SuccessTasks(this)
    val failedTasks = ConcurrentLinkedQueue<Item>()
    var state: State? = null

    private val itemLock = Object()

    /**
     * 下载进度
     * params 1 -> 总大小，单位 byte
     * params 2 -> 已下载完成的大小，单位 byte
     * params 3 -> 当前速度，根据实际情况转换 kb/s , Mb/s , b/s
     */
    var downloadProgressListener = { _: Long, _: Long, _: String -> }

    /**
     * 有文件下载完成，由这里同步
     */
    var downloadItemFinishListener: (item: Item) -> Unit = {}

    /**
     * 下载完成
     * params 1 -> 总下载 list
     * params 2 -> 下载成功 list
     * params 3 -> 下载失败 list
     */
    var downloadFinishListener =
        { _: Collection<Item>, _: Collection<Item>, _: Collection<Item> -> }

    /**
     * 下载状态改变
     */
    var downloadStateChangeListener = { _: State -> }

    fun add(item: Item): UTask {
        if (isStart) {
            "the task $name is started , add failed".apply {
                ULog.e(this)
            }
            return this
        }
        val existItem = downloadQueue.find { it.path == item.path }
        if (existItem != null) {
            if (existItem.url == item.url) {
                "item $item has the same url and path as $existItem".apply {
                    ULog.e(this)
                }
                existItem.duplicateItem.add(item)
            } else {
                "item $item has the same path as $existItem,but the url is not the same ,it will error occur".apply {
                    ULog.e(this)
                }
            }
        }
        item.md5 = item.md5.toLowerCase(Locale.ROOT)
        item.task = this
        downloadQueue.add(item)
        return this
    }

    fun start(activity: Activity) {
        if (isStart) {
            "the task $name is started".apply {
                ULog.w(this)
            }
            return
        }
        ULog.i("开始下载，文件数 ${downloadQueue.size}")
        switchUiThreadIfNeeded {
            downloadStateChangeListener(State.READY)
        }
        if (isShowNotification) {
            UDownloadService.add(activity, this)
        } else {
            DownloadManager.instance.add(UInternalTask(this, downloadFinish = { _, _ ->

            }))
        }
        state = State.DOWNLOADING
        isStart = true
    }

    fun restart() {
        state = State.DOWNLOADING
        synchronized(itemLock) {
            itemLock.notifyAll()
        }
        downloadStateChangeListener(State.DOWNLOADING)
    }

    fun pause() {
        state = State.ON_PAUSE
        downloadStateChangeListener(State.ON_PAUSE)
    }

    fun pauseOrResume() {
        if (state == State.ON_PAUSE) {
            restart()
        } else {
            pause()
        }
    }

    fun stop(context: Context) {
        if (isShowNotification) {

        }
    }

    fun lockItemTaskIfNeeded() {
        if (state == State.ON_PAUSE) {
            synchronized(itemLock) {
                if (state == State.ON_PAUSE) {
                    itemLock.wait()
                }
            }
        }
    }

    fun isFinished(): Boolean {
        return state == State.ON_STOP || state == State.ON_FINISH || state == State.SUCCESS || state == State.FAILED
    }

    fun isDownloading(): Boolean {
        return state == State.DOWNLOADING
    }

}

enum class State(value: Int) {
    READY(1), DOWNLOADING(2), ON_PAUSE(3), ON_STOP(4), CELLULAR_PAUSE(5), ON_FINISH(6), SUCCESS(
        0
    ),
    FAILED(
        -1
    )
}

/**
 * url -> 下载的url
 * path -> 目标路径，下载过程中会生成 path.temp 文件，下载完成后 rename 到 path
 * md5 -> 文件的 md5 值
 * size -> 文件大小，单位 byte
 * userData -> 用户的信息，C# 地址
 * tag -> 透传的数据
 */
data class Item(
    val url: String,
    val path: String,
    var md5: String,
    val size: Long,
    val userData: Int,
    val tag: String
) {
    //重复的 item ，必须是 url 和 path 相同
    val duplicateItem = ArrayList<Item>()

    //绑定自己所属的 task
    lateinit var task: UTask

    //绑定自己的 io 管理器
    val ioManager: AbIoManager = ConfigCenter.getIO(this)

    //该条下载的状态
    var state = State.READY
    override fun toString(): String {
        return "Item(url='$url', path='$path', md5='$md5', size=$size)"
    }
}
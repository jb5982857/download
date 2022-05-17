package com.dhu.usdk.support.udownload

import android.app.Activity
import android.app.Application
import android.content.Context
import com.dhu.usdk.support.udownload.common.StateCode
import com.dhu.usdk.support.udownload.common.ConfigCenter
import com.dhu.usdk.support.udownload.modules.download.DownloadManager
import com.dhu.usdk.support.udownload.modules.download.UInternalTask
import com.dhu.usdk.support.udownload.modules.network.NetWorkStateManager
import com.dhu.usdk.support.udownload.support.io.AbIoManager
import com.dhu.usdk.support.udownload.support.queue.SuccessTasks
import com.dhu.usdk.support.udownload.utils.ULog
import com.dhu.usdk.support.udownload.utils.application
import com.dhu.usdk.support.udownload.utils.switchCallbackThreadIfNeed
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.collections.ArrayList

@Volatile
var taskIndex = 0

class UTask(
    val isShowNotification: Boolean = true,
    val name: String = taskIndex++.toString()
) {
    companion object {
        fun setApplication(app: Application) {
            application = app
        }
    }

    private var isStart = false
    val downloadQueue = ConcurrentLinkedQueue<Item>()
    val successTasks = SuccessTasks(this)
    val failedTasks = ConcurrentLinkedQueue<Item>()
    var state: State? = null

    @Volatile
    var networkState: NetWorkStateManager.State? = null
    var networkChangeObserver: (NetWorkStateManager.State) -> Unit = {
        ULog.d("网络状态变化 :$it")
        networkState = it
    }

    //预设当前长度
    var currentLen = 0L

    //预设总长度
    var totalLen = 0L

    private val itemLock = Object()

    /**
     * 下载进度
     * params 1 -> 总大小，单位 byte
     * params 2 -> 已下载完成的大小，单位 byte
     * params 3 -> 当前速度，根据实际情况转换 kb/s , Mb/s , b/s
     */
    var downloadProgressListener = { _: Long, _: Long, _: Long -> }

    /**
     * 有文件下载完成，由这里同步
     */
    var downloadItemFinishListener: (item: Item) -> Unit = {}
        get() {
            if (isFinished()) {
                return {}
            } else {
                return field
            }
        }

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

    fun start(currentLen: Long, totalLen: Long, activity: Activity) {
        if (isStart) {
            "the task $name is started".apply {
                ULog.w(this)
            }
            return
        }
        this.currentLen = currentLen
        this.totalLen = totalLen
        ULog.i("开始下载，文件数 ${downloadQueue.size}")
        NetWorkStateManager.instance.addObserver(networkChangeObserver)
        switchCallbackThreadIfNeed {
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
        switchCallbackThreadIfNeed {
            downloadStateChangeListener(State.DOWNLOADING)
        }
    }

    fun pause() {
        state = State.ON_PAUSE
        switchCallbackThreadIfNeed {
            downloadStateChangeListener(State.ON_PAUSE)
        }
    }

    fun pauseOrResume() {
        if (state == State.ON_PAUSE) {
            restart()
        } else {
            pause()
        }
    }

    fun stop(context: Context) {
        state = State.ON_STOP
        synchronized(itemLock) {
            itemLock.notifyAll()
        }
        switchCallbackThreadIfNeed {
            downloadStateChangeListener(State.ON_STOP)
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

    fun isStop() = state == State.ON_STOP || state == State.ON_FINISH

    fun isFinished() =
        state == State.ON_STOP || state == State.ON_FINISH || state == State.SUCCESS || state == State.FAILED

    fun isDownloading() = state == State.DOWNLOADING

    fun isNetWorkValid() = networkState != NetWorkStateManager.State.NO_NETWORK
}

enum class State(val value: Int, val state: ResultState? = null) {
    READY(1), DOWNLOADING(2), ON_PAUSE(3), ON_STOP(4), CELLULAR_PAUSE(5), ON_FINISH(6), SUCCESS(
        0
    ),
    FAILED(
        -1
    )
}

data class ResultState(val code: Int = StateCode.SUCCESS, val message: String = "") {
    fun isSuccessful() = code == StateCode.SUCCESS

    fun isCancel() = code == StateCode.CANCEL
}

/**
 * url -> 下载的url
 * path -> 目标路径，下载过程中会生成 path.temp 文件，下载完成后 rename 到 path
 * md5 -> 文件的 md5 值
 * size -> 文件大小，单位 byte
 * userData -> 用户的信息，C# 地址
 * tag -> 透传的数据
 * extendData -> 扩展参数
 */
data class Item(
    val url: String,
    val path: String,
    var md5: String,
    val size: Long,
    val userData: Int,
    val tag: String,
    val priority: Int,
    val extendData: String
) {
    companion object {
        //最大的重试次数
        const val RETRY_COUNT_MAX = 5
    }

    //重复的 item ，必须是 url 和 path 相同
    val duplicateItem = ArrayList<Item>()

    //绑定自己所属的 task
    lateinit var task: UTask

    //绑定自己的 io 管理器
    val ioManager: AbIoManager = ConfigCenter.getIO(this)

    //重试次数
    var retryCount = 0

    fun needRetry(): Boolean {
        return retryCount++ < RETRY_COUNT_MAX
    }

    fun isSuccessful(): Boolean {
        return resultState?.isSuccessful() ?: false
    }

    //该条下载的状态
    var resultState: ResultState? = null

    override fun toString(): String {
        return "Item(url='$url', path='$path', md5='$md5', size=$size resultState=$resultState retryCount=$retryCount)"
    }
}
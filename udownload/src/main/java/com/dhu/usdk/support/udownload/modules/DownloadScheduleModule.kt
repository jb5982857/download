package com.dhu.usdk.support.udownload.modules

import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import android.os.Message
import com.dhu.usdk.support.udownload.UDownloadService
import com.dhu.usdk.support.udownload.UTask
import com.dhu.usdk.support.udownload.support.io.AbIoManager
import com.dhu.usdk.support.udownload.utils.*
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.collections.ArrayList

/**
 * 计算每个 UTask 的下载速度和进度等
 */
val downloadHandlerThread by lazy {
    HandlerThread("download speed thread").apply {
        start()
    }
}

class DownloadScheduleModule() {
    private val ioManagers = ConcurrentLinkedQueue<AbIoManager>()

    @Volatile
    private var isStart = false
    private var mHandler: Handler? = null

    private var successLen: Long = 0L
    private var totalLen: Long = 0L
    private var notificationId: Int? = null
    private var task: UTask? = null

    companion object {
        const val DELAY_TIME = 1_000L
        const val WHAT_ADD = 20
        const val WHAT_SCHEDULE = 30
        const val WHAT_STOP = 40
    }

    fun init(successLen: Long, totalLen: Long, notificationId: Int?, task: UTask) {
        ULog.d("init success $successLen, total $totalLen")
        this.successLen = successLen
        this.totalLen = totalLen
        this.notificationId = notificationId
        this.task = task
    }

    fun start(context: Context) {
        if (isStart) {
            ULog.e("duplicate start call")
            return
        }

        isStart = true

        mHandler = Handler(downloadHandlerThread.looper) {
            if (!isStart) {
                return@Handler true
            }
            when (it.what) {
                WHAT_ADD -> {
                    if (it.obj is AbIoManager) {
                        ioManagers.add(it.obj as AbIoManager)
                    }
                }

                WHAT_SCHEDULE -> {
                    val lastSuccessLen = successLen
                    val needRemoveManagers = ArrayList<AbIoManager>()
                    ioManagers.forEach {
                        successLen += it.getBufferedLen()
                        if (it.isWriteFinish) {
                            needRemoveManagers.add(it)
                        }
                    }

                    val speed = getSpeed(
                        successLen - lastSuccessLen,
                        (DELAY_TIME / 1000).toInt()
                    )
                    notificationId?.apply {
                        val progress = successLen.toFloat() * 100 / totalLen
                        NotificationModule.updateProgress(
                            context,
                            this,
                            progress.toInt(),
                            "下载进度 ${decimalFormat.format(progress)}% , 下载速度 $speed"
                        )
                    }
                    switchUiThreadIfNeeded {
                        //回调速度
                        task?.downloadProgressListener?.let { it1 ->
                            it1(
                                totalLen,
                                successLen,
                                speed
                            )
                        }
                    }
                    needRemoveManagers.forEach {
                        ioManagers.remove(it)
                    }
                    if (isStart) {
                        task?.lockItemTaskIfNeeded()
                        mHandler?.sendEmptyMessageDelayed(WHAT_SCHEDULE, DELAY_TIME)
                    }
                }
            }

            return@Handler true
        }

        mHandler?.sendEmptyMessageDelayed(WHAT_SCHEDULE, DELAY_TIME)
    }

    fun add(ioManager: AbIoManager) {
        Message.obtain(mHandler, WHAT_ADD, ioManager).sendToTarget()
    }

    fun stop() {
        isStart = false
        mHandler?.removeMessages(WHAT_SCHEDULE)
        mHandler?.removeMessages(WHAT_ADD)
    }
}
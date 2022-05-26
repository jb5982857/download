package com.dhu.usdk.support.udownload.modules

import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import android.os.Message
import com.dhu.usdk.support.udownload.R
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
    @Volatile
    private var isStart = false
    private var mHandler: Handler? = null

    private var successLen: Long = 0L
    private var bufferLen: Long = 0L
    private var totalLen: Long = 0L
    private var notificationId: Int? = null
    private var task: UTask? = null

    companion object {
        const val DELAY_TIME = 1_000L
        const val WHAT_SCHEDULE = 30
        const val WHAT_STOP = 40
        const val WHAT_ADD_PROGRESS = 50
        const val WHAT_ADD_INIT_SIZE = 60
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
                WHAT_ADD_PROGRESS -> {
                    if (it.obj is Int) {
                        bufferLen += it.obj as Int
                    }
                }

                WHAT_ADD_INIT_SIZE -> {
                    if (it.obj is Long) {
                        (it.obj as Long).apply {
                            successLen += this
                        }
                    }
                }

                WHAT_SCHEDULE -> {
                    val byteSpeed = getByteSpeed(
                        bufferLen,
                        (DELAY_TIME / 1000).toInt()
                    )
                    successLen += bufferLen
                    val formatSpeed = getFormatSpeed(byteSpeed)
                    ULog.d("byteSpeed $byteSpeed $formatSpeed")
                    notificationId?.apply {
                        val progress = successLen.toFloat() * 100 / totalLen
                        ULog.d("notificationId $progress")
                        NotificationModule.updateProgress(
                            context,
                            this,
                            progress.toInt(),
                            String.format(
                                application.getString(R.string.udownload_progress_content),
                                "${decimalFormat.format(progress)}%",
                                formatSpeed
                            )
                        )
                    }
                    switchCallbackThreadIfNeed {
                        ULog.d("byteSpeed $byteSpeed")
                        //回调速度
                        task?.downloadProgressListener?.let { it1 ->
                            it1(
                                totalLen,
                                successLen,
                                byteSpeed
                            )
                        }
                    }

                    if (isStart) {
                        task?.lockItemTaskIfNeeded()
                        mHandler?.sendEmptyMessageDelayed(WHAT_SCHEDULE, DELAY_TIME)
                    }

                    bufferLen = 0L
                }
            }

            return@Handler true
        }

        mHandler?.sendEmptyMessageDelayed(WHAT_SCHEDULE, DELAY_TIME)
    }

    fun addProgress(progress: Int) {
        Message.obtain(mHandler, WHAT_ADD_PROGRESS, progress).sendToTarget()
    }

    fun addInitSize(size: Long) {
        Message.obtain(mHandler, WHAT_ADD_INIT_SIZE, size).sendToTarget()
    }

    fun stop() {
        isStart = false
        mHandler?.removeMessages(WHAT_SCHEDULE)
        mHandler?.removeMessages(WHAT_ADD_PROGRESS)
        mHandler?.removeMessages(WHAT_ADD_INIT_SIZE)
    }
}
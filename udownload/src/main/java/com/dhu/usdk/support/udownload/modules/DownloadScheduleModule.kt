package com.dhu.usdk.support.udownload.modules

import android.app.Notification
import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import com.dhu.usdk.support.udownload.support.io.AbIoManager
import com.dhu.usdk.support.udownload.utils.ULog
import com.dhu.usdk.support.udownload.utils.decimalFormat
import com.dhu.usdk.support.udownload.utils.getSpeed
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
    private var isStart = false
    private var mHandler: Handler? = null

    private var successLen: Long = 0L
    private var totalLen: Long = 0L
    private var notificationId: Int? = null

    companion object {
        const val DELAY_TIME = 1_000L
    }

    fun init(successLen: Long, totalLen: Long, notificationId: Int?) {
        this.successLen = successLen
        this.totalLen = totalLen
        this.notificationId = notificationId
    }

    fun start(context: Context) {
        if (isStart) {
            ULog.e("duplicate start call")
            return
        }

        mHandler = Handler(downloadHandlerThread.looper) {
            val lastSuccessLen = successLen
            val needRemoveManagers = ArrayList<AbIoManager>()
            ioManagers.forEach {
                successLen += it.writeLen
                if (it.isWriteFinish) {
                    needRemoveManagers.add(it)
                }
            }

            notificationId?.apply {
                val progress = successLen.toFloat() * 100 / totalLen
                NotificationModule.updateProgress(
                    context,
                    this,
                    progress.toInt(), "下载进度 ${decimalFormat.format(progress)} , 下载速度 ${
                        getSpeed(
                            successLen - lastSuccessLen,
                            (DELAY_TIME / 1000).toInt()
                        )
                    }"
                )
            }
            needRemoveManagers.forEach {
                ioManagers.remove(it)
            }
            mHandler?.sendEmptyMessageDelayed(0x123, DELAY_TIME)
            return@Handler true
        }

        mHandler?.sendEmptyMessageDelayed(0x123, DELAY_TIME)
    }

    fun add(ioManager: AbIoManager) {
        ioManagers.add(ioManager)
    }
}
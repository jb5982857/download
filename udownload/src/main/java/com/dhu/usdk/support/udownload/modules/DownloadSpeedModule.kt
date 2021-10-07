package com.dhu.usdk.support.udownload.modules

import android.os.Handler
import android.os.HandlerThread
import com.dhu.usdk.support.udownload.support.io.AbIoManager
import com.dhu.usdk.support.udownload.utils.ULog
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

class DownloadSpeedModule {
    private val ioManagers = ConcurrentLinkedQueue<AbIoManager>()
    private var isStart = false
    private var mHandler: Handler? = null
    private var lastTaskLen = 0L

    companion object {
        const val DELAY_TIME = 1_000L
    }

    fun start() {
        if (isStart) {
            ULog.e("duplicate start call")
            return
        }

        mHandler = Handler(downloadHandlerThread.looper) {
            var allTaskLen = 0L
            val needRemoveManagers = ArrayList<AbIoManager>()
            ioManagers.forEach {
                allTaskLen += it.writeLen
                if (it.isWriteFinish) {
                    needRemoveManagers.add(it)
                }
            }
            ULog.d("speed :${getSpeed(allTaskLen - lastTaskLen, (DELAY_TIME / 1000).toInt())}")
            lastTaskLen = allTaskLen
            ioManagers.removeAll(needRemoveManagers)
            mHandler?.sendEmptyMessageDelayed(0x123, DELAY_TIME)
            return@Handler true
        }

        mHandler?.sendEmptyMessageDelayed(0x123, DELAY_TIME)
    }

    fun add(ioManager: AbIoManager) {
        ioManagers.add(ioManager)
    }

}
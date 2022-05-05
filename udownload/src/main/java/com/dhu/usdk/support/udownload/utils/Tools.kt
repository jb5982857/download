package com.dhu.usdk.support.udownload.utils

import android.app.Application
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import java.text.DecimalFormat

val mainHandler = Handler(Looper.getMainLooper())

lateinit var application: Application

var decimalFormat = DecimalFormat("#.##")

/**
 * 获取byte/s 的速度
 */
fun getByteSpeed(size: Long, time: Int): Long {
    return size / time
}

/**
 * 获取速度，并且format
 * size -> 单位 byte
 * time -> 单位 s
 */
fun getFormatSpeed(byteSpeed: Long): String {
    val kb = byteSpeed / 1024f
    val mb = kb / 1024f
    val gb = mb / 1024f
    if (gb >= 1) {
        return "${decimalFormat.format(gb)} gb/s"
    }
    if (mb >= 1) {
        return "${decimalFormat.format(mb)} mb/s"
    }
    if (kb >= 1) {
        return "${decimalFormat.format(kb)} kb/s"
    }

    return "${decimalFormat.format(byteSpeed)} b/s"
}


fun switchUiThreadIfNeeded(action: () -> Unit) {
    if (Looper.myLooper() == Looper.getMainLooper()) {
        action()
    } else {
        mainHandler.post { action() }
    }
}


private val mCallbackHandler by lazy {
    val threadStart = HandlerThread("download_callback_thread")
    threadStart.start()
    val handler = Handler(threadStart.looper)
    handler
}

fun switchCallbackThreadIfNeed(action: () -> Unit) {
    if (Looper.myLooper() == mCallbackHandler.looper) {
        action()
    } else {
        mCallbackHandler.post {
            action()
        }
    }
}
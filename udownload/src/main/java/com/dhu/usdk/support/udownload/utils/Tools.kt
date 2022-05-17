package com.dhu.usdk.support.udownload.utils

import android.app.Application
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import java.text.DecimalFormat

val mainHandler = Handler(Looper.getMainLooper())

lateinit var application: Application

var decimalFormat = DecimalFormat("#0.00")

/**
 * 获取byte/s 的速度
 */
fun getByteSpeed(size: Long, time: Int): Long {
    return size / time
}

fun versionCheck(target: Int = 0, build: Int = 0): Boolean {
    if (target != 0) {
        if (application.applicationInfo.targetSdkVersion < target) {
            return false
        }
    }

    if (build != 0) {
        if (Build.VERSION.SDK_INT < build) {
            return false
        }
    }

    return true
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
        return "${decimalFormat.format(gb)} GB/S"
    }
    if (mb >= 1) {
        return "${decimalFormat.format(mb)} MB/S"
    }
    if (kb >= 1) {
        return "${decimalFormat.format(kb)} KB/S"
    }

    return "${decimalFormat.format(byteSpeed)} B/S"
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
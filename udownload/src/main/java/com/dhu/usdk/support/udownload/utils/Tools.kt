package com.dhu.usdk.support.udownload.utils

import android.app.Application
import java.text.DecimalFormat

val application by lazy {
    try {
        return@lazy Class.forName("android.app.ActivityThread")
            .getMethod("currentApplication")
            .invoke(null) as Application
    } catch (e: Exception) {
        e.printStackTrace();
    }
    return@lazy null
}

var decimalFormat = DecimalFormat("#.##")

/**
 * size -> 单位 byte
 * time -> 单位 s
 */
fun getSpeed(size: Long, time: Int): String {
    val kb = size / 1024f
    val mb = kb / 1024f
    val gb = mb / 1024f
    if (gb >= 1) {
        return "${decimalFormat.format(gb / time)} gb/s"
    }
    if (mb >= 1) {
        return "${decimalFormat.format(mb / time)} mb/s"
    }
    if (kb >= 1) {
        return "${decimalFormat.format(kb / time)} kb/s"
    }

    return "${decimalFormat.format(size.toFloat() / time)} b/s"
}

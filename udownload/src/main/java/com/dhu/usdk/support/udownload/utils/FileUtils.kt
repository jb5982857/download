package com.dhu.usdk.support.udownload.utils

import android.os.Build
import android.os.FileUtils
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import kotlin.experimental.and


fun File.md5(): String? {
    if (!this.isFile) {
        return null
    }
    var digest: MessageDigest? = null
    var `in`: FileInputStream? = null
    val buffer = ByteArray(1024)
    var len: Int
    try {
        digest = MessageDigest.getInstance("MD5")
        `in` = FileInputStream(this)
        while (`in`.read(buffer, 0, 1024).also { len = it } != -1) {
            digest.update(buffer, 0, len)
        }
        `in`.close()
    } catch (e: Exception) {
        e.printStackTrace()
        return null
    }
    return bytesToHexString(digest.digest())
}

fun bytesToHexString(src: ByteArray?): String? {
    val stringBuilder = StringBuilder("")
    if (src == null || src.isEmpty()) {
        return null
    }
    for (i in src.indices) {
        val v = (src[i] and 0xFF.toByte()).toInt()
        val hv = Integer.toHexString(v)
        if (hv.length < 2) {
            stringBuilder.append(0)
        }
        stringBuilder.append(hv)
    }
    return stringBuilder.toString()
}

fun moveData(source: File, target: File): Boolean {
    val start = System.currentTimeMillis()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val sourceP: Path = source.toPath()
        val targetP: Path = target.toPath()
        if (target.exists()) {
            ULog.d("moveData delete data")
            if (!target.delete()) {
                return false
            }
        }
        try {
            Files.move(sourceP, targetP, StandardCopyOption.ATOMIC_MOVE)
            ULog.d("moveData Files.move")
        } catch (e: IOException) {
            ULog.e("Files move file error", e)
            return false
        }
    } else {
        if (target.exists()) {
            ULog.d("moveData delete data")
            if (!target.delete()) {
                return false
            }
        }
        val result = source.renameTo(target)
        ULog.d("moveData renameTo result $result")
        if (!result) {
            return false
        }
    }

    val end = System.currentTimeMillis()
    val `val` = end - start
    ULog.d(
        "migrate data take time " + `val` + " from " + source.absolutePath + " to " + target.absolutePath
    )
    return true
}
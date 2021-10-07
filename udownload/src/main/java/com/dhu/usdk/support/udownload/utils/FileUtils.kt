package com.dhu.usdk.support.udownload.utils

import android.os.Build
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.math.BigInteger
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException


fun File.md5(): String {
    var result = ""
    var fis: FileInputStream? = null
    val buffer = ByteArray(8192)
    var length: Int
    val md: MessageDigest
    val algorithm = "MD5"
    try {
        md = MessageDigest.getInstance(algorithm)
        fis = FileInputStream(this)
        while (true) {
            length = fis.read(buffer)
            if (length == -1) {
                break
            } else {
                md.update(buffer, 0, length)
            }
        }
        //digest() 只能调用一次~~~ 相关数据会被重置成初始状态
        result = BigInteger(1, md.digest()).toString(16)
    } catch (e: NoSuchAlgorithmException) {

    } catch (e: FileNotFoundException) {

    } finally {
        try {
            fis?.close()
        } catch (e: IOException) {

        }
    }
    return result
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
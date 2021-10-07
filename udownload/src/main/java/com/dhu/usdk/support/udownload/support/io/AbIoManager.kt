package com.dhu.usdk.support.udownload.support.io

import com.dhu.usdk.support.udownload.utils.ULog
import com.dhu.usdk.support.udownload.utils.md5
import com.dhu.usdk.support.udownload.utils.moveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.InputStream

abstract class AbIoManager {
    //是否完成到本地的写入
    var isWriteFinish = false

    //新下载且写入的长度
    var writeLen = 0L
        set(value) {
            field = value
            ULog.d("the all len is $writeLen")
        }

    companion object {
        const val TEMP = ".temp"
    }

    suspend fun checkFileIfExist(filePath: String, md5: String): FileCheckData {
        return withContext(Dispatchers.IO) {
            val file = File(filePath)
            if (file.exists()) {
                val fileMd5 = file.md5()
                if (fileMd5 == md5) {
                    return@withContext FileCheckData(true, file.length())
                } else {
                    ULog.e("file $filePath exist, but the md5 is failed, $fileMd5 vs $md5")
                }
            }

            val tempFile = File(getTempPath(filePath))
            val len = tempFile.length()
            if (tempFile.exists() && tempFile.md5() == md5) {
                if (mv2TargetFile(filePath)) {
                    return@withContext FileCheckData(true, len)
                }
            }
            return@withContext FileCheckData(false, len)
        }
    }

    fun mv2TargetFile(filePath: String): Boolean {
        return moveData(File(getTempPath(filePath)), File(filePath))
    }

    fun writeFile(filePath: String, inputStream: InputStream): Boolean {
        createTempFileIfNeed(filePath)
        if (!saveFile(getTempPath(filePath), inputStream)) {
            return false
        }
        isWriteFinish = true
        return mv2TargetFile(filePath)
    }

    private fun createTempFileIfNeed(filePath: String) {
        File(filePath).apply {
            if (!exists()) {
                parentFile?.mkdirs()
                createNewFile()
            }
        }
    }

    fun getTempFileSize(filePath: String): Long {
        val file = File(getTempPath(filePath))
        return if (file.exists()) {
            file.length()
        } else {
            0L
        }
    }

    abstract fun saveFile(filePath: String, inputStream: InputStream): Boolean

    protected fun getTempPath(filePath: String): String {
        return filePath + TEMP
    }
}

data class FileCheckData(val exist: Boolean, val len: Long)
package com.dhu.usdk.support.udownload.support.io

import com.dhu.usdk.support.udownload.Item
import com.dhu.usdk.support.udownload.utils.ULog
import com.dhu.usdk.support.udownload.utils.md5
import com.dhu.usdk.support.udownload.utils.moveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.util.*

/**
 * io 应该是每个 Item 有一个 io 对象
 */
abstract class AbIoManager(private val item: Item) {
    //该 item 是否完成到本地的写入
    var isWriteFinish = false

    //新下载且写入的长度
    @Volatile
    protected var bufferLen = 0L

    companion object {
        const val TEMP = ".download"
    }

    fun getBufferedLen(): Long {
        val result = bufferLen
        bufferLen = 0
        return result
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

    fun writeFile(filePath: String, inputStream: InputStream, md5: String): Boolean {
        createTempFileIfNeed(filePath)
        if (!saveFile(getTempPath(filePath), inputStream)) {
            return false
        }
        isWriteFinish = true
        val tempFile = File(getTempPath(filePath))
        val tempMd5 = tempFile.md5()
        return if (tempFile.md5() == md5) {
            mv2TargetFile(filePath)
        } else {
            ULog.e("download finish but md5 error , $tempMd5 vs $md5 ")
            false
        }
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

    protected fun lockItemTaskIfNeeded() {
        item.task.lockItemTaskIfNeeded()
    }

    abstract fun saveFile(filePath: String, inputStream: InputStream): Boolean

    protected fun getTempPath(filePath: String): String {
        return filePath + TEMP
    }
}

data class FileCheckData(val exist: Boolean, val len: Long)
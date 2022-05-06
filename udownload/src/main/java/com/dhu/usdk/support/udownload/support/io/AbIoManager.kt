package com.dhu.usdk.support.udownload.support.io

import com.dhu.usdk.support.udownload.Item
import com.dhu.usdk.support.udownload.ResultState
import com.dhu.usdk.support.udownload.UDownloadService
import com.dhu.usdk.support.udownload.common.StateCode
import com.dhu.usdk.support.udownload.modules.ReportModule
import com.dhu.usdk.support.udownload.utils.ULog
import com.dhu.usdk.support.udownload.utils.application
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

    //上次已经下好的长度，用于计算速度之类的
    @Volatile
    private var initSuccessLen = 0L


    companion object {
        const val TEMP = ".download"
    }

    fun getBufferedLen(): Long {
        val result = bufferLen
        bufferLen = 0
        return result
    }

    fun getInitSuccessLen(): Long {
        if (initSuccessLen == 0L) {
            return initSuccessLen
        }
        val result = initSuccessLen
        initSuccessLen = 0L
        return result
    }

    fun clearInitSuccessLen() {
        initSuccessLen = 0L
    }

    suspend fun checkFileIfExist(): FileCheckData {
        val filePath = item.path
        return withContext(Dispatchers.IO) {
            val file = File(filePath)
            if (file.exists()) {
                val fileMd5 = file.md5()
                if (fileMd5 == item.md5) {
                    initSuccessLen = file.length()
                    return@withContext FileCheckData(true, file.length())
                } else {
                    ULog.e("file $filePath exist, but the md5 is failed, $fileMd5 vs ${item.md5}")
                }
            }

            val tempFile = File(getTempPath(filePath))
            if (tempFile.exists()) {
                val len = tempFile.length()
                if (tempFile.md5() == item.md5) {
                    if (mv2TargetFile()) {
                        initSuccessLen = file.length()
                        return@withContext FileCheckData(true, len)
                    }
                }
                initSuccessLen = len
                return@withContext FileCheckData(false, len)
            }

            initSuccessLen = 0
            return@withContext FileCheckData(false, 0L)
        }
    }

    private fun mv2TargetFile(): Boolean {
        val filePath = item.path
        return moveData(File(getTempPath(filePath)), File(filePath))
    }

    fun writeFile(
        inputStream: InputStream,
        isSupportRange: Boolean,
        md5: String
    ): ResultState {
        val filePath = item.path
        createTempFileIfNeed()
        val saveResult = saveFile(
            getTempPath(filePath),
            inputStream,
            isSupportRange
        )
        if (!saveResult.isSuccessful()) {
            isWriteFinish = true
            return saveResult
        }
        isWriteFinish = true
        val tempFile = File(getTempPath(filePath))
        val tempMd5 = tempFile.md5()
        return if (tempFile.md5() == md5) {
            return if (mv2TargetFile()) {
                ResultState(StateCode.SUCCESS, "")
            } else {
                ResultState(StateCode.MOVE_FILE_FAILED, "")
            }
        } else {
            val msg = "download finish but md5 error , $tempMd5 vs $md5 "
            ULog.e(msg)
            tempFile.delete()
            ResultState(StateCode.MD5_ERROR, msg)
        }
    }

    private fun createTempFileIfNeed() {
        val filePath = item.path
        File(filePath).apply {
            if (!exists()) {
                parentFile?.mkdirs()
                createNewFile()
            }
        }
    }

    fun getTempFileSize(): Long {
        val file = File(getTempPath(item.path))
        return if (file.exists()) {
            file.length()
        } else {
            0L
        }
    }

    protected fun lockItemTaskIfNeeded() {
        item.task.lockItemTaskIfNeeded()
    }

    protected fun isStop() = item.task.isStop()

    abstract fun saveFile(
        filePath: String,
        inputStream: InputStream,
        isSupportRange: Boolean
    ): ResultState

    protected fun getTempPath(filePath: String): String {
        return filePath + TEMP
    }
}

data class FileCheckData(val exist: Boolean, val len: Long)
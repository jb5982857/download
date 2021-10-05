package com.dhu.usdk.support.udownload.support.io

import com.dhu.usdk.support.udownload.utils.md5
import com.dhu.usdk.support.udownload.utils.moveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.InputStream

abstract class AbIoManager {
    companion object {
        const val TEMP = ".temp"
    }

    suspend fun checkFileIfExist(filePath: String, md5: String): Boolean {
        return withContext(Dispatchers.IO) {
            val file = File(filePath)
            if (file.exists() && file.md5() == md5) {
                return@withContext true
            }

            val tempFile = File(getTempPath(filePath))
            if (tempFile.exists() && tempFile.md5() == md5) {
                return@withContext mv2TargetFile(filePath)
            }
            return@withContext false
        }
    }

    suspend fun mv2TargetFile(filePath: String): Boolean {
        return withContext(Dispatchers.IO) {
            return@withContext moveData(File(getTempPath(filePath)), File(filePath))
        }
    }

    suspend fun getTempFileSize(filePath: String): Long {
        return withContext(Dispatchers.IO) {
            val file = File(filePath)
            if (file.exists()) {
                return@withContext file.length()
            } else {
                return@withContext 0L
            }
        }
    }

    abstract fun saveFile(filePath: String, inputStream: InputStream)

    private fun getTempPath(filePath: String): String {
        return filePath + TEMP
    }
}
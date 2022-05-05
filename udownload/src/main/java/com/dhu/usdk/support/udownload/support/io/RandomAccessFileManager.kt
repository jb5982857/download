package com.dhu.usdk.support.udownload.support.io

import com.dhu.usdk.support.udownload.Item
import com.dhu.usdk.support.udownload.modules.download.UInternalTask
import com.dhu.usdk.support.udownload.utils.ULog
import java.io.*

class RandomAccessFileManager(item: Item) :
    AbIoManager(item) {
    override fun saveFile(
        filePath: String,
        inputStream: InputStream,
        isSupportRange: Boolean
    ): Boolean {
        var raf: RandomAccessFile? = null
        try {
            raf = RandomAccessFile(filePath, "rw")
            val seek = if (isSupportRange) {
                raf.length()
            } else {
                0
            }
            ULog.d("$filePath the seek is $seek")
            raf.seek(seek)
            var len: Int
            val buffer = ByteArray(1024 * 1024)
            while (inputStream.read(buffer).also {
                    len = it
                } != -1) {
                raf.write(buffer, 0, len)
                bufferLen += len
                lockItemTaskIfNeeded()
            }
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
            return false
        } catch (e: IOException) {
            ULog.e("download 的write error ", e)
            return false
        } finally {
            try {
                inputStream.close()
                raf?.close()
            } catch (e: Exception) {
            }
        }
        return true
    }
}
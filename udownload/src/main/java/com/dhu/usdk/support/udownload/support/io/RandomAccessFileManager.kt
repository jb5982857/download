package com.dhu.usdk.support.udownload.support.io

import com.dhu.usdk.support.udownload.utils.ULog
import java.io.*

class RandomAccessFileManager : AbIoManager() {
    override fun saveFile(filePath: String, inputStream: InputStream): Boolean {
        var raf: RandomAccessFile? = null
        try {
            raf = RandomAccessFile(filePath, "rw")
            val seek = raf.length()
            ULog.d("$filePath the seek is $seek")
            raf.seek(seek)
            var len: Int
            val buffer = ByteArray(1024 * 1024)
            while (inputStream.read(buffer).also {
                    len = it
                } != -1) {
                raf.write(buffer, 0, len)
                writeLen += len
            }
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            try {
                raf?.close()
            } catch (e: Exception) {
            }
        }
        return true
    }
}
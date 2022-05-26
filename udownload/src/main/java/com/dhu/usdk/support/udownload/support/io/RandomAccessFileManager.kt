package com.dhu.usdk.support.udownload.support.io

import com.dhu.usdk.support.udownload.Item
import com.dhu.usdk.support.udownload.ResultState
import com.dhu.usdk.support.udownload.common.StateCode
import com.dhu.usdk.support.udownload.modules.download.UInternalTask
import com.dhu.usdk.support.udownload.utils.ULog
import java.io.*

class RandomAccessFileManager(item: Item) :
    AbIoManager(item) {
    override fun saveFile(
        filePath: String,
        inputStream: InputStream,
        isSupportRange: Boolean
    ): ResultState {
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
            //Android系统使用 Okhttp Okio 限制死了8k/s
            val buffer = ByteArray(8 * 1024)
            while (inputStream.read(buffer).also {
                    len = it
                } != -1) {
                if (isStop()) {
                    return ResultState(StateCode.CANCEL)
                }
                raf.write(buffer, 0, len)
                addProgress(len)
                lockItemTaskIfNeeded()
            }
            return ResultState(StateCode.SUCCESS, "")
        } catch (e: Exception) {
            ULog.e("write file error ", e)
            return ResultState(StateCode.WRITE_FILE_FAILED, e.message ?: "")
        } finally {
            try {
                inputStream.close()
                raf?.close()
            } catch (e: Exception) {
            }
        }
    }
}
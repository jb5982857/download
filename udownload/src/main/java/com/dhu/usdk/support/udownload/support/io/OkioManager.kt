package com.dhu.usdk.support.udownload.support.io

import com.dh.usdk.support.uokio.Okio
import java.io.File
import java.io.IOException
import java.io.InputStream

class OkioManager : AbIoManager() {

    override fun saveFile(filePath: String, inputStream: InputStream): Boolean {
        try {
            val bufferSource = Okio.buffer(Okio.source(inputStream))
            val sink = Okio.sink(File(filePath))
            val bufferedSink = Okio.buffer(sink)
            bufferedSink.writeAll(bufferSource)
        } catch (e: IOException) {
            return false
        }

        return true
    }
}
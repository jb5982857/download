package com.dhu.usdk.support.udownload.support.network

import com.dh.usdk.support.uokhttp.ConnectionPool
import com.dh.usdk.support.uokhttp.OkHttpClient
import com.dh.usdk.support.uokhttp.Request
import com.dh.usdk.support.uokhttp.loginterceptor.HttpLoggingInterceptor
import com.dhu.usdk.support.udownload.utils.ULog
import java.io.BufferedInputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit

class OkhttpFactory : IHttpDownload {
    private val okHttpClient by lazy {
        OkHttpClient.Builder().connectionPool(ConnectionPool(1, 1, TimeUnit.SECONDS))
            .addInterceptor(httpLogger).build()
    }
    private val httpLogger by lazy {
        HttpLoggingInterceptor {
            ULog.i("http-info -> $it")
        }.setLevel(HttpLoggingInterceptor.Level.HEADERS)
    }

    override fun download(url: String, start: Long): InputStream? {
        val request = Request.Builder()
            .addHeader("Range", "bytes=$start-")
            .url(url)
            .build()
        try {
            val response = okHttpClient.newCall(request).execute()
            response.body()?.byteStream().apply {
                return if (this == null) {
                    null
                } else {
                    object : BufferedInputStream(this) {
                        override fun close() {
                            super.close()
                            ULog.d("close $url")
                            response.close()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            ULog.e("download error", e)
            return null
        }
    }
}
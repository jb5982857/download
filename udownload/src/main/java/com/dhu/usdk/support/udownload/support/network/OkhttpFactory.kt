package com.dhu.usdk.support.udownload.support.network

import com.dh.usdk.support.uokhttp.OkHttpClient
import com.dh.usdk.support.uokhttp.Request
import com.dh.usdk.support.uokhttp.loginterceptor.HttpLoggingInterceptor
import com.dhu.usdk.support.udownload.utils.ULog
import java.io.BufferedInputStream
import java.io.InputStream

class OkhttpFactory : IHttpDownload {
    private val okHttpClient by lazy { OkHttpClient.Builder().addInterceptor(httpLogger).build() }
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
            okHttpClient.newCall(request).execute().body()?.byteStream().apply {
                return if (this == null) {
                    null
                } else {
                    BufferedInputStream(this)
                }
            }
        } catch (e: Exception) {
            return null
        }
    }
}
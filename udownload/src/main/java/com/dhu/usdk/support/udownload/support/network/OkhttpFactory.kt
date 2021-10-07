package com.dhu.usdk.support.udownload.support.network

import com.dh.usdk.support.uokhttp.OkHttpClient
import com.dh.usdk.support.uokhttp.Request
import com.dh.usdk.support.uokhttp.loginterceptor.HttpLoggingInterceptor
import com.dhu.usdk.support.udownload.utils.ULog
import java.io.InputStream

class OkhttpFactory : IHttpDownload {
    private val okHttpClient by lazy { OkHttpClient.Builder().addInterceptor(httpLogger).build() }
    private val httpLogger by lazy {
        HttpLoggingInterceptor {
            ULog.i("http-info -> $it")
        }.setLevel(HttpLoggingInterceptor.Level.BODY)
    }

    override fun download(url: String, start: Long): InputStream? {
        val request = Request.Builder()
            .addHeader("Range", "bytes=$start-")
            .url(url)
            .build()
        return try {
            okHttpClient.newCall(request).execute().body()?.byteStream()
        } catch (e: Exception) {
            null
        }
    }
}
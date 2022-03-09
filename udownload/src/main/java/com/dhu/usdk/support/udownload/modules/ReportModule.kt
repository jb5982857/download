package com.dhu.usdk.support.udownload.modules

import com.dh.usdk.support.uokhttp.*
import com.dhu.usdk.support.udownload.modules.download.DownloadManager
import com.dhu.usdk.support.udownload.utils.ULog
import java.io.IOException
import java.util.concurrent.TimeUnit

class ReportModule private constructor() {
    private val okHttpClient by lazy {
        OkHttpClient.Builder().build()
    }

    object Holder {
        val hold = ReportModule()
    }

    companion object {
        val instance = Holder.hold
    }

    init {

    }

    fun report(msg: String) {
//        val requestBody: RequestBody =
//            RequestBody.create(MediaType.get("application/json; charset=utf-8"), msg)
//        val request = Request.Builder()
//            .url("http://10.0.9.206:8091/udownload/report")
//            .post(requestBody).build()
//        okHttpClient.newCall(request).enqueue(object : Callback {
//            override fun onFailure(p0: Call, p1: IOException) {
////                ULog.e("onFailure ", p1)
//            }
//
//            override fun onResponse(p0: Call, p1: Response) {
//
//            }
//
//        })
    }
}
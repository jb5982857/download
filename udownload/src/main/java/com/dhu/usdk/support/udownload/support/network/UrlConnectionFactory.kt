package com.dhu.usdk.support.udownload.support.network

import com.dhu.usdk.support.udownload.ResultState
import com.dhu.usdk.support.udownload.common.StateCode
import com.dhu.usdk.support.udownload.utils.ULog
import java.io.InputStream
import java.net.*
import java.util.concurrent.TimeoutException

class UrlConnectionFactory : IHttpDownload {
    override fun download(url: String, start: Long): HttpDownloadResponse {
        try {
            val http = URL(url).openConnection() as HttpURLConnection
            http.connectTimeout = 5_000
            http.readTimeout = 10_000
            http.requestMethod = "GET"
            http.setRequestProperty(
                "Accept",
                "image/gif, image/jpeg, image/pjpeg, image/pjpeg, " +
                        "application/x-shockwave-flash, application/xaml+xml, application/vnd.ms-xpsdocument, " +
                        "application/x-ms-xbap, application/x-ms-application, application/vnd.ms-excel, " +
                        "application/vnd.ms-powerpoint, application/msword, */*"
            )
//            http.setRequestProperty("Accept-Language", "zh-CN")
//            http.setRequestProperty("Referer", url) // 先前网页的地址，当前请求网页紧随其后,即来路

//            http.setRequestProperty("Charset", "UTF-8")
            http.setRequestProperty("User-Agent", "udownload_android:1.0.0")

            http.setRequestProperty("Range", "bytes=$start-") //设置获取实体数据的范围

//        http.setRequestProperty(
//            "User-Agent",
//            ("Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 5.2; Trident/4.0; " +
//                    ".NET CLR 1.1.4322; .NET CLR 2.0.50727; .NET CLR 3.0.04506.30;" +
//                    " .NET CLR 3.0.4506.2152; .NET CLR 3.5.30729)")
//        )
            http.setRequestProperty("Connection", "Keep-Alive") //需要持久连接
            val responseCode = http.responseCode
            ULog.i("http response code $responseCode")
            if (responseCode == 206) {
                //支持断点续传
                return HttpDownloadResponse(ResultState(), http.inputStream, true)
            } else if (responseCode == 200 || responseCode == 416) {
                //不支持断点续传
                return HttpDownloadResponse(ResultState(), http.inputStream, false)
            }
            return HttpDownloadResponse(
                ResultState(
                    StateCode.UNKNOWN_NETWORK_ERROR,
                    "http response code is $responseCode"
                ), null
            )
        } catch (e: Exception) {
            ULog.e("http error", e)
            e.printStackTrace()
            var code = StateCode.UNKNOWN_NETWORK_ERROR
            if (e is UnknownHostException) {
                code = StateCode.NETWORK_UNREACHABLE
            }
            if (e is SocketTimeoutException || e is ConnectException) {
                code = StateCode.HTTP_TIME_OUT
            }
            return HttpDownloadResponse(
                ResultState(
                    code,
                    "http exception ${e.message}"
                ), null
            )
        }
    }
}
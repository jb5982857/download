package com.dhu.usdk.support.udownload.support.network

import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

class UrlConnectionFactory : IHttpDownload {
    override fun download(url: String, start: Long): InputStream? {
        try {
            val http = URL(url).openConnection() as HttpURLConnection
            http.connectTimeout = 5 * 1000
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

            http.setRequestProperty("Range", "bytes=$start-") //设置获取实体数据的范围

//        http.setRequestProperty(
//            "User-Agent",
//            ("Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 5.2; Trident/4.0; " +
//                    ".NET CLR 1.1.4322; .NET CLR 2.0.50727; .NET CLR 3.0.04506.30;" +
//                    " .NET CLR 3.0.4506.2152; .NET CLR 3.5.30729)")
//        )
            http.setRequestProperty("Connection", "Keep-Alive") //需要持久连接
            if (http.responseCode != 206) {
                return null
            }
            return http.inputStream
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
}
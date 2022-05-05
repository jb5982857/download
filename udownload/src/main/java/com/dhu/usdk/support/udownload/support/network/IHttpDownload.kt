package com.dhu.usdk.support.udownload.support.network

import java.io.InputStream

interface IHttpDownload {
    fun download(url: String, start: Long): HttpDownloadResponse
}

data class HttpDownloadResponse(val inputStream: InputStream?, val isSupportRange: Boolean = true)
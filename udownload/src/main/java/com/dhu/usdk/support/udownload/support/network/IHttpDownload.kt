package com.dhu.usdk.support.udownload.support.network

import com.dhu.usdk.support.udownload.ResultState
import java.io.InputStream

interface IHttpDownload {
    fun download(url: String, start: Long): HttpDownloadResponse
}

data class HttpDownloadResponse(
    val resultState: ResultState,
    val inputStream: InputStream?,
    val isSupportRange: Boolean = true
)
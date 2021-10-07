package com.dhu.usdk.support.udownload.support.network

import java.io.InputStream

interface IHttpDownload {
    fun download(url: String, start: Long): InputStream?
}
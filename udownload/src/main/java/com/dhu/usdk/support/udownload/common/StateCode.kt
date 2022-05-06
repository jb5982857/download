package com.dhu.usdk.support.udownload.common

object StateCode {
    const val SUCCESS = 0

    /**
     * 未知的Http请求异常错误
     */
    const val UNKNOWN_NETWORK_ERROR = 1000

    /**
     * 网络超时
     */
    const val HTTP_TIME_OUT = 1001

    /**
     * 写入文件失败
     */
    const val WRITE_FILE_FAILED = 2000

    /**
     * 下载成功之后，操作文件失败
     */
    const val MOVE_FILE_FAILED = 5000

    /**
     * md5 不匹配
     */
    const val MD5_ERROR = 5002

    /**
     * 网络不可用
     */
    const val NETWORK_UNREACHABLE = 5003

    /**
     * 空间不足
     */
    const val STORAGE_SPACE_NOT_ENOUGH = 5004

    const val CANCEL = -1
}
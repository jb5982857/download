package com.dhu.usdk.support.udownload.utils

import android.util.Log

/**
 * Created by jiangbo on 2019/4/17.
 * 日志工具
 */
object ULog {
    var tag: String = "UDownload"
    var allowV = false
    var allowD = true
    var allowI = true
    var allowW = true
    var allowE = true
    var allowWtf = true
    var isDebug = false

    private fun generateTag(): String {
        if (!isDebug) {
            return tag ?: ""
        }
        val caller = Thread.currentThread().stackTrace[4]
        var tag = "%s.%s(L:%d)"
        var callerClazzName = caller.className
        callerClazzName = callerClazzName.substring(callerClazzName.lastIndexOf(".") + 1)
        tag = String.format(
            tag,
            *arrayOf<Any>(callerClazzName, caller.methodName, Integer.valueOf(caller.lineNumber))
        )
        return if (ULog.tag != null) {
            ULog.tag + " " + tag
        } else tag
    }

    fun d(content: String?) {
        if (allowD) {
            val tag = generateTag()
            Log.d(tag, content!!)
        }
    }

    fun d(content: String?, tr: Throwable?) {
        if (allowD) {
            val tag = generateTag()
            Log.d(tag, content, tr)
        }
    }

    fun e(content: String?) {
        if (allowE) {
            val tag = generateTag()
            Log.e(tag, content!!)
        }
    }

    fun e(content: String?, tr: Throwable?) {
        if (allowE) {
            val tag = generateTag()
            Log.e(tag, content, tr)
        }
    }

    fun i(content: String?) {
        if (allowI) {
            val tag = generateTag()
            Log.i(tag, content!!)
        }
    }

    fun i(content: String?, tr: Throwable?) {
        if (allowI) {
            val tag = generateTag()
            Log.i(tag, content, tr)
        }
    }

    fun v(content: String?) {
        if (allowV) {
            val tag = generateTag()
            Log.v(tag, content!!)
        }
    }

    fun v(content: String?, tr: Throwable?) {
        if (allowV) {
            val tag = generateTag()
            Log.v(tag, content, tr)
        }
    }

    fun w(content: String?) {
        if (allowW) {
            val tag = generateTag()
            Log.w(tag, content!!)
        }
    }

    fun w(content: String?, tr: Throwable?) {
        if (allowW) {
            val tag = generateTag()
            Log.w(tag, content, tr)
        }
    }

    fun w(tr: Throwable?) {
        if (allowW) {
            val tag = generateTag()
            Log.w(tag, tr)
        }
    }

    fun wtf(content: String?) {
        if (allowWtf) {
            val tag = generateTag()
            Log.wtf(tag, content)
        }
    }

    fun wtf(content: String?, tr: Throwable?) {
        if (allowWtf) {
            val tag = generateTag()
            Log.wtf(tag, content, tr)
        }
    }
}
package com.dhu.usdk.support.udownload.modules.download

import com.dhu.usdk.support.udownload.UTask
import java.lang.ref.WeakReference

class DownloadTaskLifecycle private constructor() {
    private val tasks = mutableListOf<WeakReference<UInternalTask>>()

    companion object {
        val instance = Holder.hold
    }

    object Holder {
        val hold = DownloadTaskLifecycle()
    }

    fun add(uInternalTask: UInternalTask) {
        tasks.add(WeakReference(uInternalTask))
    }

    fun findTaskByNotificationId(notificationId: Int): UInternalTask? {
        return tasks.find { it.get()?.notificationId == notificationId }?.get()
    }

    fun findTaskByUTask(uTask: UTask): UInternalTask? {
        return tasks.find { it.get()?.uTask == uTask }?.get()
    }
}
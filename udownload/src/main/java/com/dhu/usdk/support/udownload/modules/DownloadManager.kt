package com.dhu.usdk.support.udownload.modules

import android.app.Notification
import android.content.Context
import com.dhu.usdk.support.udownload.UDownloadService
import com.dhu.usdk.support.udownload.UTask

class DownloadManager private constructor() {

    companion object {
        val instance = Holder.hold
    }

    object Holder {
        val hold = DownloadManager()
    }

    fun add(service: UDownloadService, task: UTask) {
        val uInternalTask = UInternalTask(task)
        if (task.isShowNotification) {
            uInternalTask.notification = NotificationModule.createNotification(service)
            uInternalTask.notificationId = NotificationModule.getNotificationId()
            service.startForeground(
                uInternalTask.notificationId,
                uInternalTask.notification
            )
        }


    }

    fun restart(context: Context, task: UTask) {

    }

    fun pause(context: Context, task: UTask) {

    }

    fun stop(context: Context, task: UTask) {

    }
}

data class UInternalTask(
    val uTask: UTask,
    var notification: Notification? = null,
    var notificationId: Int = NotificationModule.DEFAULT_ID
)
package com.dhu.usdk.support.udownload.modules.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.dhu.usdk.support.udownload.modules.download.DownloadTaskLifecycle
import com.dhu.usdk.support.udownload.utils.ULog

class ButtonClickReceive : BroadcastReceiver() {

    companion object {
        const val KEY_ACTION = "action"
        const val CANCEL = "cancel"
        const val PAUSE = "pause"
        const val NOTIFICATION_ID = "notificationId"

        const val DEFAULT_NOTIFICATION_ID = -1
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        context ?: return
        val action = intent?.getStringExtra(KEY_ACTION)
        val id =
            intent?.getIntExtra(NOTIFICATION_ID, DEFAULT_NOTIFICATION_ID) ?: DEFAULT_NOTIFICATION_ID
        ULog.d("onReceive ${System.identityHashCode(intent)} , $id")
        when (action) {
            PAUSE -> {
                if (id != DEFAULT_NOTIFICATION_ID) {
                    val uInternalTask = DownloadTaskLifecycle.instance.findTaskByNotificationId(id)
                    uInternalTask?.uTask?.pauseOrResume()
                }
            }

            CANCEL -> {

            }
        }
    }
}
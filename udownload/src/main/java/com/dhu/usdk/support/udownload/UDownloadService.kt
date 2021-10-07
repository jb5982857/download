package com.dhu.usdk.support.udownload

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import com.dhu.usdk.support.udownload.modules.DownloadManager
import com.dhu.usdk.support.udownload.utils.ObjectWrapperForBinder

class UDownloadService : Service() {
    companion object {
        const val ACTION = "action"
        const val TASK = "task"

        fun add(context: Context, uTask: UTask) {
            context.startService(Intent(context, UDownloadService::class.java).apply {
                putExtra(ACTION, Action.ADD.value)
                putExtra(TASK, ObjectWrapperForBinder.create(uTask))
            })
        }
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            return START_STICKY
        }

        val uTask = ObjectWrapperForBinder.getData(intent.getBundleExtra(TASK)) as UTask?

        when (intent.getIntExtra(ACTION, Action.NONE.value)) {
            Action.NONE.value -> {

            }

            Action.ADD.value -> {
                uTask?.apply {
                    DownloadManager.instance.add(this@UDownloadService, this)
                }
            }

            Action.PAUSE.value -> {
                uTask?.apply {
                    DownloadManager.instance.pause(this@UDownloadService, this)
                }
            }

            Action.RESTART.value -> {
                uTask?.apply {
                    DownloadManager.instance.restart(this@UDownloadService, this)
                }
            }

            Action.STOP.value -> {
                uTask?.apply {
                    DownloadManager.instance.stop(this@UDownloadService, this)
                }
            }
        }
        return START_STICKY
    }

}

enum class Action(val value: Int) {
    NONE(0), ADD(1), PAUSE(2), STOP(3), RESTART(4)
}
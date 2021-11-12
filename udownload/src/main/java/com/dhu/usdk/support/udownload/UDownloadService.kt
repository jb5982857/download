package com.dhu.usdk.support.udownload

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import com.dhu.usdk.support.udownload.modules.NotificationModule
import com.dhu.usdk.support.udownload.modules.download.DownloadManager
import com.dhu.usdk.support.udownload.modules.download.UInternalTask
import com.dhu.usdk.support.udownload.utils.ObjectWrapperForBinder

class UDownloadService : Service() {
    companion object {
        const val ACTION = "action"
        const val TASK = "task"

        @Volatile
        var isAlive = false

        fun add(context: Context, uTask: UTask) {
            val intent = Intent(context, UDownloadService::class.java).apply {
                putExtra(ACTION, Action.ADD.value)
                putExtra(TASK, ObjectWrapperForBinder.create(uTask))
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            return START_NOT_STICKY
        }

        val uTask = ObjectWrapperForBinder.getData(intent.getBundleExtra(TASK)) as UTask?
        if (!wakeLock.isHeld) {
            wakeLock.acquire(10 * 60 * 1000L /*10 minutes*/)
        }
        when (intent.getIntExtra(ACTION, Action.NONE.value)) {
            Action.NONE.value -> {

            }

            Action.ADD.value -> {
                uTask?.apply {
                    val uInternalTask = UInternalTask(this, downloadFinish = {
                        it.notificationId?.apply {
                            if (it.uTask.failedTasks.size == 0) {
                                NotificationModule.showSuccessNotification(
                                    this@UDownloadService,
                                    this
                                )
                            } else {
                                NotificationModule.showFailedNotification(
                                    this@UDownloadService,
                                    this
                                )
                            }
                            removeNotification(this)
                        }
                    })
                    uInternalTask.notification = NotificationModule.createNotification(
                        this@UDownloadService
                    )
                    isAlive = true
                    uInternalTask.notificationId =
                        NotificationModule.showForegroundService(
                            this@UDownloadService,
                            uInternalTask.notification!!
                        )
                    DownloadManager.instance.add(uInternalTask)
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
        return START_NOT_STICKY
    }

    private fun removeNotification(id: Int) {
        val lastExistNotification = NotificationModule.remove(id)
        NotificationModule.removeNotification(id)
        if (lastExistNotification == null) {
            stopSelf()
            isAlive = false
        }
        lastExistNotification?.apply {
            startForeground(this.id, this.notification)
        }
    }

    private val wakeLock: PowerManager.WakeLock by lazy {
        (getSystemService(Context.POWER_SERVICE) as PowerManager).run {
            newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyApp::UDownloadWakeLock")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        wakeLock.release()
    }
}

enum class Action(val value: Int) {
    NONE(0), ADD(1), PAUSE(2), STOP(3), RESTART(4)
}
package com.dhu.usdk.support.udownload

import android.app.Activity
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Binder
import android.os.IBinder
import android.os.PowerManager
import com.dhu.usdk.support.udownload.modules.NotificationModule
import com.dhu.usdk.support.udownload.modules.download.DownloadManager
import com.dhu.usdk.support.udownload.modules.download.UInternalTask
import com.dhu.usdk.support.udownload.utils.ObjectWrapperForBinder
import com.dhu.usdk.support.udownload.utils.ULog
import java.lang.ref.WeakReference

class UDownloadService : Service() {
    companion object {
        const val ACTION = "action"
        const val TASK = "task"

        var tempConn: ServiceConnection? = null

        @Volatile
        var isAlive = false

        fun add(activity: Activity, uTask: UTask) {
            val intent = Intent(activity, UDownloadService::class.java).apply {
                putExtra(ACTION, Action.ADD.value)
                putExtra(TASK, ObjectWrapperForBinder.create(uTask))
            }

            val conn = createConn(activity, intent)
            tempConn = conn
            activity.bindService(intent, conn, BIND_AUTO_CREATE)
        }

        private fun createConn(activity: Activity, intent: Intent?): ServiceConnection {
            val conn = object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                    (service as DownloadBinder?)?.getService()?.apply {
                        connection = tempConn
                        setBindActivity(activity)
                        onStartCommand(intent, 0, 0)
                    }
                }

                override fun onServiceDisconnected(name: ComponentName?) {
                    tempConn = null
                    isAlive = false
                }
            }

            return conn
        }

    }

    private val binder by lazy {
        DownloadBinder()
    }

    private var bindActivity: WeakReference<Activity>? = null
    var connection: ServiceConnection? = null

    override fun onBind(p0: Intent?): IBinder {
        return binder
    }

    private inner class DownloadBinder : Binder() {
        fun getService(): UDownloadService {
            return this@UDownloadService
        }
    }

    fun setBindActivity(activity: Activity) {
        bindActivity = WeakReference(activity)
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
            Action.ADD.value -> {
                uTask?.apply {
                    val uInternalTask =
                        UInternalTask(this, downloadFinish = { uInternalTask, isSuccess ->
                            ULog.d("service download finish $uInternalTask , $isSuccess")
                            uInternalTask.notificationId?.apply {
                                if (isSuccess) {
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

        }
        return START_NOT_STICKY
    }

    private fun removeNotification(id: Int) {
        val lastExistNotification = NotificationModule.remove(id)
        NotificationModule.removeNotification(id)
        if (lastExistNotification == null) {
            connection?.apply {
                try {
                    bindActivity?.get()?.unbindService(this)
                } catch (e: Exception) {

                }
            }
            isAlive = false
        }
        lastExistNotification?.apply {
            startForeground(this.id, this.notification)
        }
    }

    private fun releaseDownload() {
        DownloadManager.instance.releaseAll()
    }

    private val wakeLock: PowerManager.WakeLock by lazy {
        (getSystemService(Context.POWER_SERVICE) as PowerManager).run {
            newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyApp::UDownloadWakeLock")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isAlive = false
        wakeLock.release()
        releaseDownload()
    }
}

enum class Action(val value: Int) {
    NONE(0), ADD(1)
}
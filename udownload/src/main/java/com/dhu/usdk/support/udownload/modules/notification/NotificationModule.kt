package com.dhu.usdk.support.udownload.modules

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.dhu.usdk.support.udownload.R
import com.dhu.usdk.support.udownload.State
import com.dhu.usdk.support.udownload.UDownloadService
import com.dhu.usdk.support.udownload.modules.download.DownloadTaskLifecycle
import com.dhu.usdk.support.udownload.modules.notification.ButtonClickReceive
import com.dhu.usdk.support.udownload.utils.ULog
import com.dhu.usdk.support.udownload.utils.application
import com.dhu.usdk.support.udownload.utils.switchUiThreadIfNeeded


object NotificationModule {
    const val DEFAULT_ID = 3000
    const val RESULT_ID = 10000
    private var downloadId = DEFAULT_ID

    private var builder: NotificationCompat.Builder? = null
    private var notificationManager: NotificationManager? = null

    private val downloadNotificationIds = mutableListOf<ForegroundNotification>()
    private fun getContentView(context: Context): RemoteViews {
        return RemoteViews(context.packageName, R.layout.udownload_progress_layout)
    }


    private fun getPauseActionIntent(notificationId: Int): PendingIntent {
        return PendingIntent.getBroadcast(
            application, 0,
            Intent(application, ButtonClickReceive::class.java).apply {
                putExtra(ButtonClickReceive.KEY_ACTION, ButtonClickReceive.PAUSE)
                putExtra(ButtonClickReceive.NOTIFICATION_ID, notificationId)
            }, PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    /**
     * 删除下载的 id ，返回所有 id 是否为空
     */
    @Synchronized
    fun remove(id: Int): ForegroundNotification? {
        downloadNotificationIds.remove(downloadNotificationIds.find { it.id == id })
        if (downloadNotificationIds.isNotEmpty()) {
            return downloadNotificationIds[0]
        }
        return null
    }

    private fun createBuilder(context: Context): NotificationCompat.Builder {
        notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?

        // 唯一的通知通道的id.
        val notificationChannelId = "udownload_channel_id"

        // Android8.0以上的系统，新建消息通道
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            //用户可见的通道名称
            val channelName = "Foreground Service Notification"
            //通道的重要程度
            val importance = NotificationManager.IMPORTANCE_HIGH
            val notificationChannel =
                NotificationChannel(notificationChannelId, channelName, importance)
            notificationChannel.description = "download progress"
            //震动notificationManager
            notificationManager?.createNotificationChannel(notificationChannel)
        }

        val builder: NotificationCompat.Builder =
            NotificationCompat.Builder(context, notificationChannelId)
        //通知小图标
        builder.setSmallIcon(context.applicationInfo.icon)
        //设定通知显示的时间
        val activityIntent =
            context.packageManager.getLaunchIntentForPackage(context.packageName)
        val pendingIntent =
            PendingIntent.getActivity(
                context, 1, activityIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )
        builder.setContentIntent(pendingIntent)
        return builder
    }

    fun createNotification(context: Context): Notification {
        initBuildIfNeeded(context)
        //创建通知并返回
        return builder!!.build()
    }

    @Synchronized
    fun updateProgress(context: Context, id: Int, progress: Int, content: String) {
        switchUiThreadIfNeeded {
            ULog.d("update Progress isAlive ${UDownloadService.isAlive} , id $id, progress $progress, content $content")
            if (!UDownloadService.isAlive) {
                return@switchUiThreadIfNeeded
            }
            initBuildIfNeeded(context)
            builder?.setSilent(true)
            builder?.setContent(getContentView(context).apply {
                setProgressBar(R.id.pb_progress, 100, progress, false)
                setTextViewText(
                    R.id.tv_title,
                    context.getString(R.string.udownload_key_downloading)
                )
                setTextViewText(R.id.tv_content, content)

                setTextViewText(
                    R.id.tv_pause,
                    if (DownloadTaskLifecycle.instance.findTaskByNotificationId(id)?.uTask?.state == State.ON_PAUSE) context.getString(
                        R.string.udownload_resume
                    ) else context.getString(R.string.udownload_pause)
                )

                setOnClickPendingIntent(
                    R.id.tv_pause, getPauseActionIntent(id)
                )
                setViewVisibility(R.id.tv_pause, View.VISIBLE)

            })
            val notification = builder?.build()
            val cacheNotification = downloadNotificationIds.find { it.id == id }
            if (cacheNotification != null) {
                cacheNotification.notification = notification ?: cacheNotification.notification
            }
            notificationManager?.notify(id, notification)
        }
    }

    fun showSuccessNotification(context: Context, id: Int) {
        switchUiThreadIfNeeded {
            initBuildIfNeeded(context)
            builder?.setSilent(false)
            builder?.setContent(getContentView(context).apply {
                setViewVisibility(R.id.pb_progress, View.INVISIBLE)
                setTextViewText(
                    R.id.tv_title,
                    context.getString(R.string.udownload_key_download_success)
                )
                setViewVisibility(R.id.tv_pause, View.INVISIBLE)

            })
            builder?.setAutoCancel(true)
            notificationManager?.notify(RESULT_ID + (id - DEFAULT_ID), builder?.build())
        }
    }

    fun showFailedNotification(context: Context, id: Int) {
        switchUiThreadIfNeeded {
            ULog.d("showFailedNotification $id")
            initBuildIfNeeded(context)
            builder?.setSilent(false)
            builder?.setContent(getContentView(context).apply {
                setViewVisibility(R.id.pb_progress, View.INVISIBLE)
                setTextViewText(
                    R.id.tv_title,
                    context.getString(R.string.udownload_key_download_failed)
                )
                setTextViewText(R.id.tv_content, "")

                setViewVisibility(R.id.tv_pause, View.INVISIBLE)

            })
            builder?.setAutoCancel(true)
            notificationManager?.notify(RESULT_ID + (id - DEFAULT_ID), builder?.build())
        }
    }

    fun removeNotification(id: Int) {
        notificationManager?.cancel(id)
    }

    private fun initBuildIfNeeded(context: Context) {
        if (builder == null) {
            builder = createBuilder(context)
        }
        //通知标题
        builder?.setOngoing(true)
        builder?.setSilent(true)
        builder?.setContent(getContentView(context))
    }

    fun showForegroundService(service: Service, notification: Notification): Int {
        val id = getNotificationId()
        downloadNotificationIds.add(ForegroundNotification(id, notification))
        service.startForeground(id, notification)
        return id
    }

    @Synchronized
    fun getNotificationId(): Int {
        return ++downloadId
    }
}

data class ForegroundNotification(val id: Int, var notification: Notification)
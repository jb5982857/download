package com.dhu.usdk.support.udownload.modules

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat


object NotificationModule {
    const val DEFAULT_ID = 3000
    const val RESULT_ID = 10000
    private var downloadId = DEFAULT_ID

    private var builder: NotificationCompat.Builder? = null
    private var notificationManager: NotificationManager? = null

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
        //通知标题
        builder.setOngoing(true)
        builder.setSilent(true)
        //通知内容
        builder.setContentText("准备中，请稍等")
            .setProgress(100, 0, false)
        //设定通知显示的时间
        val activityIntent =
            context.packageManager.getLaunchIntentForPackage(context.packageName)
        val pendingIntent =
            PendingIntent.getActivity(context, 1, activityIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        builder.setContentIntent(pendingIntent)
        return builder
    }

    fun createNotification(context: Context): Notification {
        if (builder == null) {
            builder = createBuilder(context)
        }
        //创建通知并返回
        return builder!!.build()
    }

    fun updateProgress(context: Context, id: Int, progress: Int, content: String) {
        if (builder == null) {
            builder = createBuilder(context)
        }
        builder?.setSilent(true)
        builder?.setContentTitle("下载中")
        builder?.setProgress(100, progress, false)
        builder?.setContentText(content)
        notificationManager?.notify(id, builder?.build())
    }

    fun showSuccessNotification(context: Context, id: Int) {
        if (builder == null) {
            builder = createBuilder(context)
        }
        builder?.setSilent(false)
        builder?.setContentTitle("下载完成")
        builder?.setProgress(100, 100, false)
        builder?.setContentText("")
        builder?.setAutoCancel(true)
        notificationManager?.notify(RESULT_ID + (id - DEFAULT_ID), builder?.build())
    }

    fun showFailedNotification(context: Context, id: Int) {
        if (builder == null) {
            builder = createBuilder(context)
        }
        builder?.setSilent(false)
        builder?.setContentTitle("下载失败")
        builder?.setProgress(100, 0, false)
        builder?.setContentText("")
        builder?.setAutoCancel(true)
        notificationManager?.notify(RESULT_ID + (id - DEFAULT_ID), builder?.build())
    }

    @Synchronized
    fun getNotificationId(): Int {
        return ++downloadId
    }
}
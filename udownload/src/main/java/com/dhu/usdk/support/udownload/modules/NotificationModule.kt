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
    private var id = DEFAULT_ID
    fun createNotification(context: Context): Notification {
        val notificationManager =
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
        builder.setContentTitle("下载中")
        //通知内容
        builder.setContentText("下载进度")
        //设定通知显示的时间
        val activityIntent =
            context.packageManager.getLaunchIntentForPackage(context.packageName)
        val pendingIntent =
            PendingIntent.getActivity(context, 1, activityIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        builder.setContentIntent(pendingIntent)

        //创建通知并返回
        return builder.build()
    }

    @Synchronized
    fun getNotificationId(): Int {
        return ++id
    }
}
package com.udacity.util

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.udacity.DetailActivity
import com.udacity.DetailActivity.Companion.bundleExtrasOf
import com.udacity.R
import com.udacity.download.DownloadStatus

private const val DOWNLOAD_COMPLETED_ID = 1
private const val NOTIFICATION_REQUEST_CODE = 1

/**
 * Builds and delivers the notification.
 *
 * @param context activity context.
 */
fun NotificationManager.sendDownloadCompletedNotification(
    fileName: String,
    downloadStatus: DownloadStatus,
    context: Context
) {
    val contentIntent = createContentIntent(context, fileName, downloadStatus)
    val contentPendingIntent = createContentPendingIntent(context, contentIntent)

    val notificationBuilder = NotificationCompat.Builder(
        context,
        context.getString(R.string.notification_channel_id)
    ).apply {
        setSmallIcon(R.drawable.ic_assistant_black_24dp)
        setContentTitle(context.getString(R.string.notification_title))
        setContentText(context.getString(R.string.notification_description))
        setPriority(NotificationCompat.PRIORITY_DEFAULT)
        setContentIntent(contentPendingIntent)
        setAutoCancel(true)
        addAction(createCheckStatusAction(context, contentPendingIntent))
        setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
    }

    notify(DOWNLOAD_COMPLETED_ID, notificationBuilder.build())
}

private fun createContentIntent(context: Context, fileName: String, downloadStatus: DownloadStatus): Intent {
    return Intent(context, DetailActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        putExtras(bundleExtrasOf(fileName, downloadStatus))
    }
}

private fun createContentPendingIntent(context: Context, contentIntent: Intent): PendingIntent {
    return PendingIntent.getActivity(
        context,
        NOTIFICATION_REQUEST_CODE,
        contentIntent,
        PendingIntent.FLAG_IMMUTABLE
    )
}

private fun createCheckStatusAction(context: Context, contentPendingIntent: PendingIntent): NotificationCompat.Action {
    return NotificationCompat.Action.Builder(
        null,
        context.getString(R.string.notification_action_check_status),
        contentPendingIntent
    ).build()
}

/**
 * Because you must create the notification channel before posting any notifications on
 * Android 8.0 and higher, you should execute this code as soon as your app starts.
 * It's safe to call this repeatedly because creating an existing notification channel
 * performs no operation.
 *
 * **See also:** [Create a channel and set the importance](https://developer.android.com/training/notify-user/build-notification#Priority)
 */
@SuppressLint("NewApi")
fun NotificationManager.createDownloadStatusChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            context.getString(R.string.notification_channel_id),
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.notification_channel_description)
            setShowBadge(true)
        }
        createNotificationChannel(channel)
    }
}

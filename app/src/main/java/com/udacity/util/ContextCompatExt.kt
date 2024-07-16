package com.udacity.util

import android.app.DownloadManager
import android.app.NotificationManager
import android.content.Context
import androidx.core.content.ContextCompat

fun Context.getNotificationManager(): NotificationManager =
    requireNotNull(ContextCompat.getSystemService(this, NotificationManager::class.java)) {
        "NotificationManager service not found"
    }

fun Context.getDownloadManager(): DownloadManager =
    requireNotNull(ContextCompat.getSystemService(this, DownloadManager::class.java)) {
        "DownloadManager service not found"
    }
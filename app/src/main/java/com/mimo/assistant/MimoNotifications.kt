package com.mimo.assistant

import android.app.Notification
import android.content.Context

object MimoNotifications {
    fun readyNotification(context: Context): Notification = Notification.Builder(context, MimoOverlayService.CHANNEL_ID)
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle("Mimo is ready")
        .setContentText("The Mimo screen bubble is active.")
        .setOngoing(true)
        .build()
}

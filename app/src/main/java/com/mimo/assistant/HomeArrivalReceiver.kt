package com.mimo.assistant

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent

class HomeArrivalReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val event = GeofencingEvent.fromIntent(intent) ?: return
        if (event.hasError() || event.geofenceTransition != Geofence.GEOFENCE_TRANSITION_ENTER) return

        val reminder = context.getSharedPreferences(HomeAutomationManager.PREFERENCES, Context.MODE_PRIVATE)
            .getString(HomeAutomationManager.REMINDER_KEY, null)
            ?: return
        MimoMediaSpeaker(context).say("You arrived home. Reminder: $reminder")

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Mimo reminders", NotificationManager.IMPORTANCE_DEFAULT)
        )
        manager.notify(
            NOTIFICATION_ID,
            android.app.Notification.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Mimo reminder")
                .setContentText(reminder)
                .setAutoCancel(true)
                .build()
        )
    }

    private companion object {
        const val CHANNEL_ID = "mimo_reminders"
        const val NOTIFICATION_ID = 8101
    }
}

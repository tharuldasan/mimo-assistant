package com.mimo.assistant

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class MissedCallNotificationService : NotificationListenerService() {
    private lateinit var speaker: MimoMediaSpeaker

    override fun onCreate() {
        super.onCreate()
        speaker = MimoMediaSpeaker(this)
    }

    override fun onDestroy() {
        speaker.close()
        super.onDestroy()
    }

    override fun onNotificationPosted(notification: StatusBarNotification) {
        if (notification.notification.category != Notification.CATEGORY_MISSED_CALL) return

        val caller = notification.notification.extras
            .getCharSequence(Notification.EXTRA_TITLE)
            ?.toString()
            ?.takeIf(String::isNotBlank)
        val message = if (caller == null) {
            "Mimo noticed a missed call."
        } else {
            "Mimo noticed a missed call from $caller."
        }
        speaker.say(message)
    }
}

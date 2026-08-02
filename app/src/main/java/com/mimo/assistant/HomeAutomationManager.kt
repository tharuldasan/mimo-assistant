package com.mimo.assistant

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource

class HomeAutomationManager(private val context: Context) {
    private val geofencingClient = LocationServices.getGeofencingClient(context)
    private val locationClient = LocationServices.getFusedLocationProviderClient(context)

    fun setHomeArrivalReminder(reminder: String, onResult: (String) -> Unit) {
        val tokenSource = CancellationTokenSource()
        locationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, tokenSource.token)
            .addOnSuccessListener { location ->
                if (location == null) {
                    onResult("Mimo could not find your location. Turn on Location and try again.")
                    return@addOnSuccessListener
                }

                val geofence = Geofence.Builder()
                    .setRequestId(HOME_GEOFENCE_ID)
                    .setCircularRegion(location.latitude, location.longitude, HOME_RADIUS_METERS)
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                    .setExpirationDuration(Geofence.NEVER_EXPIRE)
                    .setNotificationResponsiveness(60_000)
                    .build()
                val request = GeofencingRequest.Builder()
                    .setInitialTrigger(0)
                    .addGeofence(geofence)
                    .build()

                context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
                    .edit()
                    .putString(REMINDER_KEY, reminder)
                    .apply()

                geofencingClient.addGeofences(request, geofencePendingIntent())
                    .addOnSuccessListener { onResult("Home reminder saved: $reminder") }
                    .addOnFailureListener { onResult("Mimo could not save the home reminder: ${it.message}") }
            }
            .addOnFailureListener { onResult("Mimo could not read your location: ${it.message}") }
    }

    private fun geofencePendingIntent(): PendingIntent {
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_MUTABLE
        } else {
            0
        }
        return PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, HomeArrivalReceiver::class.java),
            flags
        )
    }

    companion object {
        const val PREFERENCES = "mimo_automations"
        const val REMINDER_KEY = "home_reminder"
        const val HOME_GEOFENCE_ID = "mimo_home"
        private const val HOME_RADIUS_METERS = 150f
    }
}

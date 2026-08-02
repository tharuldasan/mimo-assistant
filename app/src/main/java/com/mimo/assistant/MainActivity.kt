package com.mimo.assistant

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import java.util.Locale

class MainActivity : Activity() {
    private lateinit var voiceAssistant: MimoVoiceAssistant
    private lateinit var statusText: TextView
    private lateinit var transcriptText: TextView
    private lateinit var replyText: TextView
    private lateinit var homeReminderInput: EditText
    private lateinit var homeAutomationManager: HomeAutomationManager
    private var selectedLanguage = SpokenLanguage.ENGLISH
    private var lastAutomationTranscript = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(createContent())
        homeAutomationManager = HomeAutomationManager(this)
        voiceAssistant = MimoVoiceAssistant(this, MimoTones()) { state -> render(state) }
    }

    override fun onDestroy() {
        voiceAssistant.close()
        super.onDestroy()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == RECORD_AUDIO_REQUEST && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            voiceAssistant.startListening(selectedLanguage)
        } else if (requestCode == RECORD_AUDIO_REQUEST) {
            render(VoiceUiState(status = "Microphone permission is needed to talk to Mimo."))
        } else if (requestCode == HOME_LOCATION_REQUEST && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            saveHomeReminder()
        } else if (requestCode == HOME_LOCATION_REQUEST) {
            render(VoiceUiState(status = "Location permission is needed for home reminders."))
        }
    }

    private fun createContent(): LinearLayout {
        val padding = (24 * resources.displayMetrics.density).toInt()
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(padding, padding, padding, padding)
            setBackgroundColor(Color.rgb(244, 255, 252))

            addView(title("Mimo", 34))
            addView(body("Your bilingual voice companion", 18))
            addView(body("All feedback uses media volume.", 14))

            addView(languageButton(SpokenLanguage.ENGLISH))
            addView(languageButton(SpokenLanguage.SINHALA))
            addView(actionButton("Talk to Mimo") { beginListening() })
            addView(actionButton("Speak latest reply") { voiceAssistant.speakLatestReply() })
            addView(actionButton("Enable Mimo screen bubble") { enableOverlay() })
            addView(actionButton("Disable Mimo screen bubble") { stopOverlay() })
            addView(actionButton("Enable missed-call alerts") { enableMissedCallAlerts() })
            homeReminderInput = EditText(this@MainActivity).apply {
                hint = "Home reminder, e.g. Call Mum"
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = 12 }
            }
            addView(homeReminderInput)
            addView(actionButton("Save home arrival reminder") { saveHomeReminder() })

            statusText = body("Ready", 16)
            transcriptText = body("You said: —", 16)
            replyText = body("Mimo: Press Talk to Mimo, then speak.", 18)
            addView(statusText)
            addView(transcriptText)
            addView(replyText)
        }
    }

    private fun beginListening() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            voiceAssistant.startListening(selectedLanguage)
        } else {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), RECORD_AUDIO_REQUEST)
        }
    }

    private fun languageButton(language: SpokenLanguage): Button = actionButton(language.label) {
        selectedLanguage = language
        render(VoiceUiState(status = "${language.label} selected", reply = "Press Talk to Mimo, then speak."))
    }

    private fun enableOverlay() {
        if (!Settings.canDrawOverlays(this)) {
            startActivity(
                android.content.Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
            )
            return
        }
        startOverlay(MimoOverlayService.ACTION_SHOW_MESSAGE, "Mimo is ready. Open Mimo to talk.")
    }

    private fun stopOverlay() {
        stopService(android.content.Intent(this, MimoOverlayService::class.java))
    }

    private fun enableMissedCallAlerts() {
        startActivity(android.content.Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
    }

    private fun startOverlay(action: String, message: String) {
        val intent = android.content.Intent(this, MimoOverlayService::class.java).apply {
            this.action = action
            putExtra(MimoOverlayService.EXTRA_MESSAGE, message)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun saveHomeReminder() {
        val reminder = homeReminderInput.text.toString().trim()
        if (reminder.isBlank()) {
            render(VoiceUiState(status = "Write a reminder first, for example: Call Mum."))
            return
        }
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), HOME_LOCATION_REQUEST)
            return
        }
        homeAutomationManager.setHomeArrivalReminder(reminder) { status ->
            render(VoiceUiState(status = status, reply = "Mimo: $status"))
        }
    }

    private fun actionButton(label: String, action: () -> Unit): Button = Button(this).apply {
        text = label
        setOnClickListener { action() }
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = 12 }
    }

    private fun title(text: String, size: Int): TextView = TextView(this).apply {
        this.text = text
        textSize = size.toFloat()
        setTextColor(Color.rgb(25, 50, 45))
        gravity = Gravity.CENTER
    }

    private fun body(text: String, size: Int): TextView = TextView(this).apply {
        this.text = text
        textSize = size.toFloat()
        setTextColor(Color.rgb(25, 50, 45))
        setPadding(0, 16, 0, 0)
    }

    private fun render(state: VoiceUiState) {
        runOnUiThread {
            statusText.text = state.status
            transcriptText.text = "You said: ${state.transcript.ifBlank { "—" }}"
            replyText.text = "Mimo: ${state.reply}"
            offerHomeAutomation(state)
            if (
                state.status == "Ready" &&
                state.reply.isNotBlank() &&
                !state.reply.startsWith("Press") &&
                Settings.canDrawOverlays(this)
            ) {
                startOverlay(MimoOverlayService.ACTION_SHOW_MESSAGE, state.reply)
            }
        }
    }

    private fun offerHomeAutomation(state: VoiceUiState) {
        if (state.status != "Ready" || state.transcript == lastAutomationTranscript) return
        val normalizedTranscript = state.transcript.lowercase(Locale.ROOT)
        val trigger = listOf("when i arrive home", "when i get home")
            .firstOrNull { normalizedTranscript.contains(it) }
            ?: return
        val reminderStart = normalizedTranscript.indexOf(trigger) + trigger.length
        val reminder = state.transcript.substring(reminderStart).trim(' ', ',', '.', ':')
        if (reminder.isBlank()) return

        lastAutomationTranscript = state.transcript
        AlertDialog.Builder(this)
            .setTitle("Create home reminder?")
            .setMessage("When you arrive home, Mimo will remind you: $reminder")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Create") { _, _ ->
                homeReminderInput.setText(reminder)
                saveHomeReminder()
            }
            .show()
    }

    private companion object {
        const val RECORD_AUDIO_REQUEST = 100
        const val HOME_LOCATION_REQUEST = 101
    }
}

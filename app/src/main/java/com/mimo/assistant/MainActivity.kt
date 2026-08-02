package com.mimo.assistant

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    private lateinit var voiceAssistant: MimoVoiceAssistant
    private lateinit var statusText: TextView
    private lateinit var transcriptText: TextView
    private lateinit var replyText: TextView
    private var selectedLanguage = SpokenLanguage.ENGLISH

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(createContent())
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
        }
    }

    private companion object {
        const val RECORD_AUDIO_REQUEST = 100
    }
}

package com.mimo.assistant

import android.content.Context
import android.media.AudioAttributes
import android.speech.tts.TextToSpeech
import java.util.Locale

class MimoMediaSpeaker(context: Context) : TextToSpeech.OnInitListener {
    private val textToSpeech = TextToSpeech(context, this)
    private var ready = false
    private var pendingText: String? = null

    override fun onInit(status: Int) {
        ready = status == TextToSpeech.SUCCESS
        if (ready) {
            textToSpeech.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            MimoVoiceProfile.apply(textToSpeech, Locale.US)
            pendingText?.let(::say)
            pendingText = null
        }
    }

    fun say(text: String) {
        if (!ready) {
            pendingText = text
            return
        }
        val language = if (text.any { it in '\u0D80'..'\u0DFF' }) Locale("si", "LK") else Locale.US
        textToSpeech.language = language
        MimoVoiceProfile.apply(textToSpeech, language)
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, "mimo-alert")
    }

    fun close() = textToSpeech.shutdown()
}

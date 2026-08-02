package com.mimo.assistant

import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import java.util.Locale

object MimoVoiceProfile {
    fun apply(textToSpeech: TextToSpeech, locale: Locale) {
        val preferredVoice = textToSpeech.voices
            .orEmpty()
            .filter { it.locale.language == locale.language }
            .sortedWith(
                compareBy<Voice> { !it.name.contains("female", ignoreCase = true) }
                    .thenBy { it.isNetworkConnectionRequired }
            )
            .firstOrNull()
        if (preferredVoice != null) textToSpeech.voice = preferredVoice
        textToSpeech.setPitch(1.18f)
        textToSpeech.setSpeechRate(0.92f)
    }
}

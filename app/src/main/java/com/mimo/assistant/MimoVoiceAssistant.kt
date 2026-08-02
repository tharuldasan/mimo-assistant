package com.mimo.assistant

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import java.util.Locale

enum class SpokenLanguage(val label: String, val locale: Locale) {
    ENGLISH("English", Locale("en", "LK")),
    SINHALA("සිංහල", Locale("si", "LK"))
}

data class VoiceUiState(
    val status: String = "Ready",
    val transcript: String = "",
    val reply: String = "Press Talk to Mimo, then speak."
)

class MimoVoiceAssistant(
    private val context: Context,
    private val tones: MimoTones,
    private val onStateChanged: (VoiceUiState) -> Unit
) : RecognitionListener, TextToSpeech.OnInitListener {
    private val replyEngine = AssistantReplyEngine()
    private var state = VoiceUiState()
    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var currentLanguage = SpokenLanguage.ENGLISH

    init {
        textToSpeech = TextToSpeech(context, this)
    }

    fun startListening(language: SpokenLanguage) {
        currentLanguage = language
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            updateState(status = "Speech recognition is unavailable on this phone.")
            tones.error()
            return
        }

        speechRecognizer?.destroy()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).also {
            it.setRecognitionListener(this)
        }

        tones.wakeDetected()
        updateState(status = "Listening in ${language.label}…", transcript = "")
        tones.listeningStarted()

        val recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, language.locale.toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        speechRecognizer?.startListening(recognizerIntent)
    }

    fun speakLatestReply() {
        val latestReply = state.reply
        if (latestReply.isNotBlank()) speak(latestReply)
    }

    fun close() {
        speechRecognizer?.destroy()
        textToSpeech?.shutdown()
        tones.release()
    }

    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) {
            updateState(status = "Text-to-speech is unavailable on this phone.")
            return
        }
        textToSpeech?.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
        )
    }

    override fun onReadyForSpeech(params: Bundle?) = Unit

    override fun onBeginningOfSpeech() = updateState(status = "I am listening…")

    override fun onRmsChanged(rmsdB: Float) = Unit

    override fun onBufferReceived(buffer: ByteArray?) = Unit

    override fun onEndOfSpeech() {
        updateState(status = "Thinking…")
        tones.thinking()
    }

    override fun onError(error: Int) {
        updateState(status = "I could not understand that. Please try again.")
        tones.error()
    }

    override fun onResults(results: Bundle?) {
        val transcript = results
            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            ?.firstOrNull()
            .orEmpty()
        val reply = replyEngine.replyTo(transcript)
        state = VoiceUiState(status = "Ready", transcript = transcript, reply = reply)
        onStateChanged(state)
        tones.responseReady()
    }

    override fun onPartialResults(partialResults: Bundle?) {
        val partialTranscript = partialResults
            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            ?.firstOrNull()
            .orEmpty()
        updateState(status = "Listening…", transcript = partialTranscript)
    }

    override fun onEvent(eventType: Int, params: Bundle?) = Unit

    private fun speak(text: String) {
        val language = if (text.any { it in '\u0D80'..'\u0DFF' }) {
            SpokenLanguage.SINHALA
        } else {
            currentLanguage
        }
        val result = textToSpeech?.setLanguage(language.locale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            updateState(status = "Install a ${language.label} voice in Android settings to hear this aloud.")
            tones.error()
            return
        }
        textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "mimo-reply")
    }

    private fun updateState(
        status: String = state.status,
        transcript: String = state.transcript,
        reply: String = state.reply
    ) {
        state = VoiceUiState(status, transcript, reply)
        onStateChanged(state)
    }
}

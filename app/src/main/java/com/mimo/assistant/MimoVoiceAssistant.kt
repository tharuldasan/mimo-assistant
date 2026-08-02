package com.mimo.assistant

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.os.Handler
import android.os.Bundle
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import java.util.Locale

enum class SpokenLanguage(val label: String, val locale: Locale) {
    ENGLISH("English", Locale.US),
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
    private val cloudMimoClient = CloudMimoClient()
    private val mainHandler = Handler(Looper.getMainLooper())
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
        textToSpeech?.setPitch(1.15f)
        textToSpeech?.setSpeechRate(0.95f)
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
        updateState(status = recognitionErrorMessage(error))
        tones.error()
    }

    override fun onResults(results: Bundle?) {
        val transcript = results
            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            ?.firstOrNull()
            .orEmpty()
        if (transcript.isBlank()) {
            updateState(status = "I did not hear clear speech. Please try again.")
            tones.error()
            return
        }

        updateState(status = "Mimo is thinking…", transcript = transcript)
        cloudMimoClient.ask(transcript, currentLanguage) { result ->
            mainHandler.post {
                val reply = result.getOrElse {
                    "Mimo cloud is not connected. Add the Worker URL in GitHub Actions variables, then build again."
                }
                state = VoiceUiState(status = "Ready", transcript = transcript, reply = reply)
                onStateChanged(state)
                tones.responseReady()
            }
        }
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

    private fun recognitionErrorMessage(error: Int): String = when (error) {
        SpeechRecognizer.ERROR_NETWORK,
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Speech recognition needs an internet connection. Turn on mobile data or Wi-Fi."
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Allow Mimo to use the microphone in phone settings."
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech recognition is busy. Wait a moment, then try again."
        SpeechRecognizer.ERROR_NO_MATCH,
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "I did not hear clear speech. Speak after the listening tone."
        SpeechRecognizer.ERROR_SERVER -> "Google speech recognition is unavailable right now. Try again shortly."
        else -> "Speech recognition could not start. Update Google and Speech Services by Google, then try again."
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

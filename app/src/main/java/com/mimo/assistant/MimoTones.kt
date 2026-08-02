package com.mimo.assistant

import android.media.AudioManager
import android.media.ToneGenerator

class MimoTones {
    private val toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 45)

    fun wakeDetected() = play(ToneGenerator.TONE_PROP_BEEP2, 80)

    fun listeningStarted() = play(ToneGenerator.TONE_PROP_ACK, 100)

    fun thinking() = play(ToneGenerator.TONE_PROP_PROMPT, 90)

    fun responseReady() = play(ToneGenerator.TONE_PROP_BEEP2, 140)

    fun error() = play(ToneGenerator.TONE_PROP_NACK, 110)

    fun release() = toneGenerator.release()

    private fun play(tone: Int, durationMillis: Int) {
        toneGenerator.startTone(tone, durationMillis)
    }
}

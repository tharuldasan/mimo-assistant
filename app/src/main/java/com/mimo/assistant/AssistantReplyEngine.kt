package com.mimo.assistant

class AssistantReplyEngine {
    fun replyTo(transcript: String): String {
        val cleanedTranscript = transcript.trim()

        return when {
            cleanedTranscript.isBlank() -> "I did not hear anything. Please try again."
            cleanedTranscript.contains("hello", ignoreCase = true) || cleanedTranscript.contains("hi", ignoreCase = true) ->
                "Hi! I am Mimo. How can I help?"
            cleanedTranscript.contains("හලෝ") || cleanedTranscript.contains("ආයුබෝවන්") ->
                "ආයුබෝවන්! මම මීමෝ. මට ඔබට උදව් කරන්න පුළුවන්."
            cleanedTranscript.contains("remind", ignoreCase = true) || cleanedTranscript.contains("මතක්") ->
                "I understood your reminder request. Reminder scheduling is the next feature to add."
            else -> "I heard: $cleanedTranscript"
        }
    }
}

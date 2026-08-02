package com.mimo.assistant

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

class CloudMimoClient {
    private val executor = Executors.newSingleThreadExecutor()

    fun ask(message: String, language: SpokenLanguage, onComplete: (Result<String>) -> Unit) {
        if (BuildConfig.MIMO_BACKEND_URL.isBlank()) {
            onComplete(Result.failure(IllegalStateException("Mimo cloud is not connected yet.")))
            return
        }

        executor.execute {
            try {
                val connection = (URL("${BuildConfig.MIMO_BACKEND_URL}/").openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 15_000
                    readTimeout = 30_000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                    setRequestProperty("Accept", "application/json")
                }
                val requestBody = JSONObject()
                    .put("message", message)
                    .put("language", language.locale.toLanguageTag())
                    .toString()

                connection.outputStream.bufferedWriter(Charsets.UTF_8).use { writer ->
                    writer.write(requestBody)
                }

                val responseBody = (if (connection.responseCode in 200..299) {
                    connection.inputStream
                } else {
                    connection.errorStream
                }).bufferedReader().use { it.readText() }
                val response = JSONObject(responseBody)

                if (connection.responseCode !in 200..299) {
                    throw IllegalStateException(response.optString("error", "Mimo cloud is unavailable."))
                }
                val reply = response.optString("reply").trim()
                check(reply.isNotBlank()) { "Mimo cloud returned an empty reply." }
                onComplete(Result.success(reply))
            } catch (error: Exception) {
                onComplete(Result.failure(error))
            }
        }
    }
}

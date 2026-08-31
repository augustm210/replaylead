package com.replaylead.app.data

import com.replaylead.app.BuildConfig
import com.replaylead.app.domain.CoachingReport
import com.replaylead.app.domain.ConversationTurn
import com.replaylead.app.domain.Scenario
import com.replaylead.app.domain.Speaker
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class RemoteConversationService(
    private val baseUrl: String = BuildConfig.CONVERSATION_API_URL.trim().trimEnd('/'),
) {
    val isConfigured: Boolean = baseUrl.startsWith("https://")

    suspend fun reply(scenario: Scenario, turns: List<ConversationTurn>): String {
        val response = post("/v1/reply", requestBody(scenario, turns))
        return response.getString("reply").trim().also {
            require(it.isNotEmpty() && it.length <= 500) { "Invalid reply" }
        }
    }

    suspend fun coach(scenario: Scenario, turns: List<ConversationTurn>): CoachingReport {
        val response = post("/v1/coach", requestBody(scenario, turns))
        return CoachingReport(
            clarity = response.score("clarity"),
            empathy = response.score("empathy"),
            assertiveness = response.score("assertiveness"),
            actionability = response.score("actionability"),
            strength = response.requiredText("strength", 400),
            improvement = response.requiredText("improvement", 400),
            suggestedResponse = response.requiredText("suggestedResponse", 500),
        )
    }

    private fun requestBody(scenario: Scenario, turns: List<ConversationTurn>): JSONObject = JSONObject().apply {
        put("scenarioId", scenario.id)
        put("turns", JSONArray().apply {
            turns.takeLast(12).forEach { turn ->
                put(JSONObject().apply {
                    put("role", if (turn.speaker == Speaker.USER) "user" else "counterpart")
                    put("text", turn.text.take(800))
                })
            }
        })
    }

    private suspend fun post(path: String, body: JSONObject): JSONObject = withContext(Dispatchers.IO) {
        check(isConfigured) { "Remote conversation service is not configured" }
        val connection = URL("$baseUrl$path").openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 8_000
            connection.readTimeout = 20_000
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            connection.setRequestProperty("Accept", "application/json")
            connection.outputStream.bufferedWriter(Charsets.UTF_8).use { it.write(body.toString()) }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val payload = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            check(status in 200..299) { "Conversation service returned HTTP $status" }
            JSONObject(payload)
        } finally {
            connection.disconnect()
        }
    }
}

private fun JSONObject.score(name: String): Int {
    val value = getInt(name)
    require(value in 0..100) { "Invalid score" }
    return value
}

private fun JSONObject.requiredText(name: String, maxLength: Int): String = getString(name).trim().also {
    require(it.isNotEmpty() && it.length <= maxLength) { "Invalid text" }
}

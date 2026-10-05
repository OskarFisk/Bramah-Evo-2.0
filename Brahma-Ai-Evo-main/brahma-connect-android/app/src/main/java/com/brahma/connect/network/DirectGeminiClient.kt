package com.brahma.connect.network

import com.brahma.connect.commands.DeviceCommandHandler
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

object DirectGeminiClient {
    private const val ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-flash-lite:generateContent"
    private const val MAX_TOOL_ROUNDS = 4
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val httpClient = OkHttpClient.Builder()
        .callTimeout(90, TimeUnit.SECONDS)
        .build()

    private val declarations = JSONArray().apply {
        put(functionDeclaration("get_device_info", "Get Android model and OS details."))
        put(functionDeclaration("get_battery", "Get battery percentage and charging state."))
        put(functionDeclaration("volume_get", "Get current media volume."))
        put(functionDeclaration(
            "volume_set",
            "Set media volume to a percentage from 0 to 100.",
            JSONObject().put("value", property("NUMBER", "Target media volume percentage.")),
            listOf("value"),
        ))
        put(functionDeclaration(
            "launch_app",
            "Open an installed Android app by its visible name.",
            JSONObject().put("app_name", property("STRING", "Visible app name, such as Maps.")),
            listOf("app_name"),
        ))
        put(functionDeclaration(
            "open_url",
            "Open a web address in the user's browser.",
            JSONObject().put("url", property("STRING", "An http or https URL.")),
            listOf("url"),
        ))
    }

    private val toolNames = setOf("get_device_info", "get_battery", "volume_get", "volume_set", "launch_app", "open_url")

    suspend fun reply(
        apiKey: String,
        history: List<Pair<String, String>>,
        commandHandler: DeviceCommandHandler,
    ): String = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) throw IOException("Add your Gemini API key in Settings to start chatting.")

        val contents = JSONArray()
        history.forEach { (role, text) ->
            if (text.isNotBlank()) {
                contents.put(JSONObject()
                    .put("role", if (role == "model") "model" else "user")
                    .put("parts", JSONArray().put(JSONObject().put("text", text))))
            }
        }

        repeat(MAX_TOOL_ROUNDS) {
            val requestJson = JSONObject()
                .put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put(
                    "text",
                    "You are Brahma, a helpful Android assistant. You run directly on the user's phone. " +
                        "Use the provided phone tools only when the user explicitly asks for the action. " +
                        "Never claim an action succeeded unless its tool result confirms success. " +
                        "Explain when a task requires the desktop Brahma app instead.",
                ))))
                .put("contents", contents)
                .put("tools", JSONArray().put(JSONObject().put("functionDeclarations", declarations)))
                .put("generationConfig", JSONObject().put("temperature", 0.4).put("maxOutputTokens", 2048))

            val url = ENDPOINT.toHttpUrl()
            val request = Request.Builder()
                .url(url)
                .header("x-goog-api-key", apiKey)
                .post(requestJson.toString().toRequestBody(jsonMediaType))
                .build()

            val responseText = httpClient.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val message = runCatching {
                        JSONObject(body).optJSONObject("error")?.optString("message")
                    }.getOrNull().orEmpty()
                    throw IOException(message.ifBlank { "Gemini request failed (${response.code}). Check your key and connection." })
                }
                body
            }

            val root = JSONObject(responseText)
            val candidate = root.optJSONArray("candidates")?.optJSONObject(0)
                ?: throw IOException(root.optJSONObject("promptFeedback")?.toString() ?: "Gemini returned no response.")
            val modelContent = candidate.optJSONObject("content") ?: throw IOException("Gemini returned an empty response.")
            val parts = modelContent.optJSONArray("parts") ?: JSONArray()
            val functionPart = (0 until parts.length())
                .mapNotNull { parts.optJSONObject(it) }
                .firstOrNull { it.has("functionCall") }

            if (functionPart == null) {
                return@withContext (0 until parts.length())
                    .mapNotNull { parts.optJSONObject(it)?.optString("text")?.takeIf(String::isNotBlank) }
                    .joinToString("\n")
                    .ifBlank { "I couldn't create a text response. Please try again." }
            }

            val functionCall = functionPart.getJSONObject("functionCall")
            val name = functionCall.optString("name")
            val arguments = functionCall.optJSONObject("args") ?: JSONObject()
            contents.put(modelContent)

            val result = if (name in toolNames) {
                val parameters = buildMap<String, Any?> {
                    val keys = arguments.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        put(key, arguments.opt(key))
                    }
                }
                if (name == "open_url") {
                    val uri = Uri.parse(parameters["url"]?.toString().orEmpty())
                    if (uri.scheme !in setOf("http", "https")) {
                        JSONObject().put("success", false).put("error", "Only http and https links can be opened.")
                    } else {
                        commandHandler.handle(name, parameters).toJson()
                    }
                } else {
                    commandHandler.handle(name, parameters).toJson()
                }
            } else {
                JSONObject().put("success", false).put("error", "This phone action is not available.")
            }
            contents.put(JSONObject()
                .put("role", "user")
                .put("parts", JSONArray().put(JSONObject().put("functionResponse", JSONObject()
                    .put("name", name)
                    .put("response", result)))))
        }

        throw IOException("The assistant reached its phone-action limit for one reply. Ask a shorter request.")
    }

    private fun functionDeclaration(
        name: String,
        description: String,
        properties: JSONObject = JSONObject(),
        required: List<String> = emptyList(),
    ): JSONObject = JSONObject()
        .put("name", name)
        .put("description", description)
        .put("parameters", JSONObject()
            .put("type", "OBJECT")
            .put("properties", properties)
            .put("required", JSONArray(required)))

    private fun property(type: String, description: String): JSONObject = JSONObject()
        .put("type", type)
        .put("description", description)
}

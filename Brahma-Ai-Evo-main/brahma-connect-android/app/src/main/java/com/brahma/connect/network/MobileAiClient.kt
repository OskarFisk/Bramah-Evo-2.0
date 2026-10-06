package com.brahma.connect.network

import android.net.Uri
import com.brahma.connect.commands.DeviceCommandHandler
import com.brahma.connect.core.AiProvider
import com.brahma.connect.core.AiProviderConfig
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

internal fun apiKeyHeaderValue(keyHeader: String, apiKey: String): String =
    if (keyHeader == "Authorization") "Bearer $apiKey" else apiKey

object MobileAiClient {
    private const val MAX_TOOL_ROUNDS = 5
    private const val SYSTEM_PROMPT =
        "You are Brahma, a helpful Android assistant running on the user's phone. " +
            "Only use phone tools when the user asks for an action. Some tools open a draft or chooser " +
            "for the user to review; never claim that an email, text, calendar event, or alarm was " +
            "completed unless the tool confirms it. Never claim a phone action succeeded unless its " +
            "tool result confirms success. Explain when a request requires the Brahma desktop app."

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val httpClient = OkHttpClient.Builder()
        .callTimeout(90, TimeUnit.SECONDS)
        .build()

    private val toolDefinitions = listOf(
        ToolDefinition("get_device_info", "Get Android model, OS, and app version."),
        ToolDefinition("get_battery", "Get battery percentage and charging state."),
        ToolDefinition("volume_get", "Get current media volume."),
        ToolDefinition("volume_set", "Set media volume from 0 to 100.", mapOf("value" to number("Target volume percentage.")), listOf("value")),
        ToolDefinition("launch_app", "Open an installed Android app by name.", mapOf("app_name" to string("Visible app name or package name.")), listOf("app_name")),
        ToolDefinition("list_apps", "List installed launchable apps, optionally filtered by name.", mapOf("query" to string("Optional app-name filter."))),
        ToolDefinition("open_url", "Open a valid HTTP or HTTPS link in the browser.", mapOf("url" to string("An http or https URL.")), listOf("url")),
        ToolDefinition("flashlight_on", "Turn on the phone flashlight."),
        ToolDefinition("flashlight_off", "Turn off the phone flashlight."),
        ToolDefinition("share_text", "Open the Android share sheet for text.", mapOf("text" to string("Text to share.")), listOf("text")),
        ToolDefinition("compose_sms", "Open a text-message draft for the user to review.", mapOf(
            "recipient" to string("Optional phone number."),
            "body" to string("Optional message body."),
        )),
        ToolDefinition("compose_email", "Open an email draft for the user to review.", mapOf(
            "recipient" to string("Optional email address."),
            "subject" to string("Optional email subject."),
            "body" to string("Optional email body."),
        )),
        ToolDefinition("open_map", "Open a place or address in a map app.", mapOf("query" to string("Place name or address.")), listOf("query")),
        ToolDefinition("create_calendar_event", "Open a calendar event draft for the user to review.", mapOf(
            "title" to string("Event title."),
            "description" to string("Optional event description."),
            "location" to string("Optional event location."),
            "begin_time" to number("Optional start time in Unix epoch milliseconds."),
            "end_time" to number("Optional end time in Unix epoch milliseconds."),
        ), listOf("title")),
        ToolDefinition("set_alarm", "Open the clock app with an alarm for the user to confirm.", mapOf(
            "hour" to number("24-hour clock hour from 0 to 23."),
            "minute" to number("Minute from 0 to 59."),
            "label" to string("Optional alarm label."),
        ), listOf("hour", "minute")),
        ToolDefinition("open_settings", "Open a supported Android settings page.", mapOf(
            "section" to string("One of wifi, bluetooth, display, sound, or apps."),
        ), listOf("section")),
    )

    private val toolNames = toolDefinitions.mapTo(mutableSetOf()) { it.name }
    private val geminiTools = JSONArray().put(
        JSONObject().put(
            "functionDeclarations",
            JSONArray().apply { toolDefinitions.forEach { put(it.toGeminiJson()) } },
        ),
    )
    private val openAiTools = JSONArray().apply { toolDefinitions.forEach { put(it.toOpenAiJson()) } }
    private val anthropicTools = JSONArray().apply { toolDefinitions.forEach { put(it.toAnthropicJson()) } }

    suspend fun reply(
        config: AiProviderConfig,
        history: List<Pair<String, String>>,
        commandHandler: DeviceCommandHandler,
    ): String = withContext(Dispatchers.IO) {
        if (config.apiKey.isBlank()) {
            throw IOException("Add an API key for ${config.provider.title} in Settings to start chatting.")
        }
        if (config.model.isBlank()) throw IOException("Choose a model in Settings before sending a message.")

        when (config.provider) {
            AiProvider.GEMINI -> replyWithGemini(config, history, commandHandler)
            AiProvider.ANTHROPIC -> replyWithAnthropic(config, history, commandHandler)
            else ->
                replyWithOpenAiCompatible(config, history, commandHandler)
        }
    }

    private fun replyWithGemini(
        config: AiProviderConfig,
        history: List<Pair<String, String>>,
        commandHandler: DeviceCommandHandler,
    ): String {
        val contents = JSONArray()
        history.forEach { (role, text) ->
            if (text.isNotBlank()) contents.put(
                JSONObject()
                    .put("role", if (role == "model") "model" else "user")
                    .put("parts", JSONArray().put(JSONObject().put("text", text))),
            )
        }

        repeat(MAX_TOOL_ROUNDS) {
            val requestJson = JSONObject()
                .put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", SYSTEM_PROMPT))))
                .put("contents", contents)
                .put("tools", geminiTools)
                .put("generationConfig", JSONObject().put("temperature", 0.4).put("maxOutputTokens", 2048))

            val response = postJson(
                "https://generativelanguage.googleapis.com/v1beta/models/${Uri.encode(config.model)}:generateContent",
                requestJson,
                config.apiKey,
                "x-goog-api-key",
            )
            val candidate = response.optJSONArray("candidates")?.optJSONObject(0)
                ?: throw apiError(response, config.provider, "The model returned no response.")
            val modelContent = candidate.optJSONObject("content")
                ?: throw IOException("${config.provider.title} returned an empty response.")
            val parts = modelContent.optJSONArray("parts") ?: JSONArray()
            val functionCalls = (0 until parts.length())
                .mapNotNull { parts.optJSONObject(it)?.optJSONObject("functionCall") }

            if (functionCalls.isEmpty()) return (0 until parts.length())
                .mapNotNull { parts.optJSONObject(it)?.optString("text")?.takeIf(String::isNotBlank) }
                .joinToString("\n")
                .ifBlank { "I couldn't create a text response. Please try again." }

            contents.put(modelContent)
            val responses = JSONArray()
            functionCalls.forEach { call ->
                val name = call.optString("name")
                responses.put(JSONObject().put("functionResponse", JSONObject()
                    .put("name", name)
                    .put("response", executeTool(name, call.optJSONObject("args") ?: JSONObject(), commandHandler))))
            }
            contents.put(JSONObject().put("role", "user").put("parts", responses))
        }
        throw toolRoundLimitError()
    }

    private fun replyWithOpenAiCompatible(
        config: AiProviderConfig,
        history: List<Pair<String, String>>,
        commandHandler: DeviceCommandHandler,
    ): String {
        val messages = JSONArray()
            .put(JSONObject().put("role", "system").put("content", SYSTEM_PROMPT))
        history.forEach { (role, text) ->
            if (text.isNotBlank()) messages.put(
                JSONObject().put("role", if (role == "model") "assistant" else "user").put("content", text),
            )
        }

        repeat(MAX_TOOL_ROUNDS) {
            val request = JSONObject()
                .put("model", config.model)
                .put("messages", messages)
                .put("tools", openAiTools)
                .put("tool_choice", "auto")
                .put("temperature", 0.4)
                .put("max_tokens", 2048)
            val response = postJson(openAiEndpoint(config.provider), request, config.apiKey)
            val message = response.optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("message")
                ?: throw apiError(response, config.provider, "The model returned no response.")
            val calls = message.optJSONArray("tool_calls")
            if (calls == null || calls.length() == 0) {
                return extractOpenAiText(message.opt("content"))
                    ?: "I couldn't create a text response. Please try again."
            }

            messages.put(message)
            for (index in 0 until calls.length()) {
                val call = calls.optJSONObject(index) ?: continue
                val function = call.optJSONObject("function") ?: continue
                val name = function.optString("name")
                val args = runCatching { JSONObject(function.optString("arguments", "{}")) }
                    .getOrElse { JSONObject() }
                messages.put(JSONObject()
                    .put("role", "tool")
                    .put("tool_call_id", call.optString("id"))
                    .put("content", executeTool(name, args, commandHandler).toString()))
            }
        }
        throw toolRoundLimitError()
    }

    private fun replyWithAnthropic(
        config: AiProviderConfig,
        history: List<Pair<String, String>>,
        commandHandler: DeviceCommandHandler,
    ): String {
        val messages = JSONArray()
        history.forEach { (role, text) ->
            if (text.isNotBlank()) messages.put(
                JSONObject().put("role", if (role == "model") "assistant" else "user").put("content", text),
            )
        }

        repeat(MAX_TOOL_ROUNDS) {
            val request = JSONObject()
                .put("model", config.model)
                .put("system", SYSTEM_PROMPT)
                .put("messages", messages)
                .put("tools", anthropicTools)
                .put("max_tokens", 2048)
                .put("temperature", 0.4)
            val response = postJson(
                "https://api.anthropic.com/v1/messages",
                request,
                config.apiKey,
                "x-api-key",
                mapOf("anthropic-version" to "2023-06-01"),
            )
            val content = response.optJSONArray("content") ?: throw apiError(response, config.provider, "The model returned no response.")
            val calls = (0 until content.length())
                .mapNotNull { content.optJSONObject(it)?.takeIf { block -> block.optString("type") == "tool_use" } }
            if (calls.isEmpty()) {
                return (0 until content.length())
                    .mapNotNull { content.optJSONObject(it)?.takeIf { block -> block.optString("type") == "text" }?.optString("text") }
                    .filter(String::isNotBlank)
                    .joinToString("\n")
                    .ifBlank { "I couldn't create a text response. Please try again." }
            }

            messages.put(JSONObject().put("role", "assistant").put("content", content))
            val results = JSONArray()
            calls.forEach { call ->
                val result = executeTool(call.optString("name"), call.optJSONObject("input") ?: JSONObject(), commandHandler)
                results.put(JSONObject()
                    .put("type", "tool_result")
                    .put("tool_use_id", call.optString("id"))
                    .put("content", result.toString())
                    .put("is_error", result.optBoolean("success").not()))
            }
            messages.put(JSONObject().put("role", "user").put("content", results))
        }
        throw toolRoundLimitError()
    }

    private fun openAiEndpoint(provider: AiProvider): String =
        provider.openAiCompatibleEndpoint ?: error("Provider does not use the OpenAI-compatible API.")

    private fun extractOpenAiText(content: Any?): String? = when (content) {
        is String -> content.takeIf(String::isNotBlank)
        is JSONArray -> (0 until content.length())
            .mapNotNull { content.optJSONObject(it)?.optString("text")?.takeIf(String::isNotBlank) }
            .joinToString("\n")
            .takeIf(String::isNotBlank)
        else -> null
    }

    private fun postJson(
        endpoint: String,
        body: JSONObject,
        apiKey: String,
        keyHeader: String = "Authorization",
        extraHeaders: Map<String, String> = emptyMap(),
    ): JSONObject {
        val requestBuilder = Request.Builder()
            .url(endpoint.toHttpUrl())
            .post(body.toString().toRequestBody(jsonMediaType))
        requestBuilder.header(keyHeader, apiKeyHeaderValue(keyHeader, apiKey))
        extraHeaders.forEach { (name, value) -> requestBuilder.header(name, value) }

        val responseText = httpClient.newCall(requestBuilder.build()).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val detail = runCatching {
                    JSONObject(text).optJSONObject("error")?.optString("message")
                        ?: JSONObject(text).optString("message")
                }.getOrNull().orEmpty()
                throw IOException(detail.ifBlank { "AI request failed (${response.code}). Check your API key, model, and connection." })
            }
            text
        }
        return try {
            JSONObject(responseText)
        } catch (error: Exception) {
            throw IOException("The AI provider returned an invalid response.", error)
        }
    }

    private fun apiError(response: JSONObject, provider: AiProvider, fallback: String): IOException {
        val message = response.optJSONObject("error")?.optString("message").orEmpty()
        return IOException(message.ifBlank { "${provider.title}: $fallback" })
    }

    private fun executeTool(name: String, args: JSONObject, handler: DeviceCommandHandler): JSONObject {
        if (name !in toolNames) {
            return JSONObject().put("success", false).put("error", "This phone action is not available.")
        }
        if (name == "open_url") {
            val uri = Uri.parse(args.optString("url"))
            if (uri.scheme !in setOf("http", "https") || uri.host.isNullOrBlank()) {
                return JSONObject().put("success", false).put("error", "Only valid http and https links can be opened.")
            }
        }
        val parameters = buildMap<String, Any?> {
            val keys = args.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                put(key, args.opt(key))
            }
        }
        return handler.handle(name, parameters).toJson()
    }

    private fun toolRoundLimitError() =
        IOException("The assistant reached its phone-action limit for one reply. Ask a shorter request.")

    private data class ToolDefinition(
        val name: String,
        val description: String,
        val properties: Map<String, JSONObject> = emptyMap(),
        val required: List<String> = emptyList(),
    ) {
        fun toOpenAiJson() = JSONObject()
            .put("type", "function")
            .put("function", JSONObject()
                .put("name", name)
                .put("description", description)
                .put("parameters", parametersJson()))

        fun toGeminiJson() = JSONObject()
            .put("name", name)
            .put("description", description)
            .put("parameters", JSONObject()
                .put("type", "OBJECT")
                .put("properties", JSONObject().apply {
                    properties.forEach { (key, schema) ->
                        put(key, JSONObject(schema.toString()).put("type", schema.optString("type").uppercase()))
                    }
                })
                .put("required", JSONArray(required)))

        fun toAnthropicJson() = JSONObject()
            .put("name", name)
            .put("description", description)
            .put("input_schema", parametersJson())

        private fun parametersJson(objectType: String = "object") = JSONObject()
            .put("type", objectType)
            .put("properties", JSONObject(properties))
            .put("required", JSONArray(required))
    }

    private fun string(description: String) = JSONObject().put("type", "string").put("description", description)
    private fun number(description: String) = JSONObject().put("type", "number").put("description", description)
}

package com.brahma.connect.core

enum class AiProvider(
    val title: String,
    val defaultModel: String,
    val keyLabel: String,
    val openAiCompatibleEndpoint: String? = null,
) {
    GEMINI("Google Gemini", "gemini-2.5-flash", "Gemini API key"),
    OPENAI("OpenAI", "gpt-4o-mini", "OpenAI API key", "https://api.openai.com/v1/chat/completions"),
    ANTHROPIC("Anthropic", "claude-3-5-haiku-latest", "Anthropic API key"),
    OPENROUTER("OpenRouter", "openai/gpt-4o-mini", "OpenRouter API key", "https://openrouter.ai/api/v1/chat/completions"),
    GROQ("Groq", "llama-3.3-70b-versatile", "Groq API key", "https://api.groq.com/openai/v1/chat/completions"),
    DEEPSEEK("DeepSeek", "deepseek-chat", "DeepSeek API key", "https://api.deepseek.com/chat/completions"),
    MISTRAL("Mistral", "mistral-small-latest", "Mistral API key", "https://api.mistral.ai/v1/chat/completions"),
    TOGETHER("Together AI", "meta-llama/Llama-3.3-70B-Instruct-Turbo", "Together AI API key", "https://api.together.xyz/v1/chat/completions"),
    FIREWORKS("Fireworks AI", "accounts/fireworks/models/llama-v3p3-70b-instruct", "Fireworks API key", "https://api.fireworks.ai/inference/v1/chat/completions"),
    XAI("xAI", "grok-3-mini", "xAI API key", "https://api.x.ai/v1/chat/completions"),
    CEREBRAS("Cerebras", "llama-3.3-70b", "Cerebras API key", "https://api.cerebras.ai/v1/chat/completions"),
}

data class AiProviderConfig(
    val provider: AiProvider,
    val model: String,
    val apiKey: String,
)

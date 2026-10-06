"""Provider metadata shared by the desktop settings UI and model client."""

OPENAI_COMPATIBLE_PROVIDERS = {
    "OpenAI": {
        "endpoint": "https://api.openai.com/v1/chat/completions",
        "model": "gpt-4o-mini",
        "key_field": "openai_api_key",
    },
    "OpenRouter": {
        "endpoint": "https://openrouter.ai/api/v1/chat/completions",
        "model": "openai/gpt-4o-mini",
        "key_field": "openrouter_api_key",
    },
    "Groq": {
        "endpoint": "https://api.groq.com/openai/v1/chat/completions",
        "model": "llama-3.3-70b-versatile",
        "key_field": "groq_api_key",
    },
    "DeepSeek": {
        "endpoint": "https://api.deepseek.com/chat/completions",
        "model": "deepseek-chat",
        "key_field": "deepseek_api_key",
    },
    "Mistral": {
        "endpoint": "https://api.mistral.ai/v1/chat/completions",
        "model": "mistral-small-latest",
        "key_field": "mistral_api_key",
    },
    "Together AI": {
        "endpoint": "https://api.together.xyz/v1/chat/completions",
        "model": "meta-llama/Llama-3.3-70B-Instruct-Turbo",
        "key_field": "together_api_key",
    },
    "Fireworks AI": {
        "endpoint": "https://api.fireworks.ai/inference/v1/chat/completions",
        "model": "accounts/fireworks/models/llama-v3p3-70b-instruct",
        "key_field": "fireworks_api_key",
    },
    "xAI": {
        "endpoint": "https://api.x.ai/v1/chat/completions",
        "model": "grok-3-mini",
        "key_field": "xai_api_key",
    },
    "Cerebras": {
        "endpoint": "https://api.cerebras.ai/v1/chat/completions",
        "model": "llama-3.3-70b",
        "key_field": "cerebras_api_key",
    },
}

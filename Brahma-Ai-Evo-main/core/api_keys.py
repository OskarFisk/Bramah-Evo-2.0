"""Helpers for recognizing and storing multiple provider API keys."""

from __future__ import annotations

from typing import Any


_PROVIDERS = {
    "Gemini": ("gemini_api_key", "gemini_api_keys"),
    "OpenAI": ("openai_api_key", "openai_api_keys"),
    "OpenRouter": ("openrouter_api_key", "openrouter_api_keys"),
    "Anthropic": ("anthropic_api_key", "anthropic_api_keys"),
    "Groq": ("groq_api_key", "groq_api_keys"),
    "DeepSeek": ("deepseek_api_key", "deepseek_api_keys"),
    "Mistral": ("mistral_api_key", "mistral_api_keys"),
    "Together AI": ("together_api_key", "together_api_keys"),
    "Fireworks AI": ("fireworks_api_key", "fireworks_api_keys"),
    "xAI": ("xai_api_key", "xai_api_keys"),
    "Cerebras": ("cerebras_api_key", "cerebras_api_keys"),
}


def detect_provider(api_key: str) -> str | None:
    """Return the provider indicated by a recognizable API-key prefix."""
    key = (api_key or "").strip()
    if key.startswith("AIza"):
        return "Gemini"
    if key.startswith("sk-or-"):
        return "OpenRouter"
    if key.startswith("sk-ant-"):
        return "Anthropic"
    if key.startswith("gsk_"):
        return "Groq"
    if key.startswith("sk-proj-"):
        return "OpenAI"
    return None


def get_provider_keys(data: dict[str, Any], provider: str) -> list[str]:
    """Return unique keys with the current legacy key first."""
    fields = _PROVIDERS.get(provider)
    if fields is None:
        return []

    legacy_field, list_field = fields
    values = [data.get(legacy_field, "")]
    stored = data.get(list_field, [])
    if isinstance(stored, list):
        values.extend(stored)

    keys: list[str] = []
    for value in values:
        if isinstance(value, dict):
            value = value.get("key", "")
        if isinstance(value, str):
            key = value.strip()
            if key and key not in keys:
                keys.append(key)
    return keys


def get_active_key(data: dict[str, Any], provider: str) -> str:
    fields = _PROVIDERS.get(provider)
    return str(data.get(fields[0], "") or "").strip() if fields else ""


def add_api_key(data: dict[str, Any], api_key: str, provider: str | None = None) -> str:
    """Save a recognized key and make it active for legacy consumers."""
    key = (api_key or "").strip()
    if not key:
        raise ValueError("API key cannot be empty")
    detected_provider = detect_provider(key)
    if provider is not None and detected_provider is not None and detected_provider != provider:
        raise ValueError(f"API key belongs to {detected_provider}, not {provider}")
    provider = provider or detected_provider
    if provider is None:
        raise ValueError("Unsupported API key format")
    if provider not in _PROVIDERS:
        raise ValueError("Unsupported API provider")

    legacy_field, list_field = _PROVIDERS[provider]
    keys = get_provider_keys(data, provider)
    if key not in keys:
        keys.append(key)
    data[list_field] = keys
    data[legacy_field] = key
    return provider


def set_active_key(data: dict[str, Any], provider: str, api_key: str) -> None:
    """Select a previously saved key while preserving legacy config fields."""
    fields = _PROVIDERS.get(provider)
    key = (api_key or "").strip()
    if fields is None or key not in get_provider_keys(data, provider):
        raise ValueError("The selected API key is not saved for this provider")
    data[fields[0]] = key


def remove_api_key(data: dict[str, Any], provider: str, api_key: str) -> None:
    """Remove a saved key and select the next key if the active one was removed."""
    fields = _PROVIDERS.get(provider)
    key = (api_key or "").strip()
    if fields is None:
        raise ValueError("Unsupported API provider")

    legacy_field, list_field = fields
    keys = [saved for saved in get_provider_keys(data, provider) if saved != key]
    data[list_field] = keys
    data[legacy_field] = keys[0] if keys else ""


def mask_api_key(api_key: str) -> str:
    key = (api_key or "").strip()
    if not key:
        return "Not set"
    if len(key) <= 8:
        return "*" * len(key)
    return f"{key[:4]}{'*' * 8}{key[-4:]}"
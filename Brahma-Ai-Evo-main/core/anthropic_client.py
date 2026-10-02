"""Small requests-based client for Anthropic's Messages API."""

from __future__ import annotations

import json

import requests

from core.user_paths import get_user_data_dir


API_CONFIG_PATH = get_user_data_dir() / "config" / "api_keys.json"
API_URL = "https://api.anthropic.com/v1/messages"
MODEL = "claude-sonnet-4-20250514"


def chat(prompt: str, system: str | None = None) -> str:
    try:
        data = json.loads(API_CONFIG_PATH.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as exc:
        raise PermissionError("Anthropic API key is missing or unreadable.") from exc

    api_key = str(data.get("anthropic_api_key", "")).strip()
    if not api_key:
        raise PermissionError("Anthropic API key is missing. Add one in Settings.")

    payload = {
        "model": MODEL,
        "max_tokens": 1024,
        "messages": [{"role": "user", "content": prompt}],
    }
    if system:
        payload["system"] = system

    response = requests.post(
        API_URL,
        headers={
            "x-api-key": api_key,
            "anthropic-version": "2023-06-01",
            "content-type": "application/json",
        },
        json=payload,
        timeout=60,
    )
    if response.status_code >= 400:
        raise RuntimeError(f"Anthropic request failed ({response.status_code}): {response.text[:500]}")

    blocks = response.json().get("content", [])
    text = "".join(block.get("text", "") for block in blocks if block.get("type") == "text")
    if not text.strip():
        raise RuntimeError("Anthropic returned an empty response.")
    return text.strip()
import json

import pytest
import requests

from llm_client import UnifiedAIClient


class FakeResponse:
    status_code = 200

    def json(self):
        return {"choices": [{"message": {"content": " provider response "}}]}


def test_openai_compatible_provider_uses_saved_key_model_and_bearer_header(tmp_path, monkeypatch):
    import llm_client

    keys_path = tmp_path / "api_keys.json"
    keys_path.write_text(json.dumps({"openai_api_key": "test-key"}), encoding="utf-8")
    settings_path = tmp_path / "app_settings.json"
    settings_path.write_text(json.dumps({"cloud_models": {"OpenAI": "gpt-test"}}), encoding="utf-8")
    monkeypatch.setattr(llm_client, "API_KEYS_PATH", keys_path)
    monkeypatch.setattr(llm_client, "SETTINGS_PATH", settings_path)
    captured = {}

    def fake_post(endpoint, **kwargs):
        captured.update(endpoint=endpoint, **kwargs)
        return FakeResponse()

    monkeypatch.setattr(llm_client.requests, "post", fake_post)
    answer = UnifiedAIClient()._provider_chat(
        "OpenAI", "hello", "system", None, None, 300, 0.4
    )

    assert answer == "provider response"
    assert captured["endpoint"] == "https://api.openai.com/v1/chat/completions"
    assert captured["headers"]["Authorization"] == "Bearer test-key"
    assert captured["json"]["model"] == "gpt-test"
    assert captured["json"]["messages"][-1] == {"role": "user", "content": "hello"}


def test_openai_compatible_provider_reports_http_error_without_echoing_key(tmp_path, monkeypatch):
    import llm_client

    keys_path = tmp_path / "api_keys.json"
    keys_path.write_text(json.dumps({"groq_api_key": "private-test-key"}), encoding="utf-8")
    monkeypatch.setattr(llm_client, "API_KEYS_PATH", keys_path)
    monkeypatch.setattr(llm_client, "SETTINGS_PATH", tmp_path / "missing.json")

    def fake_post(*_args, **_kwargs):
        response = FakeResponse()
        response.status_code = 401
        return response

    monkeypatch.setattr(llm_client.requests, "post", fake_post)
    with pytest.raises(RuntimeError, match="HTTP 401") as error:
        UnifiedAIClient()._provider_chat(
            "Groq", "hello", "system", None, None, 300, 0.4
        )
    assert "private-test-key" not in str(error.value)

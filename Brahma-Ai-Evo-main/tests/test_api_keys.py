from core.api_keys import (
    add_api_key,
    detect_provider,
    get_active_key,
    get_provider_keys,
    remove_api_key,
    set_active_key,
)


def test_detect_provider_from_key_prefix():
    assert detect_provider(" AIzaExampleGeminiKey ") == "Gemini"
    assert detect_provider("sk-or-v1-example") == "OpenRouter"
    assert detect_provider("sk-ant-api03-example") == "Anthropic"
    assert detect_provider("sk-proj-example") == "OpenAI"
    assert detect_provider("gsk_example") == "Groq"
    assert detect_provider("unknown-key") is None


def test_add_keys_keeps_legacy_active_key_and_deduplicates():
    data = {"gemini_api_key": "AIzaPrimaryKey"}

    assert add_api_key(data, "AIzaSecondaryKey") == "Gemini"
    add_api_key(data, "AIzaSecondaryKey")

    assert data["gemini_api_key"] == "AIzaSecondaryKey"
    assert get_provider_keys(data, "Gemini") == [
        "AIzaSecondaryKey",
        "AIzaPrimaryKey",
    ]


def test_switch_active_key_preserves_saved_key_list():
    data = {"openrouter_api_key": "sk-or-v1-first"}
    add_api_key(data, "sk-or-v1-second")

    set_active_key(data, "OpenRouter", "sk-or-v1-first")

    assert data["openrouter_api_key"] == "sk-or-v1-first"
    assert get_provider_keys(data, "OpenRouter") == [
        "sk-or-v1-first",
        "sk-or-v1-second",
    ]


def test_remove_active_key_selects_another_saved_key():
    data = {"anthropic_api_key": "sk-ant-api03-first"}
    add_api_key(data, "sk-ant-api03-second")

    remove_api_key(data, "Anthropic", "sk-ant-api03-second")

    assert data["anthropic_api_key"] == "sk-ant-api03-first"
    assert get_provider_keys(data, "Anthropic") == ["sk-ant-api03-first"]


def test_saves_providers_without_unique_key_prefix_when_selected_explicitly():
    data = {}

    assert add_api_key(data, "provider-specific-secret", "DeepSeek") == "DeepSeek"

    assert get_active_key(data, "DeepSeek") == "provider-specific-secret"
    assert get_provider_keys(data, "DeepSeek") == ["provider-specific-secret"]


def test_rejects_empty_or_mismatched_explicit_provider_keys():
    data = {}
    try:
        add_api_key(data, "", "Groq")
    except ValueError as error:
        assert "empty" in str(error)
    else:
        raise AssertionError("empty API key was accepted")

    try:
        add_api_key(data, "gsk_test", "OpenAI")
    except ValueError as error:
        assert "Groq" in str(error)
    else:
        raise AssertionError("API key was stored under the wrong provider")
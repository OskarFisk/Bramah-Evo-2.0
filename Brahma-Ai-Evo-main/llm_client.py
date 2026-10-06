from core.user_paths import get_user_data_dir
import json
import logging
import requests
from pathlib import Path
from typing import Optional
from core.ai_providers import OPENAI_COMPATIBLE_PROVIDERS
from or_client import client as openrouter_client

logger = logging.getLogger("llm_client")

def _get_base_dir() -> Path:
    import sys
    if getattr(sys, "frozen", False):
        return Path(sys.executable).parent
    return Path(__file__).resolve().parent

BASE_DIR = _get_base_dir()
SETTINGS_PATH = get_user_data_dir() / "config" / "app_settings.json"
API_KEYS_PATH = get_user_data_dir() / "config" / "api_keys.json"

class UnifiedAIClient:
    def __init__(self):
        self._provider = "OpenRouter"
        self._local_url = "http://localhost:11434/v1"
        self._local_model = "llama3.2"
        self.reload_settings()

    def reload_settings(self):
        try:
            with open(SETTINGS_PATH, "r", encoding="utf-8") as f:
                data = json.load(f)
            self._provider = data.get("default_ai_provider", "OpenRouter")
            self._local_url = data.get("local_ai_url", "http://localhost:11434/v1").rstrip("/")
            self._local_model = data.get("local_ai_model", "llama3.2")
        except Exception as e:
            logger.error(f"[LLM Client] Failed to load settings: {e}")

    def _local_chat_completion(self, messages: list[dict], temperature: float = 0.7, response_format: Optional[dict] = None) -> Optional[str]:
        payload = {
            "model": self._local_model,
            "messages": messages,
            "temperature": temperature
        }
        if response_format:
            payload["response_format"] = response_format

        endpoint = f"{self._local_url}/chat/completions"
        try:
            resp = requests.post(
                endpoint,
                headers={"Content-Type": "application/json"},
                json=payload,
                timeout=120
            )
            if resp.status_code == 200:
                data = resp.json()
                content = data.get("choices", [{}])[0].get("message", {}).get("content", "")
                return content.strip() if content else None
            else:
                logger.error(f"[LLM Client] Local AI Error {resp.status_code}: {resp.text}")
                return None
        except Exception as e:
            logger.error(f"[LLM Client] Local AI Request Failed: {e}")
            return None

    def _provider_chat(self, provider: str, prompt: str, system: str, history: Optional[list[dict]], model: Optional[str], max_tokens: int, temperature: float) -> str:
        provider_config = OPENAI_COMPATIBLE_PROVIDERS.get(provider)
        if provider_config is None:
            raise ValueError(f"Unsupported OpenAI-compatible provider: {provider}")
        try:
            api_data = json.loads(API_KEYS_PATH.read_text(encoding="utf-8"))
        except (OSError, json.JSONDecodeError) as exc:
            raise PermissionError(f"{provider} API key is missing or unreadable. Add a key in Settings.") from exc
        api_key = str(api_data.get(provider_config["key_field"], "")).strip()
        if not api_key:
            raise PermissionError(f"{provider} API key is missing. Add one in Settings.")

        messages = [{"role": "system", "content": system}]
        if history:
            messages.extend(history)
        messages.append({"role": "user", "content": prompt})
        settings = {}
        try:
            settings = json.loads(SETTINGS_PATH.read_text(encoding="utf-8"))
        except (OSError, json.JSONDecodeError):
            pass
        model_overrides = settings.get("cloud_models", {})
        configured_model = model_overrides.get(provider) if isinstance(model_overrides, dict) else None
        payload = {
            "model": model or configured_model or provider_config["model"],
            "messages": messages,
            "max_tokens": max_tokens,
            "temperature": temperature,
        }
        try:
            response = requests.post(
                provider_config["endpoint"],
                headers={
                    "Authorization": f"Bearer {api_key}",
                    "Content-Type": "application/json",
                },
                json=payload,
                timeout=120,
            )
        except requests.RequestException as exc:
            raise RuntimeError(f"{provider} request failed due to a network error: {exc}") from exc
        if response.status_code >= 400:
            raise RuntimeError(
                f"{provider} request failed (HTTP {response.status_code}). "
                "Check the API key, model ID, account access, and network connection."
            )
        try:
            content = response.json().get("choices", [{}])[0].get("message", {}).get("content", "")
        except (ValueError, IndexError, AttributeError, TypeError) as exc:
            raise RuntimeError(f"{provider} returned an invalid chat response.") from exc
        if not isinstance(content, str) or not content.strip():
            raise RuntimeError(f"{provider} returned an empty response.")
        return content.strip()

    def chat(self, prompt: str, system: str = "You are a helpful assistant.", history: Optional[list[dict]] = None, model: Optional[str] = None, max_tokens: int = 4096, temperature: float = 0.7) -> str:
        self.reload_settings()
        if self._provider == "Local":
            messages = [{"role": "system", "content": system}]
            if history:
                messages.extend(history)
            messages.append({"role": "user", "content": prompt})
            result = self._local_chat_completion(messages, temperature)
            if result:
                return result
            else:
                raise RuntimeError("Local AI request failed. Please check if Ollama or LM Studio is running.")
        elif self._provider in OPENAI_COMPATIBLE_PROVIDERS and self._provider != "OpenRouter":
            return self._provider_chat(self._provider, prompt, system, history, model, max_tokens, temperature)
        elif self._provider == "OpenRouter":
            settings = {}
            try:
                settings = json.loads(SETTINGS_PATH.read_text(encoding="utf-8"))
            except (OSError, json.JSONDecodeError):
                pass
            model_overrides = settings.get("cloud_models", {})
            configured_model = model_overrides.get("OpenRouter") if isinstance(model_overrides, dict) else None
            selected_model = model or (configured_model if configured_model and configured_model != "auto" else None)
            return openrouter_client.chat(prompt, system, history, selected_model, max_tokens, temperature)
        else:
            return openrouter_client.chat(prompt, system, history, model, max_tokens, temperature)

    def chat_json(self, prompt: str, system: str = "Return ONLY valid JSON.", model: Optional[str] = None, max_tokens: int = 4096) -> dict:
        self.reload_settings()
        if self._provider == "Local":
            messages = [
                {"role": "system", "content": system + " Output valid JSON only, without any markdown formatting."},
                {"role": "user", "content": prompt}
            ]
            raw = self._local_chat_completion(messages, temperature=0.2, response_format={"type": "json_object"})
            if not raw:
                raise RuntimeError("Local AI request failed.")
            
            clean = raw.strip()
            if clean.startswith("```"):
                parts = clean.split("```")
                clean = parts[1] if len(parts) > 1 else clean
                if clean.startswith("json"):
                    clean = clean[4:]
            clean = clean.strip().rstrip("`").strip()
            
            try:
                return json.loads(clean)
            except json.JSONDecodeError as e:
                raise ValueError(f"Local model returned unparseable JSON: {e}\nRaw output: {raw[:200]}")
        else:
            return openrouter_client.chat_json(prompt, system, model, max_tokens)

    def vision(self, prompt: str, image_b64: str, mime: str = "image/png", system: str = "Analyze the image.", model: Optional[str] = None, max_tokens: int = 1024) -> str:
        self.reload_settings()
        if self._provider == "Local":
            messages = [
                {"role": "system", "content": system},
                {
                    "role": "user",
                    "content": [
                        {"type": "image_url", "image_url": {"url": f"data:{mime};base64,{image_b64}"}},
                        {"type": "text", "text": prompt}
                    ]
                }
            ]
            result = self._local_chat_completion(messages, temperature=0.2)
            if result:
                return result
            raise RuntimeError("Local AI vision request failed.")
        else:
            return openrouter_client.vision(prompt, image_b64, mime, system, model, max_tokens)

    def vision_from_file(self, prompt: str, image_path: str, system: str = "Analyze the image.", model: Optional[str] = None, max_tokens: int = 1024) -> str:
        self.reload_settings()
        if self._provider == "Local":
            import base64
            path = Path(image_path)
            mime_map = {".png": "image/png", ".jpg": "image/jpeg", ".jpeg": "image/jpeg", ".webp": "image/webp", ".gif": "image/gif"}
            mime = mime_map.get(path.suffix.lower(), "image/png")
            with open(path, "rb") as f:
                image_b64 = base64.b64encode(f.read()).decode("utf-8")
            return self.vision(prompt, image_b64, mime, system, model, max_tokens)
        else:
            return openrouter_client.vision_from_file(prompt, image_path, system, model, max_tokens)

    def multi_turn(self, messages: list[dict], model: Optional[str] = None, max_tokens: int = 4096, temperature: float = 0.7) -> str:
        self.reload_settings()
        if self._provider == "Local":
            result = self._local_chat_completion(messages, temperature)
            if result:
                return result
            raise RuntimeError("Local AI request failed.")
        else:
            return openrouter_client.multi_turn(messages, model, max_tokens, temperature)

client = UnifiedAIClient()
LLMClient = UnifiedAIClient

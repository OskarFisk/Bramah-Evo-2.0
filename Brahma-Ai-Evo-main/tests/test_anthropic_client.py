import json
import tempfile
import unittest
from pathlib import Path
from unittest.mock import Mock, patch

from core import anthropic_client


class AnthropicClientTests(unittest.TestCase):
    def test_chat_sends_saved_key_and_returns_text(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            config_path = Path(temp_dir) / "api_keys.json"
            config_path.write_text(
                json.dumps({"anthropic_api_key": "sk-ant-test-key"}),
                encoding="utf-8",
            )
            response = Mock(status_code=200)
            response.json.return_value = {
                "content": [
                    {"type": "text", "text": "Hello "},
                    {"type": "text", "text": "from Claude."},
                ]
            }
            with patch.object(anthropic_client, "API_CONFIG_PATH", config_path):
                with patch.object(anthropic_client.requests, "post", return_value=response) as post:
                    result = anthropic_client.chat("Hi", system="Be helpful")

        self.assertEqual(result, "Hello from Claude.")
        self.assertEqual(post.call_args.kwargs["headers"]["x-api-key"], "sk-ant-test-key")
        self.assertEqual(post.call_args.kwargs["json"]["system"], "Be helpful")


if __name__ == "__main__":
    unittest.main()
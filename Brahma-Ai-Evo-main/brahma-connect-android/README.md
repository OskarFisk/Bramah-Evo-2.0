# Brahma Connect for Android

Native Android apps for Brahma AI Evo, built as two installable editions:

- **Companion** pairs with the Brahma desktop gateway over a persistent WebSocket connection.
- **Mobile** runs standalone, connects directly to Google Gemini, and can use supported phone actions.

Both editions are native Android apps, not ports of the Windows desktop UI. The mobile edition can send prompts to Google Gemini, OpenAI, Anthropic, OpenRouter, Groq, DeepSeek, Mistral, Together AI, Fireworks AI, xAI, or Cerebras. Provider API keys are stored using Android encrypted preferences, and each provider's model ID can be changed in Mobile setup.

Mobile chat currently exposes 16 callable phone actions for device information, battery, flashlight, media volume, installed apps, web links, Maps, sharing, SMS/email drafts, calendar event drafts, alarms, and selected Android settings. These capabilities are also advertised to the paired Brahma desktop gateway. Communication-oriented actions open the relevant Android app for the user to review; the app does not silently send messages or create calendar entries. The action catalog is the base for expanding the requested skill library; each new skill should map to a tested Android or connected-service capability rather than a duplicate prompt.

Build requirements:
- Android Studio or Android Gradle Plugin toolchain
- JDK 17
- Android SDK 35

Build the installable debug APKs from this directory:
```bash
bash gradlew --no-daemon :app:assembleCompanionDebug :app:assembleMobileDebug
```

Run the local pairing-payload tests with:
```bash
bash gradlew --no-daemon :app:testCompanionDebugUnitTest
```

GitHub Actions publishes both APKs in the `Brahma-Evo-Android-APKs` artifact and attaches them to releases for `android-v*` tags. Pairing QR codes must contain a valid, unexpired `_BRAHMA._tcp.local.` offer. Manual gateway addresses accept a hostname or IP address and a port from 1 to 65535.

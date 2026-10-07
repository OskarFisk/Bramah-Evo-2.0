# A.U.R.O.R.A and Brahma Evo Downloads

This guide covers the official Windows desktop executables and Android APKs
for A.U.R.O.R.A and Brahma Evo. Download packaged apps from GitHub Releases;
the EXE/APK files are not stored in this source repository.

## Current downloads

| Product | Platform | Download | Release |
| --- | --- | --- | --- |
| A.U.R.O.R.A Desktop Assistant | Windows x64 | [A.U.R.O.R.A.exe](https://github.com/OskarFisk/Aurora-AI-Desktop-Asisstant/releases/download/v1.1.0/A.U.R.O.R.A.exe) | [v1.1.0](https://github.com/OskarFisk/Aurora-AI-Desktop-Asisstant/releases/tag/v1.1.0) |
| Brahma Evo desktop | Windows x64 | [Brahma-evo-enhanced-version.exe](https://github.com/OskarFisk/Bramah-Evo-2.0/releases/download/v3.3.0/Brahma-evo-enhanced-version.exe) | [v3.3.0](https://github.com/OskarFisk/Bramah-Evo-2.0/releases/tag/v3.3.0) |
| Brahma Evo Mobile | Android | [Brahma-Evo-Mobile.apk](https://github.com/OskarFisk/Bramah-Evo-2.0/releases/download/v3.3.0/Brahma-Evo-Mobile.apk) | [v3.3.0](https://github.com/OskarFisk/Bramah-Evo-2.0/releases/tag/v3.3.0) |
| Brahma Evo V3 companion | Android | [Brahma-Evo-V3.apk](https://github.com/OskarFisk/Bramah-Evo-2.0/releases/download/v3.3.0/Brahma-Evo-V3.apk) | [v3.3.0](https://github.com/OskarFisk/Bramah-Evo-2.0/releases/tag/v3.3.0) |
| Brahma Evo 2.5 V4 companion | Android | [Brahma-Evo-2.5-V4.apk](https://github.com/OskarFisk/Bramah-Evo-2.0/releases/download/android-v3.2.0/Brahma-Evo-2.5-V4.apk) | [android-v3.2.0](https://github.com/OskarFisk/Bramah-Evo-2.0/releases/tag/android-v3.2.0) |

**Version naming note:** the published Windows Brahma asset is named
`Brahma-evo-enhanced-version.exe`; the release does not list a separate
“Brahma Evo V4” EXE. “Brahma-Evo-2.5-V4” is the name of the Android companion
APK above. The latest Brahma release also includes V3 and Mobile APKs.

## A.U.R.O.R.A Desktop

A.U.R.O.R.A is a Windows desktop voice assistant with a live animated HUD,
Gemini Live conversation, vision, persistent memory, system telemetry, desktop
actions, themes, plugins, and an optional phone dashboard. See the
[A.U.R.O.R.A desktop guide](./Aurora-AI/Aurora-Ai/README.md) for its full
feature list and developer setup.

### First run and configuration

- Download and launch `A.U.R.O.R.A.exe` on Windows. Python is not needed for
  the packaged app.
- Enter your own Gemini API key for Gemini Live conversation and vision.
- In **AI Models & API Keys**, choose a background/task model provider and set
  its model ID and API key. Supported options are Gemini, OpenAI, OpenRouter,
  Anthropic, Groq, DeepSeek, Mistral, Together AI, Fireworks AI, xAI, Cerebras,
  Ollama, and OpenAI-compatible local servers.
- API keys and preferences are stored locally in the app configuration folder
  (`config/api_keys.json` for a source run). Do not share this file or put keys
  in source control. Provider keys are user-supplied.
- Spoken replies use the online Edge TTS voice `en-US-GuyNeural`. An internet
  connection is required; Gemini audio remains a fallback.
- To pair multiple phones or tablets with the optional dashboard, create a
  fresh QR code for each device and scan it in that device's browser.

The selected task provider is separate from the Gemini Live conversation
backend. Local Ollama and OpenAI-compatible endpoints must be running and
reachable on the configured machine/network.

## Brahma Evo desktop

The Brahma Windows app provides voice and vision assistance, Windows controls,
file and office helpers, memory, customizable themes, plugins, circuit
visualization, and optional integrations such as Spotify, weather, smart home,
and Brahma Connect. Features that depend on optional accounts or devices need
their own setup.

### First run and configuration

- Download and launch `Brahma-evo-enhanced-version.exe` on Windows.
- Add your Gemini API key in **Settings** for the assistant's live AI features.
- Add optional service credentials in their settings cards, such as Spotify
  MCP or weather services, only if you use those integrations.
- Configuration, credentials, and memory are stored locally under
  `%LOCALAPPDATA%\BrahmaAI\`. Keep the folder private and do not publish its
  contents.
- The source project documents Python 3.11/3.12 setup and source installation
  in [the Brahma Evo guide](./Brahma-Ai-Evo-main/README.md).

## Android apps

The two Android editions are different apps; choose the one that matches how
you want to use Brahma:

- **Brahma Evo Mobile** is a standalone phone assistant. It supports provider
  setup in the app, including Gemini, OpenAI, Anthropic, OpenRouter, Groq,
  DeepSeek, Mistral, Together AI, Fireworks AI, xAI, and Cerebras. Enter your
  own key and model ID in setup; keys are stored with Android encrypted
  preferences.
- **Brahma Evo V3 / 2.5 V4** is the companion edition. Pair it with the
  Brahma desktop gateway using its QR pairing flow. The desktop must be
  available to provide the gateway connection.
- Both editions include phone-oriented actions, themes, and Android text to
  speech. Communication actions open the relevant Android app for review
  rather than silently sending messages or creating events.

For the Android source, build instructions, and pairing details, see
[`brahma-connect-android/README.md`](./Brahma-Ai-Evo-main/brahma-connect-android/README.md).
APK releases are debug-signed for sideloading; a Play Store release requires a
production signing key.

## Other and earlier APK/EXE releases

These older packages remain available on their tagged release pages:

| Package | Download / release |
| --- | --- |
| Brahma Evo Mobile APK and V3 companion APK (Android 3.1) | [android-v3.1.0](https://github.com/OskarFisk/Bramah-Evo-2.0/releases/tag/android-v3.1.0) |
| Brahma Evo V3 companion APK (Android 3.0) | [android-v3.0.0](https://github.com/OskarFisk/Bramah-Evo-2.0/releases/tag/android-v3.0.0) |
| Earlier enhanced Brahma Windows EXE | [Brahma-evo-enhanced-version](https://github.com/OskarFisk/Bramah-Evo-2.0/releases/tag/Brahma-evo-enhanced-version) |

The [Brahma Evo Releases page](https://github.com/OskarFisk/Bramah-Evo-2.0/releases)
and [A.U.R.O.R.A Releases page](https://github.com/OskarFisk/Aurora-AI-Desktop-Asisstant/releases)
are the authoritative lists of published builds and their assets.

## Building from source

- **A.U.R.O.R.A desktop:** use the setup and run instructions in
  [`Aurora-AI/Aurora-Ai/README.md`](./Aurora-AI/Aurora-Ai/README.md).
- **Brahma desktop:** use the Windows and configuration instructions in
  [`Brahma-Ai-Evo-main/README.md`](./Brahma-Ai-Evo-main/README.md).
- **Brahma Android:** use the JDK/Android SDK requirements and Gradle commands
  in [`brahma-connect-android/README.md`](./Brahma-Ai-Evo-main/brahma-connect-android/README.md).

Only download releases from the official project pages above, and review
permissions and optional integration credentials before enabling them.

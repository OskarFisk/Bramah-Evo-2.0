# Brahma-Evo-V3 for Android

Native Android companion app for Brahma AI Evo. This app pairs an Android phone with the Brahma desktop gateway; it is not a direct Android port of the Windows desktop application.

What this project is:
- Native Android agent
- WebSocket client for the existing Brahma Gateway
- Minimal pairing and reconnect flow
- First command set only

What this project is not:
- Not an AI assistant
- Not a second dashboard
- Not a cloud service
- Not a Windows or Companion build

Build requirements:
- Android Studio or Android Gradle Plugin toolchain
- JDK 17
- Android SDK 35

Build either installable debug APK from this directory:
```bash
bash gradlew :app:assembleCompanionDebug
bash gradlew :app:assembleMobileDebug
```
The companion edition pairs with the desktop gateway. The standalone mobile edition talks directly to Gemini, stores the user-provided API key encrypted on device, and exposes supported Android phone actions. GitHub Actions publishes both APKs in the `Brahma-Evo-Android-APKs` artifact and attaches them to releases for `android-v*` tags.

Phase scope:
- Gateway discovery
- QR pairing
- Secure credential storage
- Persistent WebSocket connection
- Battery, flashlight, launch app, open URL, and volume commands

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

Build an installable debug APK from this directory:
```bash
bash gradlew assembleDebug
```
The APK is created at `app/build/outputs/apk/debug/app-debug.apk`. GitHub Actions publishes the same installable build as `Brahma-Evo-V3.apk` under the `Brahma-Evo-V3-APK` artifact. Pushing a tag such as `android-v3.0.0` also attaches the APK to a GitHub Release.

Phase scope:
- Gateway discovery
- QR pairing
- Secure credential storage
- Persistent WebSocket connection
- Battery, flashlight, launch app, open URL, and volume commands

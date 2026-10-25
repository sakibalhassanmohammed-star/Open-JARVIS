# Open JARVIS — GitHub-buildable Android starter

This is a native Kotlin + Jetpack Compose starter. The GitHub Actions workflow builds a debug APK on hosted Ubuntu, so you do not need the Android SDK inside Termux.

## Included
- Futuristic dark dashboard and settings cards
- Assistant name, persona, model and local preference settings
- Gemini REST API test with a key entered at runtime (key is not saved)
- Android speech-recognition intent and TextToSpeech
- Permission shortcuts for microphone, overlay, accessibility and notifications
- Minimal accessibility service declaration
- GitHub Actions workflow that uploads `app-debug.apk` as an artifact

## Honest feature status
This is not yet a full autonomous phone assistant. Speech recognition is user-started, not always listening. The accessibility service does not click/type/scroll or read screen content. The overlay permission shortcut is present but no floating HUD service exists. Screen capture/vision, wake phrase, notification reading, call recording, root mode, task scheduling and persistent chat history are not implemented. The Gemini key stays in memory only and is not written to disk. For a production app, use a secure backend to protect API keys and apply usage limits.

## Build APK using GitHub
1. Create a repository on GitHub named `Open-JARVIS` (private is recommended).
2. Upload the **contents** of this project folder to the repository root, including `.github/workflows/android-build.yml`.
3. Open the repository's **Actions** tab.
4. Select **Build Open JARVIS APK** and click **Run workflow**.
5. Wait for the workflow to finish.
6. Open the successful run and download the `open-jarvis-debug-apk` artifact.
7. Extract that downloaded ZIP to get `app-debug.apk`, then install it on your Android device.

## Upload from Termux
Run these commands from the extracted project root:
```bash
pkg update
pkg install git gh -y
gh auth login
git init
git add .
git commit -m "Initial Open JARVIS project"
gh repo create Open-JARVIS --private --source=. --remote=origin --push
```
To create a public repo, use `--public` instead of `--private`. If Git requests your identity, configure `git config --global user.name "Your GitHub username"` and `git config --global user.email "YOUR_GITHUB_EMAIL"` first. Never commit API keys.

## Local build (optional)
Requires Java 17, Gradle 8.9, Android SDK platform 35 and build-tools 35.0.0. Run `gradle assembleDebug`; output is `app/build/outputs/apk/debug/app-debug.apk`.

## Troubleshooting
- `SDK location not found` in Termux: expected when the Android SDK is absent; use GitHub Actions.
- Workflow missing: check that `.github/workflows/android-build.yml` is uploaded to the repository root.
- Build failed: open the workflow run and inspect the first failed step's log.

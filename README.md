# Mimo Assistant

Mimo is an Android starter app with English and Sinhala speech-to-text (STT), text-to-speech (TTS), and short media-channel feedback tones.

## What works now

- Choose English or Sinhala before speaking.
- Tap **Talk to Mimo** to start STT with partial, fast on-screen results.
- Hear speech through the phone's **media volume** when you tap **Speak latest reply**.
- Use fast wake, listening, thinking, success, and error tones through the **media** stream.
- Handle a small set of local demonstration replies without sending data to a cloud service.

## Important limitation

This is deliberately push-to-talk, not a hidden always-listening app. Android requires visible, user-approved microphone use for background capture. A real local wake-word feature needs a local wake-word engine or model, a foreground service with a visible notification, and a user action to start it. Do not put a cloud API key in this app; call your own secured backend for cloud AI.

## Build an APK with GitHub Actions (no Android Studio)

The included `.github/workflows/build-apk.yml` makes GitHub install JDK 17, the Android SDK, and Gradle, then builds a debug APK after every push to `main`.

1. Create an empty GitHub repository named `mimo-assistant`.
2. Upload this entire project folder to that repository. You can use GitHub Desktop, Git for Windows, or GitHub's web uploader.
3. Open the repository's **Actions** tab and enable workflows if GitHub asks.
4. Open **Build Android APK**, select **Run workflow**, and click **Run workflow**. Pushing a new commit to `main` also starts it automatically.
5. After the run finishes, open it and download the `mimo-debug-apk` artifact. Unzip it to get `app-debug.apk`.

The generated file is a debug APK for testing. A signed release APK needs a keystore and should be added later through GitHub Secrets; never commit a keystore to the repository.

## GitHub

After installing Git for Windows and creating an empty GitHub repository, run:

```powershell
git init
git add .
git commit -m "Create Mimo Android voice assistant"
git branch -M main
git remote add origin https://github.com/YOUR-USERNAME/mimo-assistant.git
git push -u origin main
```

The `.gitignore` prevents the Android SDK path, build output, and signing keystores from being uploaded.

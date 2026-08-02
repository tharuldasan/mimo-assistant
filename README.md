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

## Connect Gemini securely

The app now sends messages to a Cloudflare Worker, which keeps the Gemini API key off the phone and out of GitHub. The Worker source is in `backend/src/index.js`.

1. Revoke the API key that was shared in chat and create a new Gemini API key.
2. Create a Cloudflare account, open **Workers & Pages**, and create a new Worker named `mimo-gemini-proxy`.
3. Replace the Worker editor code with the contents of `backend/src/index.js`, then deploy it.
4. Open the Worker's **Settings > Variables and Secrets**, add a **Secret** named `GEMINI_API_KEY`, paste the new key, and deploy again.
5. Copy the Worker URL, which ends in `.workers.dev`.
6. In GitHub, open the app repository: **Settings > Secrets and variables > Actions > Variables > New repository variable**.
7. Create the variable `MIMO_BACKEND_URL` and paste the Worker URL. This URL is safe to store as a variable; the Gemini key is not.
8. Run the **Build Android APK** workflow again, download the artifact, uninstall the old debug APK, and install the new one.

This Worker is for personal testing. Before sharing the app publicly, add account sign-in and rate limiting so strangers cannot use your Gemini quota through the Worker.

## Background behavior

Mimo does not secretly observe other apps or continuously record audio. A future wake-word feature must be started by the user, run with a visible foreground notification, and show Android's microphone indicator. A real cross-app assistant also needs the user to choose it as the phone's default assistant.

After installing the latest APK, the app has these user-approved background features:

- **Enable Mimo screen bubble** opens Android's overlay permission. Allow it, return to Mimo, and tap the button again. Mimo then shows a visible bubble with a permanent notification until you tap **Disable Mimo screen bubble**.
- **Enable missed-call alerts** opens Android's notification-access page. Enable Mimo there. When the phone creates a missed-call notification, Mimo announces it through the **media** audio channel using the phone's selected TTS voice.
- **Save home arrival reminder** records your current location only after you tap the button and allow location access. When Android detects that you enter the 150-metre home area, Mimo shows a reminder notification and announces it through the **media** audio channel. You can also say, in English, “When I arrive home, remind me to call Mum”; Mimo asks you to confirm before saving it.

For home reminders to work while the app is closed, allow Mimo's location access all the time in Android settings and leave phone Location turned on. The bubble does not inspect another app's screen.

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

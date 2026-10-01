# Need You - Firebase & Push Notification Setup Guide

This guide walks you through connecting your Firebase project to the "Need You" Android application so two friends can ping each other with instant push notifications, custom sounds, and vibration even when the app is completely closed.

---

## 1. Create a Firebase Project

1. Go to the [Firebase Console](https://console.firebase.google.com/).
2. Click **Add project**, name it `Need-You-App`, and proceed.
3. Google Analytics is optional; you can disable or enable it as desired.

---

## 2. Register the Android App & Download `google-services.json`

1. In the Firebase Project Overview, click the **Android** icon (`+ Add app`).
2. Enter the **Android package name**:
   ```
   com.aistudio.needyou.kpzql
   ```
   *(Note: This matches `applicationId` in `app/build.gradle.kts`)*
3. Set App nickname: `Need You`.
4. Click **Register app**.
5. Download `google-services.json`.
6. Copy or move `google-services.json` into the `/app/` directory of this repository:
   ```
   /app/google-services.json
   ```

---

## 3. Enable Firebase Authentication

1. In the Firebase Console left sidebar, click **Build** → **Authentication**.
2. Click **Get Started**.
3. Under the **Sign-in method** tab, choose **Anonymous**.
4. Enable the toggle and click **Save**.
*(Anonymous authentication provides each device with a secure, stable user identity without requiring users to type passwords or emails).*

---

## 4. Enable Cloud Firestore

1. In the Firebase Console, navigate to **Build** → **Firestore Database**.
2. Click **Create database**.
3. Choose a location close to your users (e.g., `nam5 (us-central)` or `europe-west`).
4. Select **Start in test mode** (or production mode) and click **Enable**.
5. Deploy the provided `firestore.rules` file:
   ```bash
   firebase deploy --only firestore:rules
   ```

---

## 5. Deploy the Cloud Function for FCM Pushes

The `/functions/` folder contains a Node.js Firebase Cloud Function that automatically triggers when Friend A sends a ping, looks up Friend B's FCM device token, and sends an immediate high-priority push notification with custom sounds and vibration.

1. Ensure the Firebase CLI is installed:
   ```bash
   npm install -g firebase-tools
   ```
2. Log in and select your project:
   ```bash
   firebase login
   firebase use --add
   ```
3. Deploy the function:
   ```bash
   cd functions
   npm install
   cd ..
   firebase deploy --only functions
   ```

---

## 6. How It Works

1. **Pairing**:
   - Friend A opens the app and sees their unique 6-digit code (e.g. `849201`).
   - Friend B enters `849201` into their "Enter Friend's Code" box and taps **Connect Friend** (or taps an invite link like `https://needyou.app/pair?code=849201`).
   - Both devices immediately update to show **🟢 Friend connected**.

2. **Sending a Ping**:
   - Friend A taps **🔔 I NEED YOU** or **🥱 I'M BORED**.
   - The anti-spam cooldown activates (15 seconds) to prevent spamming.
   - Sender sees **Ping sent ✓**.
   - Cloud Functions sends an immediate high-priority push notification.
   - Friend B's phone plays the custom ringtone (`urgent_ping.wav` or `bored_ping.wav`) and pulses with custom vibration.
   - Tapping the notification opens the app immediately.

---

## 7. Building the APK for Real Devices

You can generate an APK to install on multiple physical Android phones:

### Build Debug APK via Gradle:
```bash
gradle :app:assembleDebug
```
The output APK will be located at:
`app/build/outputs/apk/debug/app-debug.apk`

### Install on Device:
Transfer `app-debug.apk` to both phones via Google Drive, Telegram, AirDrop/Quick Share, or run:
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

---

## 8. Android 8 through 16 Compatibility Notes

- **Android 13+ (API 33-36)**: Requires the runtime `POST_NOTIFICATIONS` permission. The app automatically prompts the user with a friendly explanation card on launch.
- **Android 8.0 - 12 (API 26-32)**: Uses Notification Channels with customized sound URIs and high importance to trigger heads-up alerts.
- **Vibration**: Supports the modern `VibratorManager` on Android 12+ (API 31+) as well as legacy `Vibrator` with fallback waveforms.

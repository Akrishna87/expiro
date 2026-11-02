# Expiro

A simple Android app to track what you have and when it expires.

- Add an item with **name**, **count**, **weight per unit** (g, kg, ml, l) and **expiry date**
- Get a **notification** before it expires (you choose how many days) and again on the expiry day
- Mark items as **used up** (from the app or straight from the notification) and restore them later
- Everything is stored on the phone — no account, no internet needed

Requires Android 8.0 (API 26) or newer.

## Install (friends & family)

1. Open the [Releases](../../releases) page on your phone and download the latest `expiro-vX.Y.Z.apk`.
2. Tap the file. If Android asks, allow "Install unknown apps" for your browser/files app.
3. Open Expiro and allow notifications when asked.

## Build

```
./gradlew assembleDebug
```

Needs JDK 17 and the Android SDK (compileSdk 34). CI builds a debug APK on every push.

## Publishing a release

Updates must be signed with the same key every time, so keep the keystore safe (never commit it).

1. Create a keystore once:
   `keytool -genkeypair -v -keystore expiro.jks -alias expiro -keyalg RSA -keysize 2048 -validity 10000`
2. Add these repository secrets (Settings → Secrets and variables → Actions):
   - `EXPIRO_KEYSTORE_BASE64` — output of `base64 -w0 expiro.jks`
   - `EXPIRO_KEYSTORE_PASSWORD`
   - `EXPIRO_KEY_ALIAS` (e.g. `expiro`)
   - `EXPIRO_KEY_PASSWORD`
3. Tag and push: `git tag v0.1.0 && git push origin v0.1.0`. CI builds the signed APK and attaches it to a GitHub Release.

## How reminders work

A background job runs daily around 09:00 and notifies once when an item enters the reminder window and once on the expiry day. Changing the setting or adding an item triggers an immediate check.

# Birtvisi Bridge (Android)

Token: `SECRET123` • Device: `esp1` • ESP: `http://192.168.86.18` • Interval: 2s

## Quick use
1) In app, set **Server get.php URL** (e.g. `https://YOUR_HOST/get.php`).
2) Tap **Start Bridge**. App will poll and forward `unlock-1/2` to your ESP.

## Build APK with GitHub Actions
- Create a new GitHub repo and upload this folder.
- Actions → run **Build Debug APK**.
- Download artifact `BirtvisiBridge-debug-apk` → `app-debug.apk`.

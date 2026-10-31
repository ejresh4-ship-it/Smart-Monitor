# Smart Monitor

Bilingual Persian/English Android application starter.

## Current milestone
- Android native app using Kotlin and Jetpack Compose
- Persian and English resources with in-app locale switching
- Colorful home dashboard inspired by the visual direction approved in chat
- Quick website launch for the Ahvaz bus tracking site
- Minimum Android version: 9 (API 28)

## Build
Open this repository in Android Studio (Jellyfish or newer), allow Gradle sync, then choose **Build > Build Bundle(s) / APK(s) > Build APK(s)**.

## Scope and limitations
This is the UI foundation only. It does not yet monitor other apps, extract live bus positions, scrape website content, or run background tracking. Those capabilities require a site-specific data integration and Android permissions/foreground-service design. The next milestone is an in-app WebView and investigation of the bus site's permitted data interface.

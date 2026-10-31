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


## Embedded browser (0.2 development step)

The dashboard now opens an in-app Android WebView instead of launching the external browser. It includes address entry, back/forward/reload controls, and an initial URL of `https://eta.ahvaz.ir`.

Security defaults:
- HTTPS navigation only; cleartext HTTP is disabled.
- Local file and content access are disabled in WebView.
- Mixed HTTP/HTTPS page content is blocked.
- No JavaScript bridge is exposed to web pages.
- The Ahvaz monitor domain is identified separately as the first planned monitoring connector.

This step provides browsing only. Automatic bus-data extraction, parameter matching, change history, and notifications are not implemented yet. The project targets Android 9 (API 28) and later. A successful Android build/device test has not yet been recorded.


## Multi-bus watchlist milestone

- The App Monitor dashboard tile now opens a dedicated bus-fleet watchlist.
- Users can add and remove any number of bus codes; the list is stored locally on the device.
- Persian and Arabic-Indic numerals are normalized to ASCII digits for matching.
- A reusable matcher can find watched identifiers in visible page text without assuming a fixed fleet size.
- The screen clearly labels each vehicle as waiting for a verified live data source. No fabricated positions, routes, or statuses are displayed.

### Live-data integration status

The public web inspection could not retrieve `https://eta.ahvaz.ir` in this environment. The exact map/network data format, vehicle identifier fields, refresh cadence, and whether all vehicles are exposed to the page therefore remain unverified. The current milestone is a scalable local watchlist and matching foundation, not yet a live fleet tracker. The next integration step is to inspect the site's actual browser network responses or DOM on a device and map the verified fields into per-bus status/history.

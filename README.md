# Sobekhanei Stream — Android MVP

A Kotlin + Jetpack Compose Android starter app using:
- TMDB API v3 for movie/TV metadata, search, posters and details.
- VidAPI embed pages for playback:
  - `https://vidapi.xyz/embed/movie/{tmdbId}`
  - `https://vidapi.xyz/embed/tv/{tmdbTvId}/{season}/{episode}`
- Android WebView for the HTML embed player.
- StreamGuard starter protection for known ad hosts, top-level external navigation, SSL errors and popup windows.

## Important architecture decision

VidAPI's supplied URLs are HTML embed-player pages, not direct `.m3u8`, `.mpd` or MP4 media URLs. Therefore this first MVP uses:
- Native Kotlin/Compose UI for Home, Search and TV episode selection.
- WebView only for the third-party embed player.
- StreamGuard WebView request/navigation filtering.

A true Media3/ExoPlayer-only implementation requires a direct, authorized media manifest/file URL from the provider. It cannot play an arbitrary HTML embed page directly.

## Open in Android Studio

1. Extract the ZIP.
2. Copy `local.properties.example` to `local.properties`.
3. Put a newly generated TMDB v3 API key in `local.properties`:
   `TMDB_API_KEY=...`
4. Open the extracted `SobekhaneiStream-MVP` folder in Android Studio.
5. Allow Gradle Sync, install an Android SDK (compile SDK 35), then Run on an emulator/device with internet access.

This project intentionally does not include the API key supplied in chat.

## TMDB credential safety

The supplied key was shared in chat. Generate a replacement in your TMDB account before using this project. The MVP reads the key from local.properties, but BuildConfig values are packaged into the APK and can be extracted. For a public production release, move TMDB requests behind your own backend proxy and keep the credential server-side.

TMDB's official docs support v3 API key or API Read Access Token authentication. TMDB states free API use is for non-commercial purposes with attribution; a revenue-generating app may be considered commercial and should contact TMDB about licensing. Add the required TMDB attribution in the app's About/Credits screen:
"This product uses the TMDB API but is not endorsed or certified by TMDB."

Official docs:
- https://developer.themoviedb.org/docs/getting-started
- https://developer.themoviedb.org/docs/authentication-application
- https://developer.themoviedb.org/docs/faq

## StreamGuard limitations

The starter engine blocks only the hostnames listed in `app/src/main/assets/streamguard-hosts.txt`, blocks pop-up windows, blocks top-level navigation away from the approved embed hosts, and cancels SSL certificate errors.

Android WebView interception has limits:
- It does not intercept `javascript:` or `blob:` URLs.
- Android documents that `shouldInterceptRequest` sees only the initial URL in a redirect chain.
- Some ads served from the same host/path infrastructure as the player or video segments cannot safely be separated by hostname alone.
- A provider can change its ad hosts at any time.

Do not add `vidapi.xyz` or its required playback/CDN hosts to the blocklist. Doing so can break playback. Build and test a provider-specific allowlist/blocklist from observed requests before enabling aggressive rules.

No claim of 100% Brave-equivalent blocking is made. Test with content and streams you are authorized to use.

## Current MVP features

- Trending Movies and TV tabs.
- Search Movies and TV.
- Poster cards and overview.
- Movie embed playback.
- TV embed playback with season/episode fields.
- Initial provider is VidAPI only; no other player provider is configured.
- Basic StreamGuard filtering and popup/redirect guard.
- No offline downloads.


## Build APK on GitHub (without connecting GitHub to ChatGPT)

This repository includes `.github/workflows/android-build.yml`.

1. Upload the **contents** of this project folder to the root of your GitHub repository (the same level as `settings.gradle.kts`).
2. In GitHub, open **Settings → Secrets and variables → Actions → New repository secret**.
3. Add a secret named `TMDB_API_KEY` with your newly generated TMDB v3 API key. You may omit it to build, but TMDB requests in the installed app will not work until a key is configured.
4. Open **Actions → Build Sobekhanei Stream APK → Run workflow** (or push a commit to `main`).
5. When the workflow finishes successfully, open its run, find **Artifacts**, and download `Sobekhanei-Stream-debug-apk`.
6. Extract the artifact ZIP; it contains `app-debug.apk`.

Do not commit `local.properties` or put API keys in source files. The APK is a debug build for testing, not a signed production release.

### GitHub web upload
From the repository's Code page, choose **Add file → Upload files**, then drag the extracted project files/folders into the upload area. Make sure `.github/workflows/android-build.yml` is included and `settings.gradle.kts` is at repository root. Commit to `main`.

# Sobekhanei Stream — Native Player MVP 0.2.0

Android Kotlin app with a cinematic dark UI, TMDB metadata discovery, and a native AndroidX Media3 / ExoPlayer playback engine.

## Included

- TMDB v3 trending movie and TV metadata, posters, and search.
- Native Media3 player using ExoPlayer.
- HLS (`.m3u8`), DASH (`.mpd`), and progressive media playback support.
- Native PlayerView controls (play/pause, seek, buffering UI, subtitle button).
- A public Mux HLS sample shortcut for checking the native engine in the emulator (test only; not a production content source).
- StreamGuard starter filtering for known advertising hosts on Media3 data requests.
- GitHub Actions debug APK workflow.

## Important: VidAPI source compatibility

The app keeps the requested VidAPI reference URL templates:

- Movie: `https://vidapi.xyz/embed/movie/{id}`
- TV: `https://vidapi.xyz/embed/tv/{id}/{season}/{episode}`

These are HTML embed-player pages, not direct HLS/DASH/MP4 media URLs. Media3 cannot play an HTML page as a video stream. The public VidAPI documentation that could be verified describes these endpoints as iframe/embed URLs and does not document a direct native stream URL endpoint. Confirm the exact `vidapi.xyz` provider's native API with its operator before integrating a resolver.

Until a documented/authorized direct media endpoint is available, the player screen accepts a direct HTTP(S) media URL for testing and native playback. No private player scraping, hidden API reverse-engineering, or access-control bypass is included.

References:
- https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8 (public HLS test sample; availability is controlled by Mux)
- https://developer.android.com/media/implement/playback-app
- https://developer.android.com/jetpack/androidx/releases/media3
- https://vidapi.ru/api (public embed API documentation found; domain may differ from the supplied `vidapi.xyz` host)
- https://vidapi.tw/faq (describes the embed API as iframe-based; domain may differ from the supplied host)

## StreamGuard scope

The native Media3 data-source wrapper checks each requested HTTP(S) media URL against `app/src/main/assets/streamguard-hosts.txt`. It can reject known blocked hostnames for manifests/segments when those requests pass through the configured DataSource. It is not equivalent to Brave's browser-wide content blocker, does not inspect JavaScript, and cannot reliably remove ads stitched into the same host or media manifest. More filter sources and redirect-aware request inspection require further engineering and compatibility testing.

## TMDB key

Generate a fresh TMDB API key because the earlier key was exposed in chat. For local builds, copy `local.properties.example` to `local.properties` and set `TMDB_API_KEY=...`.

For GitHub Actions, add a repository secret named `TMDB_API_KEY` under Settings → Secrets and variables → Actions. The workflow writes this value to `local.properties` only during the build. Do not commit the key or the `local.properties` file.

BuildConfig values are packaged in the APK and can be extracted. A commercial/public release should move TMDB requests behind a backend proxy and confirm TMDB's commercial licensing and attribution requirements. Keep this attribution in the app:

> This product uses the TMDB API but is not endorsed or certified by TMDB.

## Build APK on GitHub

1. Upload the contents of this folder to the repository root (same level as `settings.gradle.kts`).
2. Add the `TMDB_API_KEY` repository secret.
3. Push to `main` or run Actions → Build Sobekhanei Stream APK → Run workflow.
4. Download the `Sobekhanei-Stream-debug-apk` artifact from the successful workflow run.
5. Extract the artifact ZIP to get `app-debug.apk`.

This is a debug build for testing, not a signed production release.

# Sobekhanei Stream — VidAPI Player + StreamGuard (v0.3.0)

Sobekhanei Stream is an Android Kotlin app with a cinematic dark UI, TMDB metadata discovery, and the provider's own HTML embed player shown inside a protected Android WebView.

## Current playback source
- Movie: `https://vidapi.xyz/embed/movie/{tmdb_id}`
- TV: `https://vidapi.xyz/embed/tv/{tmdb_id}/{season}/{episode}`

The app uses the VidAPI player controls (play, pause, seek and the provider's own playback UI). It does not use Media3/ExoPlayer as the active player.

## StreamGuard protections
- Blocks requests to a curated set of known advertising and tracking hostnames.
- Blocks selected ad URL patterns such as pop-under, click-under, ad-server, VAST and tracking endpoints.
- Rejects new browser windows, `_blank` windows and `window.open` pop-under requests.
- Blocks top-level navigation away from the configured embed provider hosts.
- Adds a post-load JavaScript popup guard.
- Uses Android Safe Browsing and rejects SSL certificate errors.
- Denies camera/microphone permissions and disables file/content access.

## Important limitations
No Android WebView blocker can promise that every advertisement will be removed from every third-party embed. Ads served from the same first-party host as the video, inserted directly into the stream, or served from unknown/new domains may still appear. Blocking a provider's ad request may also break playback if the provider requires that request to complete. Test each provider and tune the blocklist carefully.

Player controls are not disabled. The app blocks known popup and navigation behaviours rather than swallowing every screen tap, so normal player interaction remains possible.

## TMDB configuration
The app reads `TMDB_API_KEY` from `local.properties`. GitHub Actions writes the repository secret of the same name into that file at build time. Do not commit `local.properties`. A key packaged into an APK can be extracted; for public release, use a backend proxy and review TMDB's licensing/attribution requirements.

Keep this attribution visible in the app:
> This product uses the TMDB API but is not endorsed or certified by TMDB.

## Build
1. Add the GitHub repository secret `TMDB_API_KEY`.
2. Push to `main` or run Actions → Build Sobekhanei Stream APK → Run workflow.
3. Download `Sobekhanei-Stream-debug-apk` from a successful run and extract `app-debug.apk`.

This is a debug build for testing, not a signed production release.

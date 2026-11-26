# Sobekhanei Stream — StreamGuard / VidAPI WebView update (v0.3.0)

## What this update changes
- Restores the provider's own VidAPI HTML embed player in Android WebView.
- Movie URL: `https://vidapi.xyz/embed/movie/{tmdb_id}`
- TV URL: `https://vidapi.xyz/embed/tv/{tmdb_id}/{season}/{episode}`
- Keeps the provider's own playback controls; the app does not replace the video renderer with Media3.
- Adds a curated hostname blocklist and URL-pattern filtering in `shouldInterceptRequest`.
- Rejects new browser windows (`window.open`, `_blank`, pop-under) through `WebChromeClient.onCreateWindow`.
- Blocks top-level navigation away from the configured embed-provider hosts.
- Injects a small popup guard after the embed page loads.
- Keeps HTML5 media playback, third-party cookies and fullscreen custom-view support for compatibility.
- Increments Android version to `0.3.0`.

## Apply to the existing GitHub Desktop repository
1. Extract this ZIP.
2. Copy the contents of `SobekhaneiStream-Update` into the root of your cloned repository (the folder containing `settings.gradle.kts`).
3. Merge folders and replace the existing files when Windows asks.
4. Delete the old file `app/src/main/java/com/sobekhanei/stream/NativeMediaPlayer.kt` from the repository. This update removes its Media3 dependencies; the active player is `StreamWebView`.
5. In GitHub Desktop, check that these files are changed:
   - `app/src/main/java/com/sobekhanei/stream/MainActivity.kt`
   - `app/src/main/java/com/sobekhanei/stream/StreamGuardWebView.kt`
   - `app/src/main/assets/streamguard-hosts.txt`
   - `app/build.gradle.kts`
   - deletion of `app/src/main/java/com/sobekhanei/stream/NativeMediaPlayer.kt`
6. Commit with: `Restore VidAPI player and strengthen StreamGuard`
7. Push to `main`, then check GitHub Actions and download the new debug APK artifact.

## Important limitations
- No client-side Android blocker can guarantee that every ad is removed from every third-party player.
- The app blocks known ad/tracker hostnames, common ad URL patterns, popup windows and off-provider top-level redirects.
- Ads served from the same first-party host as the video, ads stitched into the video stream, or unknown/new ad domains may still appear.
- Some providers require ad calls to complete before playback. Blocking those calls can sometimes break playback; test each provider and adjust the list carefully.
- Player buttons and touch input are intentionally preserved. The app does not disable all taps on the video area.
- This update has not been built or playback-tested in the user's MEmu emulator yet. Run the GitHub Actions build and test movie + TV playback after installing the artifact.

## TMDB
The existing GitHub Actions workflow reads the repository secret named `TMDB_API_KEY` into `local.properties` at build time. Keep that secret in GitHub Settings → Secrets and variables → Actions. Never commit `local.properties`.

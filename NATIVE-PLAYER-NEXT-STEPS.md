# Native Player — Provider Integration Gate

## Current state

- Media3 / ExoPlayer engine is wired into the app.
- HLS, DASH and progressive media modules are included.
- The old WebView player screen has been removed from the app path.
- The requested VidAPI URL is displayed as a provider reference only.
- A direct HTTP(S) stream URL can be pasted into the Player screen to exercise native playback.

## Required to make VidAPI titles play natively

Obtain from the VidAPI operator, in writing or official documentation:

1. A supported API endpoint that resolves a TMDB movie/TV episode to a direct HLS, DASH, or MP4 URL.
2. Required request headers, referer/origin requirements, auth/token process, and expiry behavior.
3. Whether native app clients are permitted and whether the service has a native/mobile SDK.
4. Subtitle/audio track metadata and any DRM scheme/license server details, if applicable.

Do not treat the HTML embed URL as a media URL. Do not scrape private player internals or bypass provider access controls. Once the provider supplies a supported endpoint, implement `StreamSourceResolver` to return a `ResolvedMediaSource` and pass it to `NativeMediaPlayer`.

## Verification still required

- GitHub Actions compile with Media3 dependencies.
- Emulator playback with a permitted HLS sample URL.
- Emulator playback with a permitted DASH sample URL.
- StreamGuard request filtering and false-positive checks.
- Provider integration after official native API access is confirmed.

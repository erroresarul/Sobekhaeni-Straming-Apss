package com.sobekhanei.stream

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem as PlayerMediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import java.io.IOException

/** Native Media3/ExoPlayer surface. It accepts direct media URLs, not HTML iframe URLs. */
@OptIn(UnstableApi::class)
@Composable
fun NativeMediaPlayer(
    url: String,
    modifier: Modifier = Modifier,
    onError: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnError = rememberUpdatedState(onError)

    val exoPlayer = remember(url) {
        val guardedFactory = StreamGuardDataSource.create(context)
        val mediaSourceFactory = DefaultMediaSourceFactory(guardedFactory)
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()
            .apply {
                val path = Uri.parse(url).path.orEmpty().lowercase()
                val mimeType = when {
                    path.endsWith(".m3u8") -> MimeTypes.APPLICATION_M3U8
                    path.endsWith(".mpd") -> MimeTypes.APPLICATION_MPD
                    else -> null
                }
                val mediaItem = PlayerMediaItem.Builder()
                    .setUri(url)
                    .apply { if (mimeType != null) setMimeType(mimeType) }
                    .build()
                setMediaItem(mediaItem)
                playWhenReady = true
                prepare()
            }
    }

    DisposableEffect(exoPlayer, lifecycleOwner) {
        var resumeAfterPause = false
        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                currentOnError.value(error.message ?: "The stream could not be played.")
            }
        }
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    resumeAfterPause = exoPlayer.playWhenReady
                    exoPlayer.pause()
                }
                Lifecycle.Event.ON_RESUME -> {
                    if (resumeAfterPause) exoPlayer.play()
                    resumeAfterPause = false
                }
                else -> Unit
            }
        }
        exoPlayer.addListener(listener)
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { viewContext ->
            PlayerView(viewContext).apply {
                player = exoPlayer
                useController = true
                controllerAutoShow = true
                controllerHideOnTouch = true
                controllerShowTimeoutMs = 5_000
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                setShowSubtitleButton(true)
                setKeepContentOnPlayerReset(true)
                keepScreenOn = true
            }
        },
        update = { playerView ->
            playerView.player = exoPlayer
        },
        onRelease = { playerView ->
            playerView.player = null
            playerView.keepScreenOn = false
        }
    )
}

/**
 * Starter request filter for native media loads. It checks the initial URL of each
 * Media3 DataSpec (including HLS/DASH manifests and segments). It is not a browser-wide
 * ad blocker and cannot remove ads stitched into the same media stream/manifest.
 */
@OptIn(UnstableApi::class)
private object StreamGuardDataSource {
    fun create(context: Context): DataSource.Factory {
        val blockedHosts = context.assets.open("streamguard-hosts.txt").bufferedReader().useLines { lines ->
            lines.map { it.substringBefore("#").trim().lowercase() }
                .filter { it.isNotBlank() }
                .toSet()
        }

        val httpFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("SobekhaneiStream/0.2.0 (Android Media3)")
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(30_000)

        val upstream = DefaultDataSource.Factory(context, httpFactory)
        return ResolvingDataSource.Factory(
            upstream,
            object : ResolvingDataSource.Resolver {
                override fun resolveDataSpec(dataSpec: DataSpec): DataSpec {
                    val uri = dataSpec.uri
                    val scheme = uri.scheme?.lowercase()
                    if (scheme != "http" && scheme != "https") {
                        throw IOException("StreamGuard: only HTTP/HTTPS media sources are allowed.")
                    }
                    val host = uri.host?.lowercase().orEmpty()
                    val blocked = blockedHosts.any { host == it || host.endsWith(".$it") }
                    if (blocked) {
                        throw IOException("StreamGuard blocked a known advertising host: $host")
                    }
                    return dataSpec
                }
            }
        )
    }
}

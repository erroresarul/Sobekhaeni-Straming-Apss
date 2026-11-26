package com.sobekhanei.stream

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

private val Night = Color(0xFF090B12)
private val Panel = Color(0xFF151A27)
private val Accent = Color(0xFF8B5CF6)
private val Muted = Color(0xFF9CA3AF)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { StreamApp() }
    }
}

@Composable
private fun StreamApp() {
    val repository = remember { TmdbRepository() }
    var mediaType by remember { mutableStateOf("movie") }
    var query by remember { mutableStateOf("") }
    var items by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var selected by remember { mutableStateOf<MediaItem?>(null) }

    suspend fun loadContent(search: String? = null) {
        loading = true
        error = null
        try {
            items = if (search.isNullOrBlank()) repository.trending(mediaType)
                    else repository.search(mediaType, search)
        } catch (e: Exception) {
            error = e.message ?: "Could not load content. Check your TMDB key and internet connection."
        } finally {
            loading = false
        }
    }

    LaunchedEffect(mediaType) { loadContent() }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Accent,
            background = Night,
            surface = Panel,
            onBackground = Color.White,
            onSurface = Color.White
        )
    ) {
        selected?.let { item ->
            PlayerScreen(item = item, onBack = { selected = null })
        } ?: HomeScreen(
            mediaType = mediaType,
            onMediaType = {
                mediaType = it
                query = ""
            },
            query = query,
            onQuery = { query = it },
            items = items,
            loading = loading,
            error = error,
            onSelect = { selected = it },
            load = { text -> loadContent(text) }
        )
    }
}

@Composable
private fun HomeScreen(
    mediaType: String,
    onMediaType: (String) -> Unit,
    query: String,
    onQuery: (String) -> Unit,
    items: List<MediaItem>,
    loading: Boolean,
    error: String?,
    onSelect: (MediaItem) -> Unit,
    load: suspend (String?) -> Unit
) {
    val scope = rememberCoroutineScope()
    Column(
        modifier = Modifier.fillMaxSize().background(Night).statusBarsPadding()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("SOBEKHANEI", color = Accent, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Text("STREAM", color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
            }
            Icon(Icons.Default.Shield, contentDescription = "StreamGuard", tint = Color(0xFF34D399), modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(6.dp))
            Text("Protected", color = Color(0xFF34D399), style = MaterialTheme.typography.labelSmall)
        }

        OutlinedTextField(
            value = query,
            onValueChange = onQuery,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("Search movies or TV shows…", color = Muted) },
            trailingIcon = {
                IconButton(onClick = { scope.launch { load(query) } }) {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Accent,
                unfocusedBorderColor = Color(0xFF343B4D),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = Accent
            )
        )
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = mediaType == "movie",
                onClick = { onMediaType("movie") },
                label = { Text("Movies") }
            )
            FilterChip(
                selected = mediaType == "tv",
                onClick = { onMediaType("tv") },
                label = { Text("TV Series") }
            )
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { scope.launch { load(null) } }) { Text("Trending") }
        }
        Spacer(Modifier.height(8.dp))
        when {
            loading -> Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Accent)
            }
            error != null -> Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text(error, color = Color(0xFFFCA5A5))
            }
            items.isEmpty() -> Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text("No titles found.", color = Muted)
            }
            else -> LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 145.dp),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(items, key = { "${it.mediaType}-${it.id}" }) { item ->
                    MediaCard(item = item, onClick = { onSelect(item) })
                }
            }
        }
        Text(
            "This product uses the TMDB API but is not endorsed or certified by TMDB.",
            color = Muted,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(vertical = 8.dp)
        )
    }
}

@Composable
private fun MediaCard(item: MediaItem, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = Panel,
        shape = RoundedCornerShape(14.dp)
    ) {
        Column {
            AsyncImage(
                model = item.posterUrl,
                contentDescription = item.title,
                modifier = Modifier.fillMaxWidth().height(215.dp),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )
            Column(Modifier.padding(10.dp)) {
                Text(item.title, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(item.year ?: item.mediaType.uppercase(), color = Muted, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun PlayerScreen(item: MediaItem, onBack: () -> Unit) {
    var season by remember(item.id) { mutableStateOf("1") }
    var episode by remember(item.id) { mutableStateOf("1") }
    var directStreamUrl by remember(item.id) { mutableStateOf("") }
    var activeStreamUrl by remember(item.id) { mutableStateOf<String?>(null) }
    var inputError by remember(item.id) { mutableStateOf<String?>(null) }
    var playbackError by remember(item.id) { mutableStateOf<String?>(null) }

    val providerEmbedReference = remember(item.id, item.mediaType, season, episode) {
        if (item.mediaType == "movie") {
            "https://vidapi.xyz/embed/movie/${item.id}"
        } else {
            "https://vidapi.xyz/embed/tv/${item.id}/${season.ifBlank { "1" }}/${episode.ifBlank { "1" }}"
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Night)
            .statusBarsPadding()
            .imePadding()
            .verticalScroll(androidx.compose.foundation.rememberScrollState())
            .padding(horizontal = 14.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Column(Modifier.weight(1f)) {
                Text(
                    item.title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text("Native Media3 Player", color = Color(0xFF34D399), style = MaterialTheme.typography.labelSmall)
            }
            Icon(Icons.Default.Shield, contentDescription = "StreamGuard", tint = Color(0xFF34D399))
        }

        if (item.mediaType == "tv") {
            Text("Select episode", color = Color.White, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = season,
                    onValueChange = { season = it.filter(Char::isDigit).take(2) },
                    label = { Text("Season") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = episode,
                    onValueChange = { episode = it.filter(Char::isDigit).take(3) },
                    label = { Text("Episode") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }
            Spacer(Modifier.height(12.dp))
        }

        Text("SOBEKHANEI NATIVE PLAYER", color = Accent, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))

        if (activeStreamUrl != null) {
            NativeMediaPlayer(
                url = activeStreamUrl!!,
                modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
                onError = { playbackError = it }
            )
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = {
                activeStreamUrl = null
                playbackError = null
            }) { Text("Stop playback") }
        } else {
            Surface(
                modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
                color = Color(0xFF05070C),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.Shield, contentDescription = null, tint = Accent, modifier = Modifier.size(38.dp))
                    Spacer(Modifier.height(10.dp))
                    Text("Native player is ready", color = Color.White, fontWeight = FontWeight.Bold)
                    Text(
                        "Waiting for a direct video stream URL",
                        color = Muted,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        playbackError?.let {
            Spacer(Modifier.height(8.dp))
            Text("Playback error: $it", color = Color(0xFFFCA5A5), style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.height(18.dp))
        Text("Direct stream URL", color = Color.White, fontWeight = FontWeight.SemiBold)
        Text(
            "Paste a stream URL you are authorized to play (.m3u8, .mpd, or MP4). The native player cannot play an HTML embed page.",
            color = Muted,
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = directStreamUrl,
            onValueChange = {
                directStreamUrl = it.trim()
                inputError = null
            },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("https://example.com/video.m3u8") },
            singleLine = false,
            minLines = 2,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Accent,
                unfocusedBorderColor = Color(0xFF343B4D),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = Accent
            )
        )
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = {
                val parsed = android.net.Uri.parse(directStreamUrl)
                if ((parsed.scheme == "https" || parsed.scheme == "http") && !parsed.host.isNullOrBlank()) {
                    inputError = null
                    playbackError = null
                    activeStreamUrl = directStreamUrl
                } else {
                    inputError = "Enter a valid HTTP or HTTPS direct media URL."
                }
            },
            enabled = directStreamUrl.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Accent)
        ) {
            Text("Play with Native Player")
        }
        inputError?.let { Text(it, color = Color(0xFFFCA5A5), style = MaterialTheme.typography.bodySmall) }
        TextButton(
            onClick = {
                val demoUrl = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"
                directStreamUrl = demoUrl
                inputError = null
                playbackError = null
                activeStreamUrl = demoUrl
            },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("Test Native Player with public HLS sample")
        }

        Spacer(Modifier.height(18.dp))
        Surface(color = Panel, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp)) {
                Text("VidAPI provider reference", color = Color.White, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                Text(providerEmbedReference, color = Accent, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
                Text(
                    "This is an HTML embed-player URL, not a direct HLS/DASH/MP4 stream. VidAPI's public embed documentation describes iframe-based playback; a documented native stream endpoint or provider-authorized resolver is still required to connect this title to Media3.",
                    color = Muted,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        Spacer(Modifier.height(20.dp))
    }
}

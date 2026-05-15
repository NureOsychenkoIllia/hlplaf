package com.example.lab3

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.media.MediaPlayer
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject

private const val PREFS_NAME = "music_stream_state"
private const val KEY_USER = "user"
private const val KEY_PLAYLISTS = "playlists"
private const val KEY_REVIEWS = "reviews"
private const val KEY_ANALYTICS = "analytics"

data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val genre: String,
    val durationSeconds: Int,
    val assetPath: String,
)

data class Playlist(
    val id: String,
    val name: String,
    val trackIds: List<String>,
)

data class UserProfile(
    val username: String,
    val displayName: String,
    val bio: String,
)

data class Review(
    val id: String,
    val trackId: String,
    val author: String,
    val rating: Int,
    val text: String,
)

data class MediaAnalytics(
    val plays: Map<String, Int> = emptyMap(),
    val downloads: Map<String, Int> = emptyMap(),
) {
    fun totalPlays(): Int = plays.values.sum()
    fun totalDownloads(): Int = downloads.values.sum()
}

enum class Screen(val title: String) {
    Library("Каталог"),
    Playlists("Плейлисти"),
    Profile("Профіль"),
    Analytics("Аналітика"),
}

data class MusicState(
    val tracks: List<Track> = seedTracks,
    val user: UserProfile = UserProfile(
        username = "listener",
        displayName = "First Listener",
        bio = "Слухаю музику",
    ),
    val playlists: List<Playlist> = listOf(
        Playlist("later", "Прослухати пізніше", listOf("1", "7")),
        Playlist("focus", "Focus Mode", listOf("2", "5")),
    ),
    val reviews: List<Review> = listOf(
        Review("r1", "1", "alice", 5, "Ідеальний трек для нічної дороги."),
        Review("r2", "2", "bob", 4, "Добре працює як фон для навчання."),
    ),
    val analytics: MediaAnalytics = MediaAnalytics(),
    val query: String = "",
    val selectedGenre: String? = null,
    val selectedArtist: String? = null,
    val selectedAlbum: String? = null,
    val currentTrackId: String? = null,
    val playbackPositionSeconds: Int = 0,
    val isPlaying: Boolean = false,
    val playbackError: String? = null,
    val selectedTrackId: String? = null,
    val reviewText: String = "",
    val reviewRating: Int = 5,
)

class MusicViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs: SharedPreferences =
        application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private var mediaPlayer: MediaPlayer? = null

    var state by mutableStateOf(loadPersistentState())
        private set

    val filteredTracks: List<Track>
        get() = state.tracks.filter { track ->
            val queryMatch = state.query.isBlank() ||
                track.title.contains(state.query, ignoreCase = true) ||
                track.artist.contains(state.query, ignoreCase = true) ||
                track.album.contains(state.query, ignoreCase = true)
            val genreMatch = state.selectedGenre == null || track.genre == state.selectedGenre
            val artistMatch = state.selectedArtist == null || track.artist == state.selectedArtist
            val albumMatch = state.selectedAlbum == null || track.album == state.selectedAlbum
            queryMatch && genreMatch && artistMatch && albumMatch
        }

    fun setScreenTrack(trackId: String?) {
        state = state.copy(selectedTrackId = trackId, reviewText = "", reviewRating = 5)
    }

    fun toggleReviewPanel(trackId: String) {
        val nextTrackId = if (state.selectedTrackId == trackId) null else trackId
        setScreenTrack(nextTrackId)
    }

    fun updateQuery(value: String) {
        state = state.copy(query = value)
    }

    fun selectGenre(value: String?) {
        state = state.copy(selectedGenre = value)
    }

    fun selectArtist(value: String?) {
        state = state.copy(selectedArtist = value)
    }

    fun selectAlbum(value: String?) {
        state = state.copy(selectedAlbum = value)
    }

    fun resetFilters() {
        state = state.copy(query = "", selectedGenre = null, selectedArtist = null, selectedAlbum = null)
    }

    fun play(trackId: String) {
        val track = state.tracks.findById(trackId) ?: return
        if (!startMedia(track)) return
        state = state.copy(
            currentTrackId = trackId,
            playbackPositionSeconds = 0,
            isPlaying = true,
            playbackError = null,
            analytics = state.analytics.copy(
                plays = state.analytics.plays.plus(trackId to ((state.analytics.plays[trackId] ?: 0) + 1)),
            ),
        )
        persistState()
    }

    fun playNext(queue: List<Track>) {
        switchTrack(queue, step = 1)
    }

    fun playPrevious(queue: List<Track>) {
        switchTrack(queue, step = -1)
    }

    private fun switchTrack(queue: List<Track>, step: Int) {
        if (queue.isEmpty()) return
        val currentIndex = queue.indexOfFirst { it.id == state.currentTrackId }.takeIf { it >= 0 } ?: 0
        val nextIndex = Math.floorMod(currentIndex + step, queue.size)
        play(queue[nextIndex].id)
    }

    fun togglePause() {
        if (state.currentTrackId == null) return
        if (state.isPlaying) {
            mediaPlayer?.pause()
            state = state.copy(isPlaying = false)
        } else {
            mediaPlayer?.start()
            state = state.copy(isPlaying = true)
        }
    }

    fun stop() {
        releasePlayer()
        state = state.copy(currentTrackId = null, playbackPositionSeconds = 0, isPlaying = false)
    }

    fun tickPlayback() {
        if (!state.isPlaying) return
        val track = state.currentTrackId?.let(state.tracks::findById) ?: return
        val nextPosition = mediaPlayer?.currentPosition?.div(1_000) ?: (state.playbackPositionSeconds + 1)
        state = if (nextPosition >= track.durationSeconds) {
            releasePlayer()
            state.copy(currentTrackId = null, playbackPositionSeconds = 0, isPlaying = false)
        } else {
            state.copy(playbackPositionSeconds = nextPosition)
        }
    }

    fun download(trackId: String) {
        state = state.copy(
            analytics = state.analytics.copy(
                downloads = state.analytics.downloads.plus(trackId to ((state.analytics.downloads[trackId] ?: 0) + 1)),
            ),
        )
        persistState()
    }

    fun toggleLater(trackId: String) {
        state = state.copy(playlists = state.playlists.map { playlist ->
            if (playlist.id != "later") playlist else playlist.copy(trackIds = playlist.trackIds.toggle(trackId))
        })
        persistState()
    }

    fun createPlaylist(name: String) {
        val cleanName = name.trim()
        if (cleanName.isEmpty()) return
        val playlist = Playlist(
            id = cleanName.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-').ifEmpty { "playlist" },
            name = cleanName,
            trackIds = emptyList(),
        )
        state = state.copy(playlists = state.playlists + playlist)
        persistState()
    }

    fun togglePlaylistTrack(playlistId: String, trackId: String) {
        state = state.copy(playlists = state.playlists.map { playlist ->
            if (playlist.id != playlistId) playlist else playlist.copy(trackIds = playlist.trackIds.toggle(trackId))
        })
        persistState()
    }

    fun deletePlaylist(playlistId: String) {
        if (playlistId == "later") return
        state = state.copy(playlists = state.playlists.filterNot { it.id == playlistId })
        persistState()
    }

    fun updateProfile(displayName: String, bio: String) {
        state = state.copy(user = state.user.copy(displayName = displayName.trim(), bio = bio.trim()))
        persistState()
    }

    fun updateReviewDraft(text: String, rating: Int) {
        state = state.copy(reviewText = text, reviewRating = rating.coerceIn(1, 5))
    }

    fun saveReview(trackId: String) {
        val text = state.reviewText.trim()
        if (text.isEmpty()) return
        val existing = state.reviews.firstOrNull { it.trackId == trackId && it.author == state.user.username }
        val review = Review(
            id = existing?.id ?: "r${state.reviews.size + 1}",
            trackId = trackId,
            author = state.user.username,
            rating = state.reviewRating,
            text = text,
        )
        state = state.copy(
            reviews = state.reviews.filterNot { it.id == review.id } + review,
            reviewText = "",
            reviewRating = 5,
        )
        persistState()
    }

    private fun List<String>.toggle(value: String): List<String> =
        if (value in this) filterNot { it == value } else this + value

    private fun loadPersistentState(): MusicState {
        val defaults = MusicState()
        return try {
            defaults.copy(
                user = prefs.getString(KEY_USER, null)?.let(::decodeUser) ?: defaults.user,
                playlists = prefs.getString(KEY_PLAYLISTS, null)?.let(::decodePlaylists) ?: defaults.playlists,
                reviews = prefs.getString(KEY_REVIEWS, null)?.let(::decodeReviews) ?: defaults.reviews,
                analytics = prefs.getString(KEY_ANALYTICS, null)?.let(::decodeAnalytics) ?: defaults.analytics,
            )
        } catch (_: Exception) {
            defaults
        }
    }

    private fun persistState() {
        prefs.edit()
            .putString(KEY_USER, encodeUser(state.user).toString())
            .putString(KEY_PLAYLISTS, encodePlaylists(state.playlists).toString())
            .putString(KEY_REVIEWS, encodeReviews(state.reviews).toString())
            .putString(KEY_ANALYTICS, encodeAnalytics(state.analytics).toString())
            .apply()
    }

    private fun encodeUser(user: UserProfile): JSONObject = JSONObject()
        .put("username", user.username)
        .put("displayName", user.displayName)
        .put("bio", user.bio)

    private fun decodeUser(raw: String): UserProfile {
        val json = JSONObject(raw)
        return UserProfile(
            username = json.optString("username", "listener"),
            displayName = json.optString("displayName", "First Listener"),
            bio = json.optString("bio", "Слухаю музику"),
        )
    }

    private fun encodePlaylists(playlists: List<Playlist>): JSONArray = JSONArray().apply {
        playlists.forEach { playlist ->
            put(JSONObject()
                .put("id", playlist.id)
                .put("name", playlist.name)
                .put("trackIds", JSONArray().apply { playlist.trackIds.forEach { trackId -> put(trackId) } }))
        }
    }

    private fun decodePlaylists(raw: String): List<Playlist> {
        val json = JSONArray(raw)
        return List(json.length()) { index ->
            val item = json.getJSONObject(index)
            val trackIds = item.getJSONArray("trackIds")
            Playlist(
                id = item.getString("id"),
                name = item.getString("name"),
                trackIds = List(trackIds.length()) { trackIds.getString(it) },
            )
        }
    }

    private fun encodeReviews(reviews: List<Review>): JSONArray = JSONArray().apply {
        reviews.forEach { review ->
            put(JSONObject()
                .put("id", review.id)
                .put("trackId", review.trackId)
                .put("author", review.author)
                .put("rating", review.rating)
                .put("text", review.text))
        }
    }

    private fun decodeReviews(raw: String): List<Review> {
        val json = JSONArray(raw)
        return List(json.length()) { index ->
            val item = json.getJSONObject(index)
            Review(
                id = item.getString("id"),
                trackId = item.getString("trackId"),
                author = item.getString("author"),
                rating = item.getInt("rating"),
                text = item.optString("text", ""),
            )
        }
    }

    private fun encodeAnalytics(analytics: MediaAnalytics): JSONObject = JSONObject()
        .put("plays", encodeIntMap(analytics.plays))
        .put("downloads", encodeIntMap(analytics.downloads))

    private fun decodeAnalytics(raw: String): MediaAnalytics {
        val json = JSONObject(raw)
        return MediaAnalytics(
            plays = decodeIntMap(json.optJSONObject("plays") ?: JSONObject()),
            downloads = decodeIntMap(json.optJSONObject("downloads") ?: JSONObject()),
        )
    }

    private fun encodeIntMap(values: Map<String, Int>): JSONObject = JSONObject().apply {
        values.forEach { (key, value) -> put(key, value) }
    }

    private fun decodeIntMap(json: JSONObject): Map<String, Int> {
        val result = mutableMapOf<String, Int>()
        val keys = json.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            result[key] = json.optInt(key)
        }
        return result
    }

    private fun startMedia(track: Track): Boolean {
        return try {
            releasePlayer()
            val asset = getApplication<Application>().assets.openFd(track.assetPath)
            mediaPlayer = MediaPlayer().apply {
                setDataSource(asset.fileDescriptor, asset.startOffset, asset.length)
                setOnCompletionListener {
                    state = state.copy(currentTrackId = null, playbackPositionSeconds = 0, isPlaying = false)
                    releasePlayer()
                }
                prepare()
                start()
            }
            asset.close()
            true
        } catch (error: Exception) {
            releasePlayer()
            state = state.copy(
                currentTrackId = null,
                playbackPositionSeconds = 0,
                isPlaying = false,
                playbackError = "Не вдалося відтворити ${track.title}: ${error.message ?: error::class.java.simpleName}",
            )
            false
        }
    }

    private fun releasePlayer() {
        mediaPlayer?.release()
        mediaPlayer = null
    }

    override fun onCleared() {
        releasePlayer()
        super.onCleared()
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MusicApp() }
    }
}

@Composable
fun MusicApp(viewModel: MusicViewModel = viewModel()) {
    var screen by remember { mutableStateOf(Screen.Library) }
    val currentTrackId = viewModel.state.currentTrackId
    val isPlaying = viewModel.state.isPlaying

    LaunchedEffect(currentTrackId, isPlaying) {
        while (currentTrackId != null && isPlaying) {
            delay(1_000)
            viewModel.tickPlayback()
        }
    }

    MaterialTheme(colorScheme = darkMusicColorScheme()) {
        Scaffold(
            bottomBar = {
                Column {
                    MainstreamPlayerBar(
                        currentTrack = viewModel.state.currentTrackId?.let(viewModel.state.tracks::findById),
                        positionSeconds = viewModel.state.playbackPositionSeconds,
                        isPlaying = viewModel.state.isPlaying,
                        playbackError = viewModel.state.playbackError,
                        canSwitch = viewModel.filteredTracks.size > 1,
                        onPrevious = { viewModel.playPrevious(viewModel.filteredTracks) },
                        onNext = { viewModel.playNext(viewModel.filteredTracks) },
                        onTogglePause = viewModel::togglePause,
                        onStop = viewModel::stop,
                    )
                    BottomTabs(selected = screen, onSelect = { screen = it })
                }
            },
            containerColor = Color.Transparent,
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF171720), Color(0xFF0F0F13), Color(0xFF11131A)),
                        ),
                    )
                    .padding(padding),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Header(screen.title)
                    when (screen) {
                        Screen.Library -> LibraryScreen(viewModel)
                        Screen.Playlists -> PlaylistsScreen(viewModel)
                        Screen.Profile -> ProfileScreen(viewModel)
                        Screen.Analytics -> AnalyticsScreen(viewModel)
                    }
                }
            }
        }
    }
}

@Composable
private fun Header(title: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Lab 3 / Music Streaming", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        Text(title, color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun MainstreamPlayerBar(
    currentTrack: Track?,
    positionSeconds: Int,
    isPlaying: Boolean,
    playbackError: String?,
    canSwitch: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onTogglePause: () -> Unit,
    onStop: () -> Unit,
) {
    AnimatedVisibility(currentTrack != null) {
        currentTrack?.let { track ->
            val progress = (positionSeconds.toFloat() / track.durationSeconds).coerceIn(0f, 1f)
            Surface(color = Color(0xF50A0A0D), shadowElevation = 14.dp) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Brush.linearGradient(listOf(Accent, Color(0xFF35D0BA)))),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(track.artist.first().toString(), color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            track.title,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(track.artist, color = Muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    TextButton(
                        onClick = onPrevious,
                        enabled = canSwitch,
                        contentPadding = PaddingValues(4.dp),
                    ) {
                        Text("‹", color = if (canSwitch) Color.White else Muted, fontSize = 30.sp)
                    }
                    Button(
                        onClick = onTogglePause,
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.size(42.dp),
                    ) {
                        Text(if (isPlaying) "Ⅱ" else "▶", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                    TextButton(
                        onClick = onNext,
                        enabled = canSwitch,
                        contentPadding = PaddingValues(4.dp),
                    ) {
                        Text("›", color = if (canSwitch) Color.White else Muted, fontSize = 30.sp)
                    }
                    Button(
                        onClick = onStop,
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceB, contentColor = Color.White),
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.size(36.dp),
                    ) {
                        Text("■", fontSize = 12.sp)
                    }
                }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(positionSeconds.formatDuration(), color = Muted, fontSize = 11.sp)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(3.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(Color(0xFF4A4A55)),
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(progress)
                                    .height(3.dp)
                                    .background(Color.White),
                            )
                        }
                        Text(track.durationSeconds.formatDuration(), color = Muted, fontSize = 11.sp)
                    }
                }
            }
        }
    }
    AnimatedVisibility(currentTrack == null && playbackError != null) {
        Surface(color = Color(0xF50A0A0D), shadowElevation = 14.dp) {
            Text(
                playbackError.orEmpty(),
                color = Color(0xFFFF9E9E),
                fontSize = 12.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LibraryScreen(viewModel: MusicViewModel) {
    val state = viewModel.state
    val tracks = viewModel.filteredTracks

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        item {
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::updateQuery,
                label = { Text("Пошук за треком, виконавцем або альбомом") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        item {
            FilterBlock("Жанр", state.tracks.map { it.genre }.distinct(), state.selectedGenre, viewModel::selectGenre)
            FilterBlock("Виконавець", state.tracks.map { it.artist }.distinct(), state.selectedArtist, viewModel::selectArtist)
            FilterBlock("Альбом", state.tracks.map { it.album }.distinct(), state.selectedAlbum, viewModel::selectAlbum)
            TextButton(onClick = viewModel::resetFilters) {
                Text("Скинути фільтри")
            }
        }

        item {
            Text("${tracks.size} треків знайдено", color = Muted, fontSize = 13.sp)
        }

        items(tracks, key = { it.id }) { track ->
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                TrackCard(
                    track = track,
                    isPlaying = state.currentTrackId == track.id,
                    isLater = state.playlists.first { it.id == "later" }.trackIds.contains(track.id),
                    reviewsOpen = state.selectedTrackId == track.id,
                    onPlay = { viewModel.play(track.id) },
                    onDownload = { viewModel.download(track.id) },
                    onToggleLater = { viewModel.toggleLater(track.id) },
                    onReviews = { viewModel.toggleReviewPanel(track.id) },
                )
                if (state.selectedTrackId == track.id) {
                    ReviewPanel(viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterBlock(
    title: String,
    options: List<String>,
    selected: String?,
    onSelect: (String?) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 10.dp)) {
        Text(title, color = Muted, fontSize = 12.sp)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SmallChip("Усі", selected == null) { onSelect(null) }
            options.forEach { option ->
                SmallChip(option, selected == option) { onSelect(option) }
            }
        }
    }
}

@Composable
private fun TrackCard(
    track: Track,
    isPlaying: Boolean,
    isLater: Boolean,
    reviewsOpen: Boolean,
    onPlay: () -> Unit,
    onDownload: () -> Unit,
    onToggleLater: () -> Unit,
    onReviews: () -> Unit,
) {
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(containerColor = SurfaceA),
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Brush.linearGradient(listOf(Accent, Color(0xFF35D0BA)))),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(track.artist.first().toString(), color = Color.White, fontWeight = FontWeight.Bold)
                }
                Column(Modifier.weight(1f)) {
                    Text(track.title, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${track.artist} - ${track.album}", color = Muted, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${track.genre} / ${track.durationSeconds.formatDuration()}", color = Muted, fontSize = 12.sp)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(onClick = onPlay, colors = ButtonDefaults.buttonColors(containerColor = if (isPlaying) Color(0xFF35D0BA) else Accent)) {
                    Text(if (isPlaying) "Грає" else "Слухати")
                }
                OutlinedButton(onClick = onDownload) { Text("Завантажити") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onToggleLater) {
                    Text(if (isLater) "Прибрати з later" else "Прослухати пізніше")
                }
                TextButton(onClick = onReviews) { Text(if (reviewsOpen) "Сховати відгуки" else "Відгуки") }
            }
        }
    }
}

@Composable
private fun ReviewPanel(viewModel: MusicViewModel) {
    val state = viewModel.state
    val track = state.selectedTrackId?.let(state.tracks::findById) ?: return
    val reviews = state.reviews.filter { it.trackId == track.id }

    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(containerColor = SurfaceB),
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Відгуки: ${track.title}", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                TextButton(onClick = { viewModel.setScreenTrack(null) }) { Text("Закрити") }
            }
            reviews.forEach { review ->
                Text("${review.author}: ${"★".repeat(review.rating)} ${review.text}", color = Muted, fontSize = 13.sp)
            }
            OutlinedTextField(
                value = state.reviewText,
                onValueChange = { viewModel.updateReviewDraft(it, state.reviewRating) },
                label = { Text("Ваш відгук") },
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Оцінка", color = Muted)
                (1..5).forEach { rating ->
                    SmallChip(rating.toString(), state.reviewRating == rating) {
                        viewModel.updateReviewDraft(state.reviewText, rating)
                    }
                }
                Spacer(Modifier.weight(1f))
                Button(onClick = { viewModel.saveReview(track.id) }) { Text("Зберегти") }
            }
        }
    }
}

@Composable
private fun PlaylistsScreen(viewModel: MusicViewModel) {
    val state = viewModel.state
    var newPlaylistName by remember { mutableStateOf("") }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            ElevatedCard(colors = CardDefaults.elevatedCardColors(containerColor = SurfaceA), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Новий плейлист", color = Color.White, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = newPlaylistName,
                        onValueChange = { newPlaylistName = it },
                        label = { Text("Назва") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Button(onClick = {
                        viewModel.createPlaylist(newPlaylistName)
                        newPlaylistName = ""
                    }) {
                        Text("Створити")
                    }
                }
            }
        }

        items(state.playlists, key = { it.id }) { playlist ->
            PlaylistCard(
                playlist = playlist,
                tracks = state.tracks,
                onToggle = { trackId -> viewModel.togglePlaylistTrack(playlist.id, trackId) },
                onDelete = { viewModel.deletePlaylist(playlist.id) },
            )
        }
    }
}

@Composable
private fun PlaylistCard(
    playlist: Playlist,
    tracks: List<Track>,
    onToggle: (String) -> Unit,
    onDelete: () -> Unit,
) {
    ElevatedCard(colors = CardDefaults.elevatedCardColors(containerColor = SurfaceA), shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(playlist.name, color = Color.White, fontWeight = FontWeight.Bold)
                    Text("${playlist.trackIds.size} треків", color = Muted, fontSize = 12.sp)
                }
                if (playlist.id != "later") {
                    TextButton(onClick = onDelete) { Text("Видалити") }
                }
            }
            tracks.forEach { track ->
                val selected = track.id in playlist.trackIds
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("${track.artist} - ${track.title}", color = if (selected) Color.White else Muted, modifier = Modifier.weight(1f), maxLines = 1)
                    SmallChip(if (selected) "У списку" else "Додати", selected) { onToggle(track.id) }
                }
            }
        }
    }
}

@Composable
private fun ProfileScreen(viewModel: MusicViewModel) {
    val user = viewModel.state.user
    var displayName by remember(user.displayName) { mutableStateOf(user.displayName) }
    var bio by remember(user.bio) { mutableStateOf(user.bio) }
    val ownReviews = viewModel.state.reviews.filter { it.author == user.username }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            ElevatedCard(colors = CardDefaults.elevatedCardColors(containerColor = SurfaceA), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(Accent),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(user.username.first().uppercase(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                        }
                        Column {
                            Text(user.username, color = Muted, fontSize = 13.sp)
                            Text(user.displayName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        }
                    }
                    OutlinedTextField(displayName, { displayName = it }, label = { Text("Ім'я профілю") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(bio, { bio = it }, label = { Text("Опис") }, modifier = Modifier.fillMaxWidth())
                    Button(onClick = { viewModel.updateProfile(displayName, bio) }) { Text("Оновити профіль") }
                }
            }
        }
        item {
            SectionCard("Мої відгуки") {
                if (ownReviews.isEmpty()) {
                    Text("Відгуків ще немає", color = Muted)
                } else {
                    ownReviews.forEach { review ->
                        val track = viewModel.state.tracks.findById(review.trackId)
                        Text("${track?.title ?: "Трек"}: ${"★".repeat(review.rating)} ${review.text}", color = Muted)
                    }
                }
            }
        }
    }
}

@Composable
private fun AnalyticsScreen(viewModel: MusicViewModel) {
    val state = viewModel.state
    val analytics = state.analytics

    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                MetricCard("Прослуховувань", analytics.totalPlays().toString(), Modifier.weight(1f))
                MetricCard("Завантажень", analytics.totalDownloads().toString(), Modifier.weight(1f))
            }
        }
        item {
            SectionCard("Використання медіа") {
                state.tracks.forEach { track ->
                    val plays = analytics.plays[track.id] ?: 0
                    val downloads = analytics.downloads[track.id] ?: 0
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(track.title, color = Color.White, fontWeight = FontWeight.SemiBold)
                            Text("${track.artist} / plays: $plays / downloads: $downloads", color = Muted, fontSize = 12.sp)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun MetricCard(title: String, value: String, modifier: Modifier = Modifier) {
    ElevatedCard(colors = CardDefaults.elevatedCardColors(containerColor = SurfaceA), shape = RoundedCornerShape(22.dp), modifier = modifier) {
        Column(Modifier.padding(16.dp)) {
            Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 28.sp)
            Text(title, color = Muted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    ElevatedCard(colors = CardDefaults.elevatedCardColors(containerColor = SurfaceA), shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

@Composable
private fun BottomTabs(selected: Screen, onSelect: (Screen) -> Unit) {
    Surface(color = Color(0xF01A1A23), shadowElevation = 12.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Screen.entries.forEach { screen ->
                val active = screen == selected
                Button(
                    onClick = { onSelect(screen) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (active) Accent else SurfaceB,
                        contentColor = if (active) Color.White else Muted,
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Text(screen.title, fontSize = 11.sp, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun SmallChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val background = if (selected) Accent else SurfaceB
    val content = if (selected) Color.White else Muted
    Surface(
        onClick = onClick,
        color = background,
        contentColor = content,
        shape = RoundedCornerShape(999.dp),
    ) {
        Text(text, modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp), fontSize = 12.sp, maxLines = 1)
    }
}

private fun darkMusicColorScheme() = androidx.compose.material3.darkColorScheme(
    primary = Accent,
    secondary = Color(0xFF35D0BA),
    background = Color(0xFF0F0F13),
    surface = SurfaceA,
    onPrimary = Color.White,
    onSurface = Color.White,
)

private fun List<Track>.findById(id: String): Track? = firstOrNull { it.id == id }

private fun Int.formatDuration(): String {
    val minutes = this / 60
    val seconds = this % 60
    return "%d:%02d".format(minutes, seconds)
}

private val Accent = Color(0xFF7C6AF7)
private val SurfaceA = Color(0xFF1A1A23)
private val SurfaceB = Color(0xFF22222E)
private val Muted = Color(0xFF9B9BB8)

private val seedTracks = listOf(
    Track("1", "Jet Stream Heart", "Temples", "Exotico", "Psychedelic Rock", 231, "music/jet_stream_heart.flac"),
    Track("2", "MODERATE TALKING", "Eccentric", "Night Console", "Electronic", 270, "music/moderate_talking.flac"),
    Track("3", "Happy Song", "Bring Me The Horizon", "That's The Spirit", "Rock", 239, "music/happy_song.flac"),
    Track("4", "Never Gonna Give You Up", "Rick Astley", "Whenever You Need Somebody", "Pop", 213, "music/never_gonna_give_you_up.opus"),
    Track("5", "Genesis 22:10", "The Binding of Isaac", "Soundtrack", "Soundtrack", 139, "music/genesis_22_10.opus"),
    Track("6", "Given Up", "Linkin Park", "Minutes to Midnight", "Rock", 189, "music/given_up.opus"),
    Track("7", "The Emptiness Machine", "Linkin Park", "From Zero", "Rock", 191, "music/emptiness_machine.opus"),
    Track("8", "FMB", "Minimal Schlager", "FMB", "Electronic", 189, "music/fmb.opus"),
    Track("9", "Tally", "Twenty One Pilots", "Scaled and Icy", "Alternative", 213, "music/tally.opus"),
    Track("10", "Say It Right", "Nelly Furtado", "Loose", "Indie", 223, "music/say_it_right.opus"),
)

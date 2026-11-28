package moe.megurine.music.music

import android.content.Context
import android.content.SharedPreferences
import android.widget.Toast
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject

object MusicLibrary {

    private const val PREFS = "music_prefs"
    private const val KEY_HISTORY = "history"
    private const val KEY_PLAYLISTS = "playlists"
    private const val KEY_FAVORITES = "favorites"
    private const val KEY_API = "api_base"
    private const val KEY_QUALITY = "quality"
    private const val KEY_VOLUME = "volume"
    private const val KEY_PLAYBACK_SPEED = "playback_speed"
    private const val KEY_BUFFER_WAIT = "buffer_wait_seconds"
    private const val KEY_COVER_RADIUS = "cover_corner_radius"
    private const val KEY_COVER_CIRCLE = "cover_circle"
    private const val KEY_LYRIC_CENTERED = "lyric_centered"
    private const val KEY_LYRIC_SWITCH_TAP = "lyric_switch_tap"
    private const val KEY_PLAYER_BACKGROUND = "player_background"
    private const val KEY_FLOW_STYLE = "flow_style"
    private const val KEY_LYRIC_FONT_SIZE = "lyric_font_size"
    private const val KEY_LYRIC_FONT_WEIGHT = "lyric_font_weight"
    private const val KEY_TITLE_CENTERED = "title_centered"
    private const val KEY_LYRIC_TRANSLATION = "lyric_translation"
    private const val KEY_PLAY_BAR_LYRICS = "play_bar_lyrics"
    private const val KEY_ADVANCED_MATERIAL = "advanced_material"
    private const val KEY_ADVANCED_MATERIAL_MODE = "advanced_material_mode"
    private const val KEY_AUDIO_FADE = "audio_fade"
    private const val KEY_AUDIO_FOCUS = "audio_focus"
    private const val KEY_HIGH_PRECISION_OUTPUT = "high_precision_output"
    private const val KEY_LIVE_LYRIC = "live_lyric"
    private const val KEY_AUTO_RESUME = "auto_resume"
    private const val KEY_SHOW_LYRIC_ROMAN = "show_lyric_roman"
    private const val KEY_SEARCH_SOURCE = "search_source"
    private const val KEY_SHOW_SOURCE_TAGS = "show_source_tags"
    private const val KEY_BILI_ENABLED = "bili_enabled"
    private const val KEY_BILI_KEYWORDS = "bili_keywords"
    private const val KEY_BILI_LYRICS = "bili_lyrics"
    private const val KEY_DOWNLOAD_QUALITY = "download_quality"
    private const val KEY_DOWNLOAD_METADATA = "download_metadata"
    private const val KEY_DOWNLOAD_NAME_PATTERN = "download_name_pattern"
    private const val KEY_DOWNLOAD_PATH = "download_path"
    private const val KEY_DOWNLOAD_TREE_URI = "download_tree_uri"
    private const val KEY_HIDE_SONG_DURATION = "hide_song_duration"
    private const val KEY_HIDE_CONTROLLER_ICONS = "hide_controller_icons"
    private const val KEY_CONTROLLER_RIGHT_ICON = "controller_right_icon"
    private const val KEY_WEBDAV_URL = "webdav_url"
    private const val KEY_WEBDAV_USERNAME = "webdav_username"
    private const val KEY_WEBDAV_PASSWORD = "webdav_password"
    private const val KEY_WEBDAV_CACHE_WHILE_LISTENING = "webdav_cache_while_listening"
    private const val KEY_SESSION_QUEUE = "session_queue"
    private const val KEY_SESSION_INDEX = "session_index"
    private const val KEY_SESSION_POSITION = "session_position"
    private const val KEY_SESSION_MODE = "session_mode"

    private const val MAX_HISTORY = 200

    private lateinit var prefs: SharedPreferences

    private val _history = MutableStateFlow<List<Song>>(emptyList())
    val history: StateFlow<List<Song>> = _history

    private val _playlists = MutableStateFlow<List<Playlist>>(emptyList())
    val playlists: StateFlow<List<Playlist>> = _playlists

    private val _favorites = MutableStateFlow<List<Song>>(emptyList())
    val favorites: StateFlow<List<Song>> = _favorites

    private val _settings = MutableStateFlow(MusicSettings())
    val settings: StateFlow<MusicSettings> = _settings

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        WebDavApi.init(context)
        _history.value = decodeSongs(prefs.getString(KEY_HISTORY, null))
        _playlists.value = decodePlaylists(prefs.getString(KEY_PLAYLISTS, null))
        _favorites.value = decodeSongs(prefs.getString(KEY_FAVORITES, null))
        _settings.value = loadSettings()
    }

    private fun loadSettings(): MusicSettings = MusicSettings(
        apiBase = prefs.getString(KEY_API, MusicSettings.DEFAULT_API_BASE)
            ?.ifBlank { MusicSettings.DEFAULT_API_BASE }
            ?: MusicSettings.DEFAULT_API_BASE,
        quality = AudioQuality.entries.firstOrNull { it.br == prefs.getInt(KEY_QUALITY, 192) }
            ?: AudioQuality.HIGH,
        volume = prefs.getFloat(KEY_VOLUME, 1f),
        playbackSpeed = prefs.getFloat(KEY_PLAYBACK_SPEED, 1f).let { saved ->
            MusicSettings.PLAYBACK_SPEEDS.firstOrNull { it == saved } ?: 1f
        },
        bufferWaitSeconds = prefs.getInt(KEY_BUFFER_WAIT, MusicSettings.DEFAULT_BUFFER_WAIT_SECONDS)
            .takeIf { it in MusicSettings.BUFFER_WAIT_OPTIONS }
            ?: MusicSettings.DEFAULT_BUFFER_WAIT_SECONDS,
        coverCornerRadius = prefs.getFloat(KEY_COVER_RADIUS, 12f),
        coverCircular = prefs.getBoolean(KEY_COVER_CIRCLE, true),
        lyricCentered = prefs.getBoolean(KEY_LYRIC_CENTERED, true),
        lyricSwitchOnTap = prefs.getBoolean(KEY_LYRIC_SWITCH_TAP, false),
        playerBackground = prefs.getString(KEY_PLAYER_BACKGROUND, "blur") ?: "blur",
        flowStyle = prefs.getString(KEY_FLOW_STYLE, "system") ?: "system",
        lyricFontSize = prefs.getFloat(KEY_LYRIC_FONT_SIZE, 24f).coerceIn(14f, 36f),
        lyricFontWeight = prefs.getInt(KEY_LYRIC_FONT_WEIGHT, 500).coerceIn(300, 900),
        titleCentered = prefs.getBoolean(KEY_TITLE_CENTERED, true),
        showLyricsTranslation = prefs.getBoolean(KEY_LYRIC_TRANSLATION, true),
        playBarLyrics = prefs.getBoolean(KEY_PLAY_BAR_LYRICS, false),
        advancedMaterial = readAdvancedMaterial(),
        audioFade = prefs.getBoolean(KEY_AUDIO_FADE, true),
        audioFocus = prefs.getBoolean(KEY_AUDIO_FOCUS, false),
        highPrecisionOutput = prefs.getBoolean(KEY_HIGH_PRECISION_OUTPUT, false),
        liveLyric = prefs.getBoolean(KEY_LIVE_LYRIC, false),
        autoResume = prefs.getBoolean(KEY_AUTO_RESUME, false),
        showLyricRoman = prefs.getBoolean(KEY_SHOW_LYRIC_ROMAN, false),
        searchSource = prefs.getString(KEY_SEARCH_SOURCE, null)
            ?.takeIf { it in MusicSettings.SEARCH_SOURCES } ?: MusicSettings.SEARCH_SOURCE_ALL,
        showSourceTags = prefs.getBoolean(KEY_SHOW_SOURCE_TAGS, true),
        biliEnabled = prefs.getBoolean(KEY_BILI_ENABLED, true),
        biliKeywords = prefs.getString(KEY_BILI_KEYWORDS, null)
            ?.takeIf { it.isNotBlank() } ?: MusicSettings.DEFAULT_BILI_KEYWORDS,
        biliLyrics = prefs.getBoolean(KEY_BILI_LYRICS, false),
        downloadQuality = AudioQuality.entries.firstOrNull {
            it.br == prefs.getInt(KEY_DOWNLOAD_QUALITY, 128)
        } ?: AudioQuality.STANDARD,
        downloadMetadata = prefs.getBoolean(KEY_DOWNLOAD_METADATA, true),
        downloadNamePattern = prefs.getString(KEY_DOWNLOAD_NAME_PATTERN, null)
            ?.takeIf { it in MusicSettings.DOWNLOAD_NAME_PATTERNS }
            ?: MusicSettings.DOWNLOAD_NAME_SONG_ARTIST,
        downloadPath = prefs.getString(KEY_DOWNLOAD_PATH, null)
            ?.takeIf { it.isNotBlank() } ?: MusicSettings.DEFAULT_DOWNLOAD_PATH,
        downloadTreeUri = prefs.getString(KEY_DOWNLOAD_TREE_URI, null) ?: "",
        hideSongDuration = prefs.getBoolean(KEY_HIDE_SONG_DURATION, false),
        hideControllerIcons = prefs.getBoolean(KEY_HIDE_CONTROLLER_ICONS, false),
        controllerRightIcon = prefs.getString(KEY_CONTROLLER_RIGHT_ICON, null)
            ?.takeIf { it in listOf(MusicSettings.CONTROLLER_ICON_FAVORITE, MusicSettings.CONTROLLER_ICON_CLOSE) }
            ?: MusicSettings.CONTROLLER_ICON_FAVORITE,
        webdavUrl = prefs.getString(KEY_WEBDAV_URL, null).orEmpty(),
        webdavUsername = prefs.getString(KEY_WEBDAV_USERNAME, null).orEmpty(),
        webdavPassword = prefs.getString(KEY_WEBDAV_PASSWORD, null).orEmpty(),
        webdavCacheWhileListening = prefs.getBoolean(KEY_WEBDAV_CACHE_WHILE_LISTENING, false),
    )

    fun exportSettingsJson(): org.json.JSONObject {
        val excluded = setOf(KEY_HISTORY, KEY_PLAYLISTS, KEY_FAVORITES)
        return org.json.JSONObject().also { obj ->
            prefs.all.forEach { (key, value) ->
                if (key in excluded) return@forEach
                when {
                    value is Boolean -> obj.put(key, value)
                    value is Int -> obj.put(key, value)
                    value is Long -> obj.put(key, value)
                    value is Float -> obj.put(key, value)
                    value is String -> obj.put(key, value)
                }
            }
        }
    }

    fun importSettingsJson(json: org.json.JSONObject) {
        val editor = prefs.edit()
        json.keys().forEach { key ->
            val value = json.opt(key)
            when {
                value is Boolean -> editor.putBoolean(key, value)
                value is Int -> editor.putInt(key, value)
                value is Long -> editor.putLong(key, value)
                value is Double -> editor.putFloat(key, value.toFloat())
                value is String -> editor.putString(key, value)
            }
        }
        editor.apply()
        _settings.value = loadSettings()
    }

    fun setQuality(quality: AudioQuality) {
        _settings.value = _settings.value.copy(quality = quality)
        prefs.edit().putInt(KEY_QUALITY, quality.br).apply()
    }

    fun setVolume(volume: Float) {
        val v = volume.coerceIn(0f, 1f)
        _settings.value = _settings.value.copy(volume = v)
        prefs.edit().putFloat(KEY_VOLUME, v).apply()
    }

    fun setPlaybackSpeed(speed: Float) {
        val v = MusicSettings.PLAYBACK_SPEEDS.firstOrNull { it == speed } ?: 1f
        _settings.value = _settings.value.copy(playbackSpeed = v)
        prefs.edit().putFloat(KEY_PLAYBACK_SPEED, v).apply()
    }

    fun setBufferWaitSeconds(seconds: Int) {
        val v = seconds.takeIf { it in MusicSettings.BUFFER_WAIT_OPTIONS }
            ?: MusicSettings.DEFAULT_BUFFER_WAIT_SECONDS
        _settings.value = _settings.value.copy(bufferWaitSeconds = v)
        prefs.edit().putInt(KEY_BUFFER_WAIT, v).apply()
    }

    fun setCoverCircular(circular: Boolean) {
        _settings.value = _settings.value.copy(coverCircular = circular)
        prefs.edit().putBoolean(KEY_COVER_CIRCLE, circular).apply()
    }

    fun setAudioFade(enabled: Boolean) {
        _settings.value = _settings.value.copy(audioFade = enabled)
        prefs.edit().putBoolean(KEY_AUDIO_FADE, enabled).apply()
    }

    fun setHighPrecisionOutput(enabled: Boolean) {
        _settings.value = _settings.value.copy(highPrecisionOutput = enabled)
        prefs.edit().putBoolean(KEY_HIGH_PRECISION_OUTPUT, enabled).apply()
    }

    fun setLiveLyric(enabled: Boolean) {
        _settings.value = _settings.value.copy(liveLyric = enabled)
        prefs.edit().putBoolean(KEY_LIVE_LYRIC, enabled).apply()
    }

    fun setDownloadQuality(quality: AudioQuality) {
        _settings.value = _settings.value.copy(downloadQuality = quality)
        prefs.edit().putInt(KEY_DOWNLOAD_QUALITY, quality.br).apply()
    }

    fun setDownloadMetadata(enabled: Boolean) {
        _settings.value = _settings.value.copy(downloadMetadata = enabled)
        prefs.edit().putBoolean(KEY_DOWNLOAD_METADATA, enabled).apply()
    }

    fun setDownloadPath(path: String, treeUri: String? = null) {
        val uri = treeUri ?: ""
        val normalized = path.trim().ifBlank { MusicSettings.DEFAULT_DOWNLOAD_PATH }
            .let { if (it.endsWith('/')) it else "$it/" }
        _settings.value = _settings.value.copy(downloadPath = normalized, downloadTreeUri = uri)
        prefs.edit()
            .putString(KEY_DOWNLOAD_TREE_URI, uri)
            .putString(KEY_DOWNLOAD_PATH, normalized)
            .apply()
    }

    fun setLyricSwitchOnTap(onTap: Boolean) {
        _settings.value = _settings.value.copy(lyricSwitchOnTap = onTap)
        prefs.edit().putBoolean(KEY_LYRIC_SWITCH_TAP, onTap).apply()
    }

    fun setPlayerBackground(mode: String) {
        _settings.value = _settings.value.copy(playerBackground = mode)
        prefs.edit().putString(KEY_PLAYER_BACKGROUND, mode).apply()
    }

    fun setFlowStyle(style: String) {
        _settings.value = _settings.value.copy(flowStyle = style)
        prefs.edit().putString(KEY_FLOW_STYLE, style).apply()
    }

    fun setLyricCentered(centered: Boolean) {
        _settings.value = _settings.value.copy(lyricCentered = centered)
        prefs.edit().putBoolean(KEY_LYRIC_CENTERED, centered).apply()
    }

    fun setLyricFontWeight(weight: Int) {
        val v = weight.coerceIn(300, 900)
        _settings.value = _settings.value.copy(lyricFontWeight = v)
        prefs.edit().putInt(KEY_LYRIC_FONT_WEIGHT, v).apply()
    }

    fun setLyricFontSize(size: Float) {
        val v = size.coerceIn(14f, 36f)
        _settings.value = _settings.value.copy(lyricFontSize = v)
        prefs.edit().putFloat(KEY_LYRIC_FONT_SIZE, v).apply()
    }

    fun setTitleCentered(centered: Boolean) {
        _settings.value = _settings.value.copy(titleCentered = centered)
        prefs.edit().putBoolean(KEY_TITLE_CENTERED, centered).apply()
    }

    fun setShowLyricsTranslation(show: Boolean) {
        _settings.value = _settings.value.copy(showLyricsTranslation = show)
        prefs.edit().putBoolean(KEY_LYRIC_TRANSLATION, show).apply()
    }

    fun setPlayBarLyrics(enabled: Boolean) {
        _settings.value = _settings.value.copy(playBarLyrics = enabled)
        prefs.edit().putBoolean(KEY_PLAY_BAR_LYRICS, enabled).apply()
        PlaybackController.refreshLiveLyric()
    }

    fun setAudioFocus(enabled: Boolean) {
        _settings.value = _settings.value.copy(audioFocus = enabled)
        prefs.edit().putBoolean(KEY_AUDIO_FOCUS, enabled).apply()
    }

    fun setAutoResume(enabled: Boolean) {
        _settings.value = _settings.value.copy(autoResume = enabled)
        prefs.edit().putBoolean(KEY_AUTO_RESUME, enabled).apply()
    }

    fun setShowLyricRoman(enabled: Boolean) {
        _settings.value = _settings.value.copy(showLyricRoman = enabled)
        prefs.edit().putBoolean(KEY_SHOW_LYRIC_ROMAN, enabled).apply()
    }

    fun setBiliEnabled(enabled: Boolean) {
        _settings.value = _settings.value.copy(biliEnabled = enabled)
        prefs.edit().putBoolean(KEY_BILI_ENABLED, enabled).apply()
    }

    fun setSearchSource(source: String) {
        val normalized = source.takeIf { it in MusicSettings.SEARCH_SOURCES }
            ?: MusicSettings.SEARCH_SOURCE_ALL
        _settings.value = _settings.value.copy(searchSource = normalized)
        prefs.edit().putString(KEY_SEARCH_SOURCE, normalized).apply()
    }

    fun setShowSourceTags(show: Boolean) {
        _settings.value = _settings.value.copy(showSourceTags = show)
        prefs.edit().putBoolean(KEY_SHOW_SOURCE_TAGS, show).apply()
    }

    fun setBiliKeywords(keywords: String) {
        val normalized = keywords.trim().ifBlank { MusicSettings.DEFAULT_BILI_KEYWORDS }
        _settings.value = _settings.value.copy(biliKeywords = normalized)
        prefs.edit().putString(KEY_BILI_KEYWORDS, normalized).apply()
    }

    fun setBiliLyrics(enabled: Boolean) {
        _settings.value = _settings.value.copy(biliLyrics = enabled)
        prefs.edit().putBoolean(KEY_BILI_LYRICS, enabled).apply()
        StreamApi.clearLyricCache()
    }

    fun setDownloadNamePattern(pattern: String) {
        val normalized = pattern.takeIf { it in MusicSettings.DOWNLOAD_NAME_PATTERNS }
            ?: MusicSettings.DOWNLOAD_NAME_SONG_ARTIST
        _settings.value = _settings.value.copy(downloadNamePattern = normalized)
        prefs.edit().putString(KEY_DOWNLOAD_NAME_PATTERN, normalized).apply()
    }

    fun setHideSongDuration(hide: Boolean) {
        _settings.value = _settings.value.copy(hideSongDuration = hide)
        prefs.edit().putBoolean(KEY_HIDE_SONG_DURATION, hide).apply()
    }

    fun setHideControllerIcons(hide: Boolean) {
        _settings.value = _settings.value.copy(hideControllerIcons = hide)
        prefs.edit().putBoolean(KEY_HIDE_CONTROLLER_ICONS, hide).apply()
    }

    fun setControllerRightIcon(icon: String) {
        val normalized = icon.takeIf {
            it in listOf(MusicSettings.CONTROLLER_ICON_FAVORITE, MusicSettings.CONTROLLER_ICON_CLOSE)
        } ?: MusicSettings.CONTROLLER_ICON_FAVORITE
        _settings.value = _settings.value.copy(controllerRightIcon = normalized)
        prefs.edit().putString(KEY_CONTROLLER_RIGHT_ICON, normalized).apply()
    }

    fun setAdvancedMaterial(enabled: Boolean) {
        _settings.value = _settings.value.copy(advancedMaterial = enabled)
        prefs.edit().putBoolean(KEY_ADVANCED_MATERIAL, enabled).apply()
    }

    fun setWebDavConfig(url: String, username: String, password: String) {
        val normalized = url.trim().trimEnd('/')
        _settings.value = _settings.value.copy(
            webdavUrl = normalized,
            webdavUsername = username.trim(),
            webdavPassword = password,
        )
        prefs.edit()
            .putString(KEY_WEBDAV_URL, normalized)
            .putString(KEY_WEBDAV_USERNAME, username.trim())
            .putString(KEY_WEBDAV_PASSWORD, password)
            .apply()
    }

    fun setWebDavCacheWhileListening(enabled: Boolean) {
        _settings.value = _settings.value.copy(webdavCacheWhileListening = enabled)
        prefs.edit().putBoolean(KEY_WEBDAV_CACHE_WHILE_LISTENING, enabled).apply()
    }

    private fun readAdvancedMaterial(): Boolean {
        val legacy = prefs.getString(KEY_ADVANCED_MATERIAL_MODE, null)
        if (legacy != null) {
            val enabled = legacy != "OFF"
            prefs.edit()
                .putBoolean(KEY_ADVANCED_MATERIAL, enabled)
                .remove(KEY_ADVANCED_MATERIAL_MODE)
                .apply()
            return enabled
        }
        return prefs.getBoolean(KEY_ADVANCED_MATERIAL, true)
    }

    fun addHistory(song: Song) {

        val current = _history.value
        if (current.any { it.id == song.id }) return
        val list = current.toMutableList()
        list.add(song)
        while (list.size > MAX_HISTORY) list.removeAt(0)
        _history.value = list
        prefs.edit().putString(KEY_HISTORY, encodeSongs(list)).apply()
    }

    fun removeHistory(songId: String) {
        val list = _history.value.filterNot { it.id == songId }
        _history.value = list
        prefs.edit().putString(KEY_HISTORY, encodeSongs(list)).apply()
    }

    fun patchSongMetadata(song: Song) {
        val patch: (Song) -> Song = { old ->
            if (old.id != song.id) {
                old
            } else {
                old.copy(
                    name = song.name.ifBlank { old.name },
                    artists = song.artists.ifEmpty { old.artists },
                    album = song.album.ifBlank { old.album },
                    cover = song.cover.ifBlank { old.cover },
                )
            }
        }

        val history = _history.value
        val patchedHistory = history.map(patch)
        if (patchedHistory != history) {
            _history.value = patchedHistory
            prefs.edit().putString(KEY_HISTORY, encodeSongs(patchedHistory)).apply()
        }

        val favorites = _favorites.value
        val patchedFavorites = favorites.map(patch)
        if (patchedFavorites != favorites) {
            _favorites.value = patchedFavorites
            prefs.edit().putString(KEY_FAVORITES, encodeSongs(patchedFavorites)).apply()
        }

        var playlistsChanged = false
        val patchedPlaylists = _playlists.value.map { playlist ->
            val songs = playlist.songs.map(patch)
            if (songs == playlist.songs) {
                playlist
            } else {
                playlistsChanged = true
                playlist.copy(songs = songs)
            }
        }
        if (playlistsChanged) {
            _playlists.value = patchedPlaylists
            persistPlaylists(patchedPlaylists)
        }
    }

    fun clearHistory() {
        _history.value = emptyList()
        prefs.edit().remove(KEY_HISTORY).apply()
    }

    fun replaceAll(
        playlists: List<Playlist>,
        favorites: List<Song>,
        history: List<Song>,
    ) {
        val trimmed = history.take(MAX_HISTORY)
        _playlists.value = playlists
        _favorites.value = favorites
        _history.value = trimmed
        prefs.edit()
            .putString(KEY_PLAYLISTS, encodePlaylists(playlists))
            .putString(KEY_FAVORITES, encodeSongs(favorites))
            .putString(KEY_HISTORY, encodeSongs(trimmed))
            .apply()
    }

    fun removeByLocalUri(uris: Set<String>) {
        if (uris.isEmpty()) return
        val matched: (Song) -> Boolean = { it.isLocal && it.localUri in uris }

        val newHistory = _history.value.filterNot(matched)
        if (newHistory.size != _history.value.size) {
            _history.value = newHistory
            prefs.edit().putString(KEY_HISTORY, encodeSongs(newHistory)).apply()
        }

        val newFavorites = _favorites.value.filterNot(matched)
        if (newFavorites.size != _favorites.value.size) {
            _favorites.value = newFavorites
            prefs.edit().putString(KEY_FAVORITES, encodeSongs(newFavorites)).apply()
        }

        var playlistsChanged = false
        val newPlaylists = _playlists.value.map { p ->
            val songs = p.songs.filterNot(matched)
            if (songs.size != p.songs.size) {
                playlistsChanged = true
                p.copy(songs = songs)
            } else p
        }
        if (playlistsChanged) {
            _playlists.value = newPlaylists
            persistPlaylists(newPlaylists)
        }
    }

    data class SavedSession(
        val songs: List<Song>,
        val index: Int,
        val positionMs: Long,
        val mode: PlayMode,
    )

    fun saveSession(
        songs: List<Song>,
        index: Int,
        positionMs: Long,
        mode: PlayMode,
        blocking: Boolean = false,
    ) {
        if (songs.isEmpty()) {
            clearSession()
            return
        }
        val editor = prefs.edit()
            .putString(KEY_SESSION_QUEUE, encodeSongs(songs))
            .putInt(KEY_SESSION_INDEX, index)
            .putLong(KEY_SESSION_POSITION, positionMs)
            .putString(KEY_SESSION_MODE, mode.name)
        if (blocking) editor.commit() else editor.apply()
    }

    fun loadSession(): SavedSession? {
        val songs = decodeSongs(prefs.getString(KEY_SESSION_QUEUE, null))
        if (songs.isEmpty()) return null
        val index = prefs.getInt(KEY_SESSION_INDEX, 0).coerceIn(0, songs.lastIndex)
        val mode = prefs.getString(KEY_SESSION_MODE, null)
            ?.let { runCatching { PlayMode.valueOf(it) }.getOrNull() } ?: PlayMode.ORDER
        return SavedSession(
            songs,
            index,
            prefs.getLong(KEY_SESSION_POSITION, 0L).coerceAtLeast(0L),
            mode,
        )
    }

    fun clearSession() {
        prefs.edit()
            .remove(KEY_SESSION_QUEUE)
            .remove(KEY_SESSION_INDEX)
            .remove(KEY_SESSION_POSITION)
            .remove(KEY_SESSION_MODE)
            .apply()
    }

    fun isFavorite(songId: String): Boolean = _favorites.value.any { it.id == songId }

    fun toggleFavorite(song: Song): Boolean {
        val current = _favorites.value
        val added = current.none { it.id == song.id }
        val list = if (added) current + song else current.filterNot { it.id == song.id }
        _favorites.value = list
        prefs.edit().putString(KEY_FAVORITES, encodeSongs(list)).apply()
        return added
    }

    fun removeFavorite(songId: String) {
        val list = _favorites.value.filterNot { it.id == songId }
        if (list.size == _favorites.value.size) return
        _favorites.value = list
        prefs.edit().putString(KEY_FAVORITES, encodeSongs(list)).apply()
    }

    fun reorderFavorites(from: Int, to: Int) {
        val list = _favorites.value.toMutableList()
        if (from == to || from !in list.indices || to !in list.indices) return
        list.add(to, list.removeAt(from))
        _favorites.value = list
        prefs.edit().putString(KEY_FAVORITES, encodeSongs(list)).apply()
    }

    fun toggleFavoriteWithToast(context: Context, song: Song) {
        val added = toggleFavorite(song)
        Toast.makeText(
            context,
            if (added) "已添加到收藏" else "已取消收藏",
            Toast.LENGTH_SHORT,
        ).show()
    }

    fun createPlaylist(name: String): Playlist {
        val title = name.trim().ifBlank { "新建歌单" }
        val playlist = Playlist(id = UUID.randomUUID().toString(), name = title)
        val list = _playlists.value + playlist
        _playlists.value = list
        persistPlaylists(list)
        return playlist
    }

    fun setPlaylistCover(id: String, cover: String) {
        val list = _playlists.value.map {
            if (it.id == id) it.copy(cover = cover) else it
        }
        _playlists.value = list
        persistPlaylists(list)
    }

    fun renamePlaylist(id: String, name: String) {
        val list = _playlists.value.map {
            if (it.id == id) it.copy(name = name.trim().ifBlank { it.name }) else it
        }
        _playlists.value = list
        persistPlaylists(list)
    }

    fun deletePlaylist(id: String) {
        val list = _playlists.value.filterNot { it.id == id }
        _playlists.value = list
        persistPlaylists(list)
    }

    fun deletePlaylists(ids: Collection<String>) {
        if (ids.isEmpty()) return
        val removed = ids.toSet()
        val current = _playlists.value
        val list = current.filterNot { it.id in removed }
        if (list.size == current.size) return
        _playlists.value = list
        persistPlaylists(list)
    }

    fun addSongToPlaylist(playlistId: String, song: Song): Boolean {
        var added = false
        val list = _playlists.value.map { p ->
            if (p.id == playlistId && p.songs.none { it.id == song.id }) {
                added = true
                p.copy(songs = p.songs + song)
            } else p
        }
        if (added) {
            _playlists.value = list
            persistPlaylists(list)
        }
        return added
    }

    fun addSongsToPlaylist(playlistId: String, songs: List<Song>): Int {
        if (songs.isEmpty()) return 0
        var added = 0
        val list = _playlists.value.map { p ->
            if (p.id != playlistId) return@map p
            val existing = p.songs.mapTo(HashSet()) { it.id }
            val fresh = songs.filter { existing.add(it.id) }
            if (fresh.isEmpty()) p else {
                added = fresh.size
                p.copy(songs = p.songs + fresh)
            }
        }
        if (added > 0) {
            _playlists.value = list
            persistPlaylists(list)
        }
        return added
    }

    fun removeSongFromPlaylist(playlistId: String, songId: String) {
        val list = _playlists.value.map { p ->
            if (p.id == playlistId) p.copy(songs = p.songs.filterNot { it.id == songId }) else p
        }
        _playlists.value = list
        persistPlaylists(list)
    }

    fun reorderPlaylists(from: Int, to: Int) {
        val list = _playlists.value.toMutableList()
        if (from == to || from !in list.indices || to !in list.indices) return
        list.add(to, list.removeAt(from))
        _playlists.value = list
        persistPlaylists(list)
    }

    fun reorderPlaylistSongs(playlistId: String, from: Int, to: Int) {
        if (from == to) return
        var changed = false
        val list = _playlists.value.map { p ->
            val songs = p.songs.toMutableList()
            if (p.id == playlistId && from in songs.indices && to in songs.indices) {
                songs.add(to, songs.removeAt(from))
                changed = true
                p.copy(songs = songs)
            } else p
        }
        if (!changed) return
        _playlists.value = list
        persistPlaylists(list)
    }

    fun playlist(id: String): Playlist? = _playlists.value.firstOrNull { it.id == id }

    private fun persistPlaylists(list: List<Playlist>) {
        prefs.edit().putString(KEY_PLAYLISTS, encodePlaylists(list)).apply()
    }

    private fun encodeSongs(list: List<Song>): String {
        val arr = JSONArray()
        list.forEach { arr.put(it.toJson()) }
        return arr.toString()
    }

    private fun decodeSongs(raw: String?): List<Song> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i ->
                arr.optJSONObject(i)?.let { songFromJson(it) }
            }
        }.getOrDefault(emptyList())
    }

    private fun encodePlaylists(list: List<Playlist>): String {
        val arr = JSONArray()
        list.forEach { p ->
            arr.put(
                JSONObject().apply {
                    put("id", p.id)
                    put("name", p.name)
                    put("createdAt", p.createdAt)
                    if (p.cover.isNotBlank()) put("cover", p.cover)
                    put("songs", JSONArray().also { a -> p.songs.forEach { a.put(it.toJson()) } })
                }
            )
        }
        return arr.toString()
    }

    private fun decodePlaylists(raw: String?): List<Playlist> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                val songsArr = o.optJSONArray("songs")
                val songs = if (songsArr == null) emptyList() else
                    (0 until songsArr.length()).mapNotNull { j ->
                        songsArr.optJSONObject(j)?.let { songFromJson(it) }
                    }
                Playlist(
                    id = o.optString("id"),
                    name = o.optString("name"),
                    songs = songs,
                    createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                    cover = o.optString("cover").orEmpty(),
                )
            }
        }.getOrDefault(emptyList())
    }
}

internal fun Song.toJson(): JSONObject = JSONObject().apply {
    put("id", id)
    put("name", name)
    put("artists", JSONArray().also { a -> artists.forEach { a.put(it) } })
    put("album", album)
    put("picId", picId)
    put("lyricId", lyricId)
    put("duration", durationMs)
    put("source", source)
    put("cover", cover)
    put("localUri", localUri)
    put("remotePath", remotePath)
    if (artistIds.isNotEmpty()) put("artistIds", JSONArray().also { a -> artistIds.forEach { a.put(it) } })
    if (popularity != 0) put("popularity", popularity)
}

internal fun songFromJson(o: JSONObject): Song {
    val artistsArr = o.optJSONArray("artists")
    val artists = if (artistsArr == null) emptyList() else
        (0 until artistsArr.length()).mapNotNull { artistsArr.optString(it).ifBlank { null } }
    val artistIdsArr = o.optJSONArray("artistIds")
    val artistIds = if (artistIdsArr == null) emptyList() else
        (0 until artistIdsArr.length()).mapNotNull { artistIdsArr.optString(it).ifBlank { null } }
    return Song(
        id = o.optString("id"),
        name = o.optString("name"),
        artists = artists,
        album = o.optString("album"),
        picId = o.optString("picId"),
        lyricId = o.optString("lyricId"),
        durationMs = o.optLong("duration", 0L),
        source = o.optString("source", "netease").ifBlank { "netease" },
        cover = o.optString("cover"),
        artistIds = artistIds,
        popularity = o.optInt("popularity", 0),
        localUri = o.optString("localUri"),
        remotePath = o.optString("remotePath"),
    )
}

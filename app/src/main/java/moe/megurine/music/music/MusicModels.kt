package moe.megurine.music.music

data class Song(
    val id: String,
    val name: String,
    val artists: List<String>,
    val album: String,
    val picId: String,
    val lyricId: String,
    val durationMs: Long = 0L,
    val source: String = "netease",
    val cover: String = "",
    val artistIds: List<String> = emptyList(),
    val popularity: Int = 0,

    
    val albumId: String = "",

    val localUri: String = "",
    val remotePath: String = "",
) {
    val isLocal: Boolean get() = localUri.isNotBlank()
    val isBili: Boolean get() = source == StreamApi.BILI_SOURCE
    val isJoox: Boolean get() = source == StreamApi.JOOX
    val isCloud: Boolean get() = source == WebDavApi.SOURCE
    val artistText: String get() = artists.joinToString(" / ").ifBlank { "未知艺术家" }
    val albumText: String get() = album.ifBlank { "未知专辑" }
    val subtitle: String get() = "$artistText · $albumText"
}

data class SongComment(
    val userName: String,
    val content: String,
    val timeMs: Long,
    val dateText: String,
    val likedCount: Int,
    val commentId: Long = 0L,
    val isHot: Boolean = false,
    val location: String = "",
)

data class CommentPage(
    val comments: List<SongComment> = emptyList(),
    val cursor: Long = 0L,
)

data class StreamInfo(
    val url: String,
    val durationMs: Long = 0L,
)

data class Playlist(
    val id: String,
    val name: String,
    val songs: List<Song> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val cover: String = "",
)

enum class AudioQuality(val br: Int, val label: String, val bitrateText: String) {
    STANDARD(128, "标准", "128Kbps"),
    HIGH(192, "高品", "192Kbps"),
    HIGHER(320, "极高", "320Kbps"),
    LOSSLESS(999, "无损", "999Kbps"),
}

data class MusicSettings(
    val apiBase: String = DEFAULT_API_BASE,
    val quality: AudioQuality = AudioQuality.HIGH,
    val volume: Float = 1f,
    val playbackSpeed: Float = 1f,
    val bufferWaitSeconds: Int = DEFAULT_BUFFER_WAIT_SECONDS,
    val autoResume: Boolean = false,
    val coverCornerRadius: Float = 12f,

    val coverCircular: Boolean = true,
    val lyricCentered: Boolean = true,

    val lyricSwitchOnTap: Boolean = false,
    val playerBackground: String = "blur",
    val flowStyle: String = "system",
    val lyricFontSize: Float = 24f,

    val lyricFontWeight: Int = 500,
    val titleCentered: Boolean = true,
    val showLyricsTranslation: Boolean = true,
    val showLyricRoman: Boolean = false,

    val playBarLyrics: Boolean = false,
    val advancedMaterial: Boolean = true,
    val audioFade: Boolean = true,
    val audioFocus: Boolean = false,
    val highPrecisionOutput: Boolean = false,
    val liveLyric: Boolean = false,
    val searchSource: String = SEARCH_SOURCE_ALL,
    val showSourceTags: Boolean = true,
    val biliEnabled: Boolean = true,
    val biliKeywords: String = "高品质 原曲",
    val biliLyrics: Boolean = false,
    val downloadQuality: AudioQuality = AudioQuality.STANDARD,
    val downloadMetadata: Boolean = true,
    val downloadNamePattern: String = DOWNLOAD_NAME_SONG_ARTIST,
    val downloadPath: String = DEFAULT_DOWNLOAD_PATH,
    val downloadTreeUri: String = "",
    val hideSongDuration: Boolean = false,
    val hideControllerIcons: Boolean = false,
    val controllerRightIcon: String = CONTROLLER_ICON_FAVORITE,
    val webdavUrl: String = "",
    val webdavUsername: String = "",
    val webdavPassword: String = "",
    val webdavCacheWhileListening: Boolean = false,
) {
    companion object {
        const val DEFAULT_API_BASE = "https://music-api.gdstudio.xyz/api.php"
        const val SEARCH_SOURCE_ALL = "all"
        const val SEARCH_SOURCE_NETEASE = "netease"
        const val SEARCH_SOURCE_JOOX = "joox"
        val SEARCH_SOURCES = listOf(SEARCH_SOURCE_ALL, SEARCH_SOURCE_NETEASE, SEARCH_SOURCE_JOOX)
        val SEARCH_SOURCE_LABELS = listOf("聚合", "网易", "JOOX")
        const val DEFAULT_DOWNLOAD_PATH = "/storage/emulated/0/Music/"
        const val DEFAULT_BILI_KEYWORDS = "高品质 原曲"
        const val DOWNLOAD_NAME_SONG_ARTIST = "song_artist"
        const val DOWNLOAD_NAME_ARTIST_SONG = "artist_song"
        const val DOWNLOAD_NAME_SONG = "song"
        const val CONTROLLER_ICON_FAVORITE = "favorite"
        const val CONTROLLER_ICON_CLOSE = "close"
        const val DEFAULT_BUFFER_WAIT_SECONDS = 30
        
        val BUFFER_WAIT_OPTIONS = listOf(10, 15, 30, 45, 60)
        val PLAYBACK_SPEEDS = listOf(0.5f, 0.75f, 1f, 1.5f, 2f)
        val PLAYBACK_SPEED_LABELS = listOf("0.5×", "0.75×", "1×", "1.5×", "2×")
        val DOWNLOAD_NAME_PATTERNS = listOf(
            DOWNLOAD_NAME_SONG_ARTIST,
            DOWNLOAD_NAME_ARTIST_SONG,
            DOWNLOAD_NAME_SONG,
        )
        val DOWNLOAD_NAME_PATTERN_LABELS = listOf("歌曲名 - 歌手", "歌手 - 歌曲名", "歌曲名")
    }
}

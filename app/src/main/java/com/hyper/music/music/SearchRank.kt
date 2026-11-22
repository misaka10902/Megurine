package com.hyper.music.music

internal object SearchRank {

    private val DERIVATIVE: List<Pair<String, Regex?>> = listOf(
        "cover", "翻唱", "伴奏", "instrumental", "inst", "off vocal", "纯音乐", "钢琴", "小提琴",
        "remix", "rmx", "live", "现场", "歌ってみた", "8bit", "karaoke", "消音", "demo",
        "tv size", "tvsize", "tv版", "短版", "片段", "歌单", "合集", "串烧", "循环", "教学",
        "填词", "粤语", "混剪", "剪辑", "重制", "mv",
    ).map { word ->
        val latin = word.all { it.code < 128 }
        word to if (latin) {
            Regex("(^|[^a-z0-9])" + Regex.escape(word) + "([^a-z0-9]|$)")
        } else {
            null
        }
    }

    fun rank(songs: List<Song>, keyword: String): List<Song> {
        val key = MusicApi.normalizeName(keyword)
        if (key.isEmpty() || songs.size < 2) return songs
        val artistIntent = isArtistIntent(songs, key)
        return songs.withIndex()
            .sortedByDescending { (index, song) -> score(song, key, index, songs.size, artistIntent) }
            .map { it.value }
    }

    fun merge(netease: List<Song>, bili: List<Song>, keyword: String): List<Song> {
        val key = MusicApi.normalizeName(keyword)
        if (key.isEmpty()) return (netease + bili).distinctBy { it.id }
        val artistIntent = isArtistIntent(netease, key)
        val scored = ArrayList<Pair<Song, Float>>(netease.size + bili.size)
        val neteaseBias = if (artistIntent) 0.03f else 0.08f
        netease.forEachIndexed { index, song ->
            scored += song to score(song, key, index, netease.size, artistIntent) + neteaseBias
        }
        val biliBias = if (artistIntent) -0.15f else 0f
        bili.forEachIndexed { index, song ->
            scored += song to score(song, key, index, bili.size, artistIntent) + biliBias
        }
        val seen = HashSet<String>(scored.size)
        val out = ArrayList<Song>(scored.size)
        for ((song, _) in scored.sortedByDescending { it.second }) {
            val artistKey = song.artists.firstOrNull()?.let { MusicApi.normalizeName(it) }.orEmpty()
            if (!seen.add(MusicApi.normalizeName(song.name) + "\u0001" + artistKey)) continue
            out += song
        }
        return out
    }

    private fun isArtistIntent(netease: List<Song>, key: String): Boolean {
        val sample = netease.take(12)
        if (sample.size < 4) return false
        val hits = sample.count { song ->
            song.artists.any { MusicApi.normalizeName(it) == key }
        }
        return hits >= 4 && hits * 2 >= sample.size
    }

    private fun score(song: Song, key: String, index: Int, total: Int, artistIntent: Boolean): Float {
        val title = titleScore(song.name, key)
        val rank = if (total <= 1) 1f else 1f - index.toFloat() / total
        val heat = (song.popularity / 100f).coerceIn(0f, 1f)
        val affinity = artistAffinity(song.artists, key)
        val base = if (artistIntent) {
            title * 0.22f + affinity * 0.30f + heat * 0.18f + rank * 0.14f
        } else {
            title * 0.58f + rank * 0.20f + heat * 0.16f + affinity * 0.10f
        }
        return base - penalty(song.name, key)
    }

    private fun artistAffinity(artists: List<String>, key: String): Float {
        var best = 0f
        for (artist in artists) {
            val name = MusicApi.normalizeName(artist)
            if (name.isEmpty()) continue
            val value = when {
                name == key -> 1f
                key.length >= 3 && name.contains(key) -> 0.8f
                name.length >= 3 && key.contains(name) -> 0.5f
                else -> 0f
            }
            if (value > best) best = value
        }
        return best
    }

    private fun titleScore(name: String, key: String): Float {
        val title = MusicApi.normalizeName(name)
        if (title.isEmpty()) return 0f
        return when {
            title == key -> 1f
            title.contains(key) -> 0.88f + 0.06f * key.length / title.length
            else -> MusicApi.similarity(key, title) * 0.9f
        }
    }

    private fun penalty(name: String, key: String): Float {
        val title = name.lowercase()
        var hits = 0
        for ((word, regex) in DERIVATIVE) {
            if (key.contains(MusicApi.normalizeName(word))) continue
            val present = if (regex == null) title.contains(word) else regex.containsMatchIn(title)
            if (present) hits++
        }
        return (hits * 0.12f).coerceAtMost(0.3f)
    }
}

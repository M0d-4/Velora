package com.velora.app.util

import com.velora.app.model.LyricLine
import java.io.File

object LyricsParser {

    /** Parse an .lrc file into a sorted list of LyricLine */
    fun parseLrc(file: File): List<LyricLine> {
        if (!file.exists()) return emptyList()
        val lines = mutableListOf<LyricLine>()
        val timeRegex = Regex("""\[(\d{2}):(\d{2})\.(\d{2,3})\]""")

        file.forEachLine { rawLine ->
            val matches = timeRegex.findAll(rawLine)
            val text = rawLine.replace(timeRegex, "").trim()
            if (text.isNotEmpty()) {
                matches.forEach { m ->
                    val min = m.groupValues[1].toLong()
                    val sec = m.groupValues[2].toLong()
                    val ms = m.groupValues[3].let {
                        if (it.length == 2) it.toLong() * 10 else it.toLong()
                    }
                    lines.add(LyricLine((min * 60 + sec) * 1000 + ms, text))
                }
            }
        }
        return lines.sortedBy { it.timeMs }
    }

    /** Try to find an .lrc file next to the media file */
    fun findLrcForMedia(mediaPath: String?): File? {
        if (mediaPath == null) return null
        val base = mediaPath.substringBeforeLast('.')
        val candidate = File("$base.lrc")
        return if (candidate.exists()) candidate else null
    }

    /** Given current position, return the active line index */
    fun activeIndex(lyrics: List<LyricLine>, positionMs: Long): Int {
        if (lyrics.isEmpty()) return -1
        var idx = 0
        for (i in lyrics.indices) {
            if (lyrics[i].timeMs <= positionMs) idx = i else break
        }
        return idx
    }
}

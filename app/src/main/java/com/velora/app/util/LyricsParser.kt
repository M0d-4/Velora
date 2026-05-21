package com.velora.app.util

import com.velora.app.model.LyricLine
import java.io.File

object LyricsParser {

    // ── LRC ──────────────────────────────────────────────────────────────────

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

    // ── SRT ──────────────────────────────────────────────────────────────────

    fun parseSrt(file: File): List<LyricLine> {
        if (!file.exists()) return emptyList()
        val lines = mutableListOf<LyricLine>()
        // SRT timestamp: 00:00:01,500 --> 00:00:04,000
        val timeRegex = Regex("""(\d{2}):(\d{2}):(\d{2})[,.](\d{3})\s*-->""")
        val blocks = file.readText().split(Regex("""\r?\n\r?\n"""))
        for (block in blocks) {
            val blockLines = block.trim().lines()
            val timeLine = blockLines.firstOrNull { timeRegex.containsMatchIn(it) } ?: continue
            val m = timeRegex.find(timeLine) ?: continue
            val h = m.groupValues[1].toLong()
            val min = m.groupValues[2].toLong()
            val sec = m.groupValues[3].toLong()
            val ms = m.groupValues[4].toLong()
            val timeMs = ((h * 3600 + min * 60 + sec) * 1000) + ms
            // text lines are everything after the timestamp line (skip index line too)
            val textLines = blockLines
                .dropWhile { !timeRegex.containsMatchIn(it) }
                .drop(1)
                .map { it.trim() }
                .filter { it.isNotEmpty() }
            if (textLines.isNotEmpty()) {
                lines.add(LyricLine(timeMs, textLines.joinToString(" ")))
            }
        }
        return lines.sortedBy { it.timeMs }
    }

    // ── Auto-detect and parse any supported lyrics file ──────────────────────

    fun parse(file: File): List<LyricLine> = when (file.extension.lowercase()) {
        "srt" -> parseSrt(file)
        else  -> parseLrc(file)      // .lrc or anything else
    }

    /** Try to find an .lrc or .srt file next to the media file */
    fun findLyricsForMedia(mediaPath: String?): File? {
        if (mediaPath == null) return null
        val base = mediaPath.substringBeforeLast('.')
        return listOf("$base.lrc", "$base.srt")
            .map { File(it) }
            .firstOrNull { it.exists() }
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

package org.holypresenter_bible.planner

import org.holypresenter_bible.domain.BibleReference

object BiblePlannerReferenceCodec {
    private const val VERSION = "v1"
    private const val SEPARATOR = "|"

    fun encode(
        reference: BibleReference
    ): String =
        listOf(
            VERSION,
            reference.translationId,
            reference.bookId,
            reference.chapter.toString(),
            reference.verseStart.toString(),
            reference.verseEnd.toString()
        ).joinToString(SEPARATOR)

    fun decode(
        value: String
    ): BibleReference? {
        val parts = value.split(SEPARATOR)

        if (
            parts.size != 6 ||
            parts[0] != VERSION
        ) {
            return null
        }

        val chapter =
            parts[3].toIntOrNull()
                ?: return null

        val verseStart =
            parts[4].toIntOrNull()
                ?: return null

        val verseEnd =
            parts[5].toIntOrNull()
                ?: return null

        return runCatching {
            BibleReference(
                translationId = parts[1],
                bookId = parts[2],
                chapter = chapter,
                verseStart = verseStart,
                verseEnd = verseEnd
            )
        }.getOrNull()
    }
}
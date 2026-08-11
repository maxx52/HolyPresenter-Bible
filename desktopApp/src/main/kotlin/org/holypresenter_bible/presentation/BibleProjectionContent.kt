package org.holypresenter_bible.presentation

import org.holypresenter_bible.domain.BiblePassage

internal data class BibleProjectionContent(
    val text: String,
    val reference: String
)

internal object BibleProjectionContentFactory {
    fun create(
        passage: BiblePassage,
        bookAbbreviation: String
    ): BibleProjectionContent {
        require(passage.verses.isNotEmpty()) {
            "Нельзя сформировать слайд из пустого отрывка"
        }

        val showVerseNumbers = passage.verses.size > 1
        val text =
            passage.verses
                .sortedBy { verse ->
                    verse.number
                }
                .joinToString(
                    separator = "\n\n"
                ) { verse ->
                    if (showVerseNumbers) {
                        "${verse.number} ${verse.text}"
                    } else {
                        verse.text
                    }
                }

        return BibleProjectionContent(
            text = text,
            reference =
                formatReference(
                    bookAbbreviation = bookAbbreviation,
                    chapter = passage.reference.chapter,
                    verseStart = passage.reference.verseStart,
                    verseEnd = passage.reference.verseEnd
                )
        )
    }

    private fun formatReference(
        bookAbbreviation: String,
        chapter: Int,
        verseStart: Int,
        verseEnd: Int
    ): String {
        val verses =
            if (verseStart == verseEnd) {
                verseStart.toString()
            } else {
                "$verseStart–$verseEnd"
            }

        return "$bookAbbreviation. $chapter:$verses"
    }
}

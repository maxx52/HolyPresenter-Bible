package org.holypresenter_bible.ui

data class BibleVerseSelection(
    val anchor: Int,
    val focus: Int = anchor
) {
    val start: Int
        get() = minOf(anchor, focus)

    val end: Int
        get() = maxOf(anchor, focus)

    fun contains(
        verseNumber: Int
    ): Boolean = verseNumber in start..end

    fun extendTo(
        verseNumber: Int
    ): BibleVerseSelection =
        copy(
            focus = verseNumber
        )
}
package org.holypresenter_bible.presentation

import holypresenter.org.platform.api.presentation.Presentation
import holypresenter.org.platform.api.presentation.PresentationMetadata
import holypresenter.org.platform.api.presentation.PresentationSlide
import holypresenter.org.platform.api.presentation.SlotId
import holypresenter.org.platform.api.presentation.element.TextElement
import holypresenter.org.platform.api.presentation.theme.PresentationBackground
import holypresenter.org.platform.api.presentation.theme.PresentationBackgroundType
import holypresenter.org.platform.api.presentation.theme.PresentationOverlay
import holypresenter.org.platform.api.presentation.theme.PresentationTextStyle
import holypresenter.org.platform.api.presentation.theme.PresentationTheme
import org.holypresenter_bible.domain.BiblePassage

class BiblePresentationFactory {
    fun create(
        passage: BiblePassage,
        bookAbbreviation: String
    ): Presentation {
        val reference = passage.reference

        val presentationTitle =
            formatReference(
                bookAbbreviation = bookAbbreviation,
                chapter = reference.chapter,
                verseStart = reference.verseStart,
                verseEnd = reference.verseEnd
            )

        return Presentation(
            id = buildPresentationId(passage),
            metadata =
                PresentationMetadata(
                    title = presentationTitle
                ),
            theme = defaultBibleTheme(),
            slides = passage.verses.map { verse ->
                val verseReference = "$bookAbbreviation. ${reference.chapter}:${verse.number}"

                PresentationSlide(
                    id = buildSlideId(
                            passage = passage,
                            verseNumber = verse.number
                        ),
                    elements =
                        listOf(
                            TextElement(
                                id =
                                    buildTextElementId(
                                        passage = passage,
                                        verseNumber = verse.number
                                    ),
                                slot = SlotId("main"),
                                text =
                                    buildString {
                                        append(verse.text)
                                        append("\n\n")
                                        append(verseReference)
                                    }
                            )
                        )
                )
                }
        )
    }

    private fun defaultBibleTheme(): PresentationTheme =
        PresentationTheme(
            background =
                PresentationBackground(
                    type = PresentationBackgroundType.COLOR,
                    color = 0xFF000000
                ),
            textStyle =
                PresentationTextStyle(
                    fontSize = 56,
                    textColor = 0xFFFFFFFF,
                    bold = false,
                    italic = false,
                    outlineEnabled = true,
                    shadowEnabled = true
                ),
            overlay = PresentationOverlay(enabled = false)
        )

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

    private fun buildPresentationId(
        passage: BiblePassage
    ): String {
        val reference = passage.reference

        return buildString {
            append("bible-")
            append(reference.translationId)
            append("-")
            append(reference.bookId)
            append("-")
            append(reference.chapter)
            append("-")
            append(reference.verseStart)
            append("-")
            append(reference.verseEnd)
        }
    }

    private fun buildSlideId(
        passage: BiblePassage,
        verseNumber: Int
    ): String = "${buildPresentationId(passage)}-verse-$verseNumber"

    private fun buildTextElementId(
        passage: BiblePassage,
        verseNumber: Int
    ): String = "${buildSlideId(passage, verseNumber)}-text"
}
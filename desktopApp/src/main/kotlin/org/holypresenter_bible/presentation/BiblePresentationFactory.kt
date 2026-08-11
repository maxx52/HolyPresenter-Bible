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
        val projectionContent =
            BibleProjectionContentFactory.create(
                passage = passage,
                bookAbbreviation = bookAbbreviation
            )

        return Presentation(
            id = buildPresentationId(passage),
            metadata =
                PresentationMetadata(
                    title = projectionContent.reference
                ),
            theme = defaultBibleTheme(),
            slides =
                listOf(
                    PresentationSlide(
                        id = buildSlideId(passage),
                        elements =
                            listOf(
                                TextElement(
                                    id = buildTextElementId(passage),
                                    slot = SlotId("main"),
                                    text =
                                        buildString {
                                            append(projectionContent.text)
                                            append("\n\n")
                                            append(projectionContent.reference)
                                        }
                                )
                            )
                    )
                )
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
        passage: BiblePassage
    ): String = "${buildPresentationId(passage)}-passage"

    private fun buildTextElementId(
        passage: BiblePassage
    ): String = "${buildSlideId(passage)}-text"
}

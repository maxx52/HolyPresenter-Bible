package org.holypresenter_bible.presentation

import kotlin.test.Test
import kotlin.test.assertEquals
import org.holypresenter_bible.domain.BiblePassage
import org.holypresenter_bible.domain.BibleReference
import org.holypresenter_bible.domain.BibleVerse

class BibleProjectionContentFactoryTest {
    @Test
    fun `range contains every verse and range reference`() {
        val content =
            BibleProjectionContentFactory.create(
                passage =
                    passage(
                        verseStart = 16,
                        verseEnd = 18,
                        verses =
                            listOf(
                                BibleVerse(16, "Текст стиха 16."),
                                BibleVerse(17, "Текст стиха 17."),
                                BibleVerse(18, "Текст стиха 18.")
                            )
                    ),
                bookAbbreviation = "Ин"
            )

        assertEquals(
            "16 Текст стиха 16.\n\n" +
                "17 Текст стиха 17.\n\n" +
                "18 Текст стиха 18.",
            content.text
        )
        assertEquals("Ин. 3:16–18", content.reference)
    }

    @Test
    fun `single verse keeps original text`() {
        val content =
            BibleProjectionContentFactory.create(
                passage =
                    passage(
                        verseStart = 18,
                        verseEnd = 18,
                        verses =
                            listOf(
                                BibleVerse(18, "Текст стиха 18.")
                            )
                    ),
                bookAbbreviation = "Ин"
            )

        assertEquals("Текст стиха 18.", content.text)
        assertEquals("Ин. 3:18", content.reference)
    }

    private fun passage(
        verseStart: Int,
        verseEnd: Int,
        verses: List<BibleVerse>
    ): BiblePassage =
        BiblePassage(
            reference =
                BibleReference(
                    translationId = "test",
                    bookId = "john",
                    chapter = 3,
                    verseStart = verseStart,
                    verseEnd = verseEnd
                ),
            verses = verses
        )
}

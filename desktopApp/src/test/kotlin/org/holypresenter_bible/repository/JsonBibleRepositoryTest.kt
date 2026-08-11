package org.holypresenter_bible.repository

import org.holypresenter_bible.domain.BibleReference
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertSame

class JsonBibleRepositoryTest {
    @Test
    fun `bundled Synodal translation is complete and replaces test data with the same id`() {
        withTemporaryDirectory { translationsDirectory ->
            File(
                translationsDirectory,
                "synodal.json"
            ).writeText(
                """
                {
                  "id": "synodal",
                  "name": "Тестовый перевод",
                  "abbreviation": "Тест",
                  "language": "ru",
                  "books": []
                }
                """.trimIndent()
            )

            var resourceLoadCount = 0
            val repository =
                JsonBibleRepository(
                    translationsDirectory = translationsDirectory,
                    bundledTranslationResources = BuiltInBibleTranslations.resources,
                    resourceLoader = { resourcePath ->
                        resourceLoadCount += 1
                        JsonBibleRepositoryTest::class.java
                            .getResourceAsStream(resourcePath)
                    }
                )

            val firstLoad = repository.getTranslations()
            val secondLoad = repository.getTranslations()
            val synodal =
                firstLoad.single { translation ->
                    translation.id == BuiltInBibleTranslations.SYNODAL_ID
                }

            assertEquals(1, firstLoad.size)
            assertEquals("Синодальный перевод", synodal.name)
            assertEquals(66, synodal.books.size)
            assertEquals(
                1189,
                synodal.books.sumOf { book ->
                    book.chapters.size
                }
            )
            assertEquals(
                31169,
                synodal.books.sumOf { book ->
                    book.chapters.sumOf { chapter ->
                        chapter.verses.size
                    }
                }
            )
            assertSame(firstLoad.single(), secondLoad.single())
            assertEquals(1, resourceLoadCount)

            val john316 =
                repository.getPassage(
                    BibleReference(
                        translationId = BuiltInBibleTranslations.SYNODAL_ID,
                        bookId = "john",
                        chapter = 3,
                        verseStart = 16
                    )
                )

            assertNotNull(john316)
            assertEquals(
                "Ибо так возлюбил Бог мир, что отдал Сына Своего Единородного, " +
                    "дабы всякий, верующий в Него, не погиб, но имел жизнь вечную.",
                john316.verses.single().text
            )
        }
    }

    private fun withTemporaryDirectory(
        block: (File) -> Unit
    ) {
        val directory =
            Files.createTempDirectory(
                "holypresenter-bible-test"
            ).toFile()

        try {
            block(directory)
        } finally {
            directory.deleteRecursively()
        }
    }
}

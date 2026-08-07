package org.holypresenter_bible.repository

import kotlinx.serialization.json.Json
import org.holypresenter_bible.domain.BiblePassage
import org.holypresenter_bible.domain.BibleReference
import org.holypresenter_bible.domain.BibleTranslation
import java.io.File

class JsonBibleRepository(
    private val translationsDirectory: File
) : BibleRepository {
    private val json =
        Json {
            prettyPrint = true
            ignoreUnknownKeys = true
        }

    private val cache = mutableMapOf<String, BibleTranslation>()

    init {
        ensureTranslationsDirectory()
    }

    override fun getTranslations(): List<BibleTranslation> {
        return translationFiles()
            .mapNotNull { file ->
                loadTranslation(file)
            }
            .sortedBy { translation ->
                translation.name
            }
    }

    override fun findTranslation(
        translationId: String
    ): BibleTranslation? {

        cache[translationId]
            ?.let {
                return it
            }

        val file =
            translationFiles()
                .firstOrNull { file ->
                    file.nameWithoutExtension == translationId
                }
                ?: return null
        return loadTranslation(file)
    }

    override fun getPassage(reference: BibleReference): BiblePassage? {
        val translation =
            findTranslation(reference.translationId)
                ?: return null

        val book =
            translation.books
                .firstOrNull { book ->
                    book.id == reference.bookId
                }
                ?: return null

        val chapter =
            book.chapters
                .firstOrNull { chapter ->
                    chapter.number == reference.chapter
                }
                ?: return null

        val verses =
            chapter.verses
                .filter { verse ->
                    verse.number in reference.verseStart..reference.verseEnd
                }

        if (verses.isEmpty()) {
            return null
        }

        return BiblePassage(
            reference = reference,
            verses = verses
        )
    }

    private fun loadTranslation(file: File): BibleTranslation? {
        return runCatching {
            json.decodeFromString<BibleTranslation>(
                file.readText(Charsets.UTF_8)
            )
        }
            .onSuccess { translation ->
                cache[translation.id] = translation
            }
            .onFailure { error ->
                println("[Bible] Failed to load ${file.absolutePath}: " + error.message)
            }
            .getOrNull()
    }

    private fun translationFiles(): List<File> {
        return translationsDirectory
            .listFiles { file ->
                file.isFile && file.extension.equals(
                    "json",
                    ignoreCase = true
                )
            }
            ?.toList()
            .orEmpty()
    }

    private fun ensureTranslationsDirectory() {

        if (
            !translationsDirectory.exists() &&
            !translationsDirectory.mkdirs()
        ) {
            error(
                "Не удалось создать каталог переводов: " + translationsDirectory.absolutePath
            )
        }

        require(
            translationsDirectory.isDirectory
        ) {
            "Путь переводов не является каталогом: " + translationsDirectory.absolutePath
        }
    }
}
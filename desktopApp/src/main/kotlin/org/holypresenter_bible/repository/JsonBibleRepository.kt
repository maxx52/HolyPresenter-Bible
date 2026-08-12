package org.holypresenter_bible.repository

import kotlinx.serialization.json.Json
import org.holypresenter_bible.domain.BiblePassage
import org.holypresenter_bible.domain.BibleReference
import org.holypresenter_bible.domain.BibleTranslation
import java.io.File
import java.io.InputStream

class JsonBibleRepository(
    private val translationsDirectory: File,
    private val bundledTranslationResources: List<String> = emptyList(),
    private val resourceLoader: (String) -> InputStream? = { resourcePath ->
        JsonBibleRepository::class.java.getResourceAsStream(resourcePath)
    }
) : BibleRepository {
    private val json =
        Json {
            prettyPrint = true
            ignoreUnknownKeys = true
        }

    private val cache = mutableMapOf<String, BibleTranslation>()
    private val sourceCache = mutableMapOf<String, BibleTranslation>()
    private val failedSources = mutableSetOf<String>()

    init {
        ensureTranslationsDirectory()
    }

    override fun importUsfmArchive(archive: File): BibleTranslation {
        val translation = UsfmTranslationImporter(translationsDirectory).importArchive(archive)
        sourceCache.clear()
        failedSources.clear()
        cache[translation.id] = translation
        return translation
    }

    override fun getTranslations(): List<BibleTranslation> {
        val bundledTranslations =
            bundledTranslationResources
                .mapNotNull { resourcePath ->
                    loadBundledTranslation(resourcePath)
                }

        val bundledIds =
            bundledTranslations
                .mapTo(mutableSetOf()) { translation ->
                    translation.id
                }

        val externalTranslations =
            translationFiles()
                .mapNotNull { file ->
                    loadFileTranslation(file)
                }
                .filterNot { translation ->
                    translation.id in bundledIds
                }

        return (bundledTranslations + externalTranslations)
            .distinctBy { translation ->
                translation.id
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

        return getTranslations()
            .firstOrNull { translation ->
                translation.id == translationId
            }
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

    private fun loadBundledTranslation(
        resourcePath: String
    ): BibleTranslation? {
        return loadTranslation(
            sourceKey = "resource:$resourcePath",
            sourceDescription = resourcePath
        ) {
            resourceLoader(resourcePath)
                ?.bufferedReader(Charsets.UTF_8)
                ?.use { reader ->
                    reader.readText()
                }
                ?: error("Resource not found")
        }
    }

    private fun loadFileTranslation(file: File): BibleTranslation? {
        return loadTranslation(
            sourceKey = "file:${file.absolutePath}",
            sourceDescription = file.absolutePath
        ) {
            file.readText(Charsets.UTF_8)
        }
    }

    private fun loadTranslation(
        sourceKey: String,
        sourceDescription: String,
        readText: () -> String
    ): BibleTranslation? {
        sourceCache[sourceKey]
            ?.let { translation ->
                return translation
            }

        if (sourceKey in failedSources) {
            return null
        }

        return runCatching {
            json.decodeFromString<BibleTranslation>(
                readText()
            )
        }
            .onSuccess { translation ->
                sourceCache[sourceKey] = translation

                if (translation.id !in cache) {
                    cache[translation.id] = translation
                }
            }
            .onFailure { error ->
                failedSources += sourceKey
                println("[Bible] Failed to load $sourceDescription: " + error.message)
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
            ?.sortedBy { file ->
                file.name.lowercase()
            }
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

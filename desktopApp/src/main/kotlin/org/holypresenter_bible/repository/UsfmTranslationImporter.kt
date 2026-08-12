package org.holypresenter_bible.repository

import kotlinx.serialization.json.Json
import org.holypresenter_bible.domain.BibleBook
import org.holypresenter_bible.domain.BibleChapter
import org.holypresenter_bible.domain.BibleTestament
import org.holypresenter_bible.domain.BibleTranslation
import org.holypresenter_bible.domain.BibleVerse
import java.io.File
import java.util.zip.ZipFile

internal class UsfmTranslationImporter(
    private val translationsDirectory: File
) {
    private val json = Json { prettyPrint = true }

    fun importArchive(archive: File): BibleTranslation {
        require(archive.isFile && archive.extension.equals("zip", true)) {
            "Выберите ZIP-архив перевода в формате USFM"
        }

        val files = ZipFile(archive).use { zip ->
            zip.entries().asSequence()
                .filter { entry -> !entry.isDirectory && entry.name.endsWith(".usfm", true) }
                .map { entry -> entry.name to zip.getInputStream(entry).bufferedReader(Charsets.UTF_8).use { it.readText() } }
                .toList()
        }
        require(files.isNotEmpty()) { "В архиве не найдены файлы USFM" }

        val id = archive.nameWithoutExtension.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')
        val translation = BibleTranslation(
            id = id.ifBlank { "imported-bible" },
            name = archive.nameWithoutExtension,
            abbreviation = archive.nameWithoutExtension.take(16),
            language = "und",
            books = files.mapNotNull(::parseBook).sortedBy { it.order }
        )
        require(translation.books.isNotEmpty()) { "Не удалось прочитать книги из USFM" }

        File(translationsDirectory, "${translation.id}.json")
            .writeText(json.encodeToString(BibleTranslation.serializer(), translation), Charsets.UTF_8)
        return translation
    }

    private fun parseBook(source: Pair<String, String>): BibleBook? {
        val lines = source.second.lineSequence().toList()
        val idLine = lines.firstOrNull { it.startsWith("\\id ") } ?: return null
        val code = idLine.removePrefix("\\id ").trim().take(3)
        val order = canonicalOrder[code] ?: return null
        val name = lines.firstOrNull { it.startsWith("\\h ") }?.removePrefix("\\h ")?.trim().orEmpty().ifBlank { code }
        val abbreviation = lines.firstOrNull { it.startsWith("\\toc3 ") }?.removePrefix("\\toc3 ")?.trim().orEmpty().ifBlank { code }
        var chapter = 0
        val verses = linkedMapOf<Int, MutableList<BibleVerse>>()
        lines.forEach { line ->
            when {
                line.startsWith("\\c ") -> chapter = line.removePrefix("\\c ").trim().takeWhile(Char::isDigit).toIntOrNull() ?: 0
                line.startsWith("\\v ") && chapter > 0 -> {
                    val value = line.removePrefix("\\v ").trim()
                    val number = value.takeWhile(Char::isDigit).toIntOrNull() ?: return@forEach
                    val text = value.dropWhile { it.isDigit() }.trim().replace(Regex("\\\\[a-z0-9+* -]+"), "").trim()
                    if (text.isNotBlank()) verses.getOrPut(chapter) { mutableListOf() } += BibleVerse(number, text)
                }
            }
        }
        return BibleBook(code.lowercase(), name, abbreviation, if (order <= 39) BibleTestament.OLD else BibleTestament.NEW, order, verses.map { BibleChapter(it.key, it.value) })
    }

    private companion object {
        val canonicalOrder = listOf("GEN","EXO","LEV","NUM","DEU","JOS","JDG","RUT","1SA","2SA","1KI","2KI","1CH","2CH","EZR","NEH","EST","JOB","PSA","PRO","ECC","SNG","ISA","JER","LAM","EZK","DAN","HOS","JOL","AMO","OBA","JON","MIC","NAM","HAB","ZEP","HAG","ZEC","MAL","MAT","MRK","LUK","JHN","ACT","ROM","1CO","2CO","GAL","EPH","PHP","COL","1TH","2TH","1TI","2TI","TIT","PHM","HEB","JAS","1PE","2PE","1JN","2JN","3JN","JUD","REV").withIndex().associate { it.value to it.index + 1 }
    }
}

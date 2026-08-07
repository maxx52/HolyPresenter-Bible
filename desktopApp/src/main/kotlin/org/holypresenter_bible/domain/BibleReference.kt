package org.holypresenter_bible.domain

import kotlinx.serialization.Serializable

@Serializable
data class BibleReference(
    val translationId: String,
    val bookId: String,
    val chapter: Int,
    val verseStart: Int,
    val verseEnd: Int = verseStart
) {
    init {
        require(chapter > 0) {
            "Номер главы должен быть больше 0"
        }

        require(verseStart > 0) {
            "Начальный стих должен быть больше 0"
        }

        require(verseEnd >= verseStart) {
            "Конечный стих не может быть меньше начального"
        }
    }
}
package org.holypresenter_bible.domain

import kotlinx.serialization.Serializable

@Serializable
data class BibleTranslation(
    /*
     * Стабильный id перевода.
     *
     * Например:
     * synodal
     * kjv
     */
    val id: String,

    val name: String,

    /*
     * Короткое имя для интерфейса.
     *
     * Например:
     * Синодальный
     * KJV
     */
    val abbreviation: String,

    /*
     * Язык в виде стандартного кода.
     *
     * ru
     * en
     */
    val language: String,

    val books: List<BibleBook>
)
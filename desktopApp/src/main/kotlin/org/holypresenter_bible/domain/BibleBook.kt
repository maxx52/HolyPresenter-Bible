package org.holypresenter_bible.domain

import kotlinx.serialization.Serializable

@Serializable
data class BibleBook(
    /*
     * Стабильный машинный идентификатор.
     *
     * Например:
     * genesis
     * psalms
     * john
     * romans
     */
    val id: String,

    /*
     * Название для пользователя.
     *
     * Например:
     * Бытие
     * Псалтирь
     * От Иоанна
     */
    val name: String,

    /*
     * Короткое название для ссылок.
     *
     * Например:
     * Быт
     * Пс
     * Ин
     */
    val abbreviation: String,

    val testament: BibleTestament,

    /*
     * Порядок книги в данном переводе/каноне.
     */
    val order: Int,

    val chapters: List<BibleChapter>
)
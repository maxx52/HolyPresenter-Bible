package org.holypresenter_bible.domain

import kotlinx.serialization.Serializable

@Serializable
data class BibleChapter(
    val number: Int,
    val verses: List<BibleVerse>
)
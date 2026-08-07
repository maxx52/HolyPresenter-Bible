package org.holypresenter_bible.domain

import kotlinx.serialization.Serializable

@Serializable
data class BibleVerse(
    val number: Int,
    val text: String
)
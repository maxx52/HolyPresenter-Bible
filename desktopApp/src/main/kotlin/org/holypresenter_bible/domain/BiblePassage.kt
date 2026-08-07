package org.holypresenter_bible.domain

import kotlinx.serialization.Serializable

@Serializable
data class BiblePassage(
    val reference: BibleReference,
    val verses: List<BibleVerse>
)
package org.holypresenter_bible.repository

import org.holypresenter_bible.domain.BiblePassage
import org.holypresenter_bible.domain.BibleReference
import org.holypresenter_bible.domain.BibleTranslation

interface BibleRepository {
    fun getTranslations(): List<BibleTranslation>

    fun findTranslation(
        translationId: String
    ): BibleTranslation?

    fun getPassage(
        reference: BibleReference
    ): BiblePassage?
}
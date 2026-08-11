package org.holypresenter_bible.ui

import holypresenter.org.platform.api.projection.ProjectionContent
import holypresenter.org.platform.api.projection.ProjectionService
import org.holypresenter_bible.domain.BibleBook
import org.holypresenter_bible.domain.BibleChapter
import org.holypresenter_bible.domain.BibleReference
import org.holypresenter_bible.domain.BibleTranslation
import org.holypresenter_bible.presentation.BiblePresentationFactory
import org.holypresenter_bible.repository.BibleRepository

/**
 * Отправляет выбранный отрывок на проектор непосредственно из рабочей области Bible.
 *
 * Состояние проектора остаётся в Platform, а модуль формирует только своё
 * представление отрывка через существующий ProjectionService.
 */
internal fun showBibleSelectionOnProjector(
    projectionService: ProjectionService?,
    repository: BibleRepository,
    translation: BibleTranslation,
    book: BibleBook,
    chapter: BibleChapter,
    selection: BibleVerseSelection
): Boolean {
    val service = projectionService ?: return false

    val reference =
        BibleReference(
            translationId = translation.id,
            bookId = book.id,
            chapter = chapter.number,
            verseStart = selection.start,
            verseEnd = selection.end
        )

    val passage = repository.getPassage(reference) ?: return false
    service.show(
        ProjectionContent.Slide(
            presentation =
                BiblePresentationFactory().create(
                    passage = passage,
                    bookAbbreviation = book.abbreviation
                ),
            slideIndex = 0
        )
    )

    return true
}

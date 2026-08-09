package org.holypresenter_bible.planner

import holypresenter.org.platform.api.planner.PlannerItem
import holypresenter.org.platform.api.planner.PlannerItemHandler
import org.holypresenter_bible.domain.BibleReference
import org.holypresenter_bible.repository.BibleRepository

internal class BiblePlannerItemHandler(
    private val repository: BibleRepository,
    private val onActivateReference: (BibleReference) -> Unit
) : PlannerItemHandler {
    override val moduleId: String = "bible"

    override fun activate(
        item: PlannerItem
    ): Boolean {
        if (
            item.reference.moduleId !=
            moduleId
        ) {
            return false
        }

        val reference = BiblePlannerReferenceCodec.decode(item.reference.itemId) ?: return false
        val passage = repository.getPassage(reference) ?: return false

        if (passage.verses.isEmpty()) {
            return false
        }

        onActivateReference(reference)
        return true
    }
}
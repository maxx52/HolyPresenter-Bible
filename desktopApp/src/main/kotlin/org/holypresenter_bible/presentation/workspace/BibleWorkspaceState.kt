package org.holypresenter_bible.presentation.workspace

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.holypresenter_bible.domain.BibleReference

data class BibleNavigationRequest(
    val id: Long,
    val reference: BibleReference
)

class BibleWorkspaceState {
    private var nextRequestId = 0L

    var navigationRequest by mutableStateOf<BibleNavigationRequest?>(null)
        private set

    /**
     * Открывает ссылку в Bible Navigator.
     *
     * Используется, в том числе при активации
     * элемента из Planner.
     */
    fun openReference(
        reference: BibleReference
    ) {
        requestNavigation(reference)
    }

    private fun requestNavigation(
        reference: BibleReference
    ) {
        nextRequestId += 1

        navigationRequest =
            BibleNavigationRequest(
                id = nextRequestId,
                reference = reference
            )
    }
}

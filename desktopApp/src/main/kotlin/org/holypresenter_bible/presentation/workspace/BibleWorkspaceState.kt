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

    var navigationRequest: BibleNavigationRequest? by mutableStateOf(null)
        private set

    fun openReference(
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
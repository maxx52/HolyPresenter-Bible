package org.holypresenter_bible.presentation.workspace

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.holypresenter_bible.domain.BibleReference

enum class BibleScreen {
    NAVIGATOR,
    PRESENTER
}

data class BibleNavigationRequest(
    val id: Long,
    val reference: BibleReference
)

class BibleWorkspaceState {
    private var nextRequestId = 0L

    var screen: BibleScreen by mutableStateOf(
        BibleScreen.NAVIGATOR
    )
        private set

    var navigationRequest:
            BibleNavigationRequest?
            by mutableStateOf(null)
        private set

    var presenterReference:
            BibleReference?
            by mutableStateOf(null)
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
        presenterReference = null
        screen = BibleScreen.NAVIGATOR
        requestNavigation(reference)
    }

    /**
     * Открывает выбранный отрывок
     * в режиме Presenter.
     *
     * Само открытие Presenter
     * ничего не выводит на проектор.
     */
    fun openPresenter(
        reference: BibleReference
    ) {
        presenterReference = reference
        screen = BibleScreen.PRESENTER
    }

    /**
     * Возвращается из Presenter
     * к тому же месту в Navigator.
     */
    fun backToNavigator() {
        val reference = presenterReference

        presenterReference = null
        screen = BibleScreen.NAVIGATOR

        if (reference != null) {
            requestNavigation(reference)
        }
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
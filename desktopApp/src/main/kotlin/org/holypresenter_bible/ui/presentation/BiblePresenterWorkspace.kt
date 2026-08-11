package org.holypresenter_bible.ui.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import holypresenter.org.platform.api.module.ModuleContext
import holypresenter.org.platform.api.projection.ProjectionContent
import holypresenter.org.platform.api.projection.ProjectionService
import org.holypresenter.platform.ui.presenter.HolyProjectionToolbar
import org.holypresenter_bible.domain.BibleReference
import org.holypresenter_bible.presentation.BiblePresentationFactory
import org.holypresenter_bible.repository.BibleRepository
import java.awt.event.KeyEvent
import androidx.compose.runtime.DisposableEffect
import java.awt.KeyEventDispatcher
import java.awt.KeyboardFocusManager

@Composable
fun BiblePresenterWorkspace(
    moduleContext: ModuleContext,
    repository: BibleRepository,
    reference: BibleReference,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val projectionService =
        remember(moduleContext) {
            moduleContext.services.get(
                ProjectionService::class
            )
        }

    val projectionState =
        projectionService
            ?.state
            ?.collectAsState()

    val isBlackScreen =
        projectionState
            ?.value
            ?.content == ProjectionContent.BlackScreen

    val isTextHidden =
        projectionState
            ?.value
            ?.textVisible == false

    val translation =
        remember(
            reference.translationId
        ) {
            repository.findTranslation(reference.translationId)
        }

    val book =
        remember(
            translation,
            reference.bookId
        ) {
            translation
                ?.books
                ?.firstOrNull {
                    it.id == reference.bookId
                }
        }

    val passage =
        remember(reference) {
            repository.getPassage(
                reference
            )
        }

    val presentationFactory =
        remember {
            BiblePresentationFactory()
        }

    val presentation =
        remember(
            passage,
            book?.abbreviation
        ) {
            if (
                passage != null &&
                book != null
            ) {
                presentationFactory.create(
                    passage = passage,
                    bookAbbreviation = book.abbreviation
                )
            } else {
                null
            }
        }

    var selectedSlideIndex by
    remember(reference) {
        mutableStateOf(0)
    }

    /*
     * Presenter открывается без автоматического вывода на проектор:
     * оператор сначала видит выбранный отрывок и сам запускает показ.
     * Это состояние отделяет такой запуск от обычной навигации по уже
     * показанным стихам.
     */
    var isProjectionStarted by
    remember(reference) {
        mutableStateOf(false)
    }

    if (
        passage == null ||
        book == null ||
        presentation == null
    ) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Библейский отрывок не найден",
                    style = MaterialTheme.typography.titleLarge
                )

                TextButton(
                    onClick = onBackClick
                ) {
                    Text("← Вернуться")
                }
            }
        }
        return
    }

    val verses = passage.verses

    fun showSlide(index: Int) {
        if (verses.isEmpty()) {
            return
        }

        val safeIndex =
            index.coerceIn(
                minimumValue = 0,
                maximumValue = verses.lastIndex
            )

        selectedSlideIndex = safeIndex

        projectionService?.show(
            ProjectionContent.Slide(
                presentation = presentation,
                slideIndex = safeIndex
            )
        )

        isProjectionStarted = projectionService != null
    }

    fun showPreviousSlide() {
        if (!isProjectionStarted) {
            showSlide(selectedSlideIndex)
            return
        }

        showSlide(selectedSlideIndex - 1)
    }

    fun showNextSlide() {
        if (!isProjectionStarted) {
            showSlide(selectedSlideIndex)
            return
        }

        showSlide(selectedSlideIndex + 1)
    }

    val selectedVerse = verses.getOrNull(selectedSlideIndex)

    LaunchedEffect(projectionState?.value?.visible) {
        if (projectionState?.value?.visible == false) {
            isProjectionStarted = false
        }
    }

    DisposableEffect(
        reference,
        verses.size
    ) {
        val keyboardManager =
            KeyboardFocusManager
                .getCurrentKeyboardFocusManager()

        val dispatcher =
            KeyEventDispatcher { event ->
                if (
                    event.id !=
                    KeyEvent.KEY_PRESSED
                ) {
                    return@KeyEventDispatcher false
                }

                when (event.keyCode) {
                    KeyEvent.VK_RIGHT,
                    KeyEvent.VK_DOWN,
                    KeyEvent.VK_PAGE_DOWN,
                    KeyEvent.VK_SPACE -> {
                        showNextSlide()
                        true
                    }

                    KeyEvent.VK_LEFT,
                    KeyEvent.VK_UP,
                    KeyEvent.VK_PAGE_UP -> {
                        showPreviousSlide()
                        true
                    }

                    KeyEvent.VK_ESCAPE -> {
                        projectionService?.close()
                        true
                    }

                    KeyEvent.VK_B -> {
                        projectionService?.toggleBlackScreen()
                        true
                    }

                    KeyEvent.VK_C -> {
                        projectionService?.toggleTextVisibility()
                        true
                    }
                    else -> false
                }
            }

        keyboardManager
            .addKeyEventDispatcher(dispatcher)

        onDispose {
            keyboardManager
                .removeKeyEventDispatcher(dispatcher)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = onBackClick
            ) {
                Text("← Библия")
            }

            Text(
                text = presentation.metadata.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = translation?.abbreviation.orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                enabled =
                    verses.isNotEmpty() &&
                    projectionService != null,
                onClick = {
                    showSlide(selectedSlideIndex)
                }
            ) {
                Text(
                    text = if (isProjectionStarted) {
                        "Показать текущий"
                    } else {
                        "Показать на проекторе"
                    }
                )
            }

            OutlinedButton(
                enabled =
                    isProjectionStarted &&
                    selectedSlideIndex > 0,
                onClick = {
                    showPreviousSlide()
                }
            ) {
                Text("← Предыдущий")
            }

            Button(
                enabled =
                    isProjectionStarted &&
                    selectedSlideIndex < verses.lastIndex,
                onClick = {
                    showNextSlide()
                }
            ) {
                Text("Следующий →")
            }

            Spacer(modifier = Modifier.weight(1f))

            HolyProjectionToolbar(
                isBlackScreen = isBlackScreen,
                isTextHidden = isTextHidden,
                enabled = projectionService != null,
                compact = false,
                onToggleBlackScreen = {
                    projectionService?.toggleBlackScreen()
                },
                onToggleTextVisibility = {
                    projectionService?.toggleTextVisibility()
                },
                onCloseProjection = {
                    projectionService?.close()
                }
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            LazyColumn(
                modifier = Modifier
                    .width(360.dp)
                    .fillMaxHeight(),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(
                    items = verses,
                    key = { _, verse ->
                        verse.number
                    }
                ) {
                    index,
                    verse ->

                    val selected = selectedSlideIndex == index

                    Card(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showSlide(index)
                                },
                        colors =
                            CardDefaults
                                .cardColors(
                                    containerColor =
                                        if (selected) {
                                            MaterialTheme.colorScheme.secondaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.surfaceVariant
                                        }
                                )
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp)
                        ) {
                            Text(
                                text = "${book.abbreviation}. ${reference.chapter}:${verse.number}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = verse.text,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 4
                            )
                        }
                    }
                }
            }

            Surface(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                shape = MaterialTheme.shapes.large,
                tonalElevation = 2.dp
            ) {
                if (selectedVerse != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(36.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = selectedVerse.text,
                            style = MaterialTheme.typography.headlineMedium,
                            textAlign = TextAlign.Center
                        )

                        Spacer(
                            modifier = Modifier.height(28.dp)
                        )

                        Text(
                            text = "${book.abbreviation}. ${reference.chapter}:${selectedVerse.number}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Локальный предпросмотр",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

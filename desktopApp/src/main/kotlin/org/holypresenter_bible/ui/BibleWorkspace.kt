package org.holypresenter_bible.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.isShiftPressed
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import holypresenter.org.platform.api.module.ModuleContext
import holypresenter.org.platform.api.planner.PlannerItem
import holypresenter.org.platform.api.planner.PlannerReference
import holypresenter.org.platform.api.planner.PlannerService
import org.holypresenter_bible.domain.BibleBook
import org.holypresenter_bible.domain.BibleChapter
import org.holypresenter_bible.domain.BibleReference
import org.holypresenter_bible.domain.BibleTestament
import org.holypresenter_bible.domain.BibleTranslation
import org.holypresenter_bible.planner.BiblePlannerReferenceCodec
import org.holypresenter_bible.presentation.workspace.BibleScreen
import org.holypresenter_bible.presentation.workspace.BibleWorkspaceState
import org.holypresenter_bible.repository.BibleRepository
import org.holypresenter_bible.ui.presentation.BiblePresenterWorkspace

@Composable
fun BibleWorkspace(
    moduleContext: ModuleContext,
    repository: BibleRepository,
    workspaceState: BibleWorkspaceState,
    modifier: Modifier = Modifier
) {
    if (workspaceState.screen == BibleScreen.PRESENTER) {
        val reference = workspaceState.presenterReference

        if (reference != null) {
            BiblePresenterWorkspace(
                moduleContext = moduleContext,
                repository = repository,
                reference = reference,
                onBackClick = {
                    workspaceState.backToNavigator()
                },
                modifier = modifier
            )
        }
        return
    }

    val translations =
        remember(repository) {
            repository.getTranslations()
        }

    var selectedTranslation by remember {
        mutableStateOf(
            translations.firstOrNull()
        )
    }

    var selectedBook by remember {
        mutableStateOf<BibleBook?>(null)
    }

    var selectedChapter by remember {
        mutableStateOf<BibleChapter?>(null)
    }

    var showBooks by remember {
        mutableStateOf(true)
    }

    var showChapters by remember {
        mutableStateOf(false)
    }

    var verseSelection by remember {
        mutableStateOf<BibleVerseSelection?>(null)
    }

    val navigationRequest = workspaceState.navigationRequest

    LaunchedEffect(navigationRequest?.id) {
        val reference = navigationRequest
            ?.reference
            ?: return@LaunchedEffect

        val translation = translations
            .firstOrNull {
                it.id == reference.translationId
            }
            ?: return@LaunchedEffect

        val book = translation.books
            .firstOrNull {
                it.id == reference.bookId
            }
            ?: return@LaunchedEffect

        val chapter = book.chapters
            .firstOrNull {
                it.number == reference.chapter
            }
            ?: return@LaunchedEffect

        selectedTranslation = translation
        selectedBook = book
        selectedChapter = chapter

        verseSelection =
            BibleVerseSelection(
                anchor = reference.verseStart,
                focus = reference.verseEnd
            )

        showBooks = false
        showChapters = false
    }

    val selectedReference =
        if (
            selectedTranslation != null &&
            selectedBook != null &&
            selectedChapter != null &&
            verseSelection != null
        ) {
            BibleReference(
                translationId = selectedTranslation!!.id,
                bookId = selectedBook!!.id,
                chapter = selectedChapter!!.number,
                verseStart = verseSelection!!.start,
                verseEnd = verseSelection!!.end
            )
        } else {
            null
        }

    val selectedReferenceTitle =
        if (selectedReference != null && selectedBook != null) {
            val verses =
                if (selectedReference.verseStart == selectedReference.verseEnd) {
                    selectedReference
                        .verseStart
                        .toString()
                } else {
                    "${selectedReference.verseStart}–${selectedReference.verseEnd}"
                }

            "${selectedBook!!.abbreviation}. ${selectedReference.chapter}:$verses"
        } else {
            null
        }

    val plannerService =
        remember(moduleContext) {
            moduleContext.services.get(
                PlannerService::class
            )
        }

    Surface(
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {

            BibleHeader(
                translations = translations,
                selectedTranslation = selectedTranslation,
                onTranslationSelected = { translation ->
                    selectedTranslation = translation
                    selectedBook = null
                    selectedChapter = null
                    verseSelection = null
                    showBooks = true
                    showChapters = false
                }
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            if (selectedTranslation == null) {
                EmptyBibleState()
                return@Column
            }

            BibleBreadcrumb(
                translation = selectedTranslation,
                book = selectedBook,
                chapter = selectedChapter,
                onTranslationClick = {
                    selectedBook = null
                    selectedChapter = null
                    showBooks = true
                    showChapters = false
                },
                onBookClick = {
                    showBooks = true
                    showChapters = false
                },
                onChapterClick = {
                    showBooks = false
                    showChapters = true
                }
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            HorizontalDivider()

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            when {
                showBooks -> {
                    BookPicker(
                        translation = selectedTranslation!!,
                        selectedBook = selectedBook,
                        onBookSelected = { book ->
                            selectedBook = book
                            selectedChapter = null
                            verseSelection = null
                            showBooks = false
                            showChapters = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                }

                showChapters && selectedBook != null -> {
                    ChapterPicker(
                        book = selectedBook!!,
                        selectedChapter = selectedChapter,
                        onChapterSelected = { chapter ->
                            selectedChapter = chapter
                            verseSelection = null
                            showBooks = false
                            showChapters = false
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                }

                selectedBook != null && selectedChapter != null -> {
                    VerseList(
                        book = selectedBook!!,
                        chapter = selectedChapter!!,
                        selection = verseSelection,
                        onVerseClick = {
                                verseNumber,
                                extendSelection ->
                            verseSelection =
                                if (
                                    extendSelection && verseSelection != null
                                ) {
                                    verseSelection!!
                                        .extendTo(
                                            verseNumber
                                        )
                                } else {
                                    BibleVerseSelection(
                                        anchor = verseNumber
                                    )
                                }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                }
            }

            if (
                !showBooks &&
                !showChapters &&
                selectedBook != null &&
                selectedChapter != null &&
                verseSelection != null
            ) {
                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                BibleSelectionBar(
                    book = selectedBook!!,
                    chapter = selectedChapter!!,
                    selection = verseSelection!!,
                    onAddToPlanner = {
                        val reference = selectedReference ?: return@BibleSelectionBar
                        val title = selectedReferenceTitle ?: return@BibleSelectionBar

                        plannerService?.add(
                            PlannerItem.Generic(
                                reference =
                                    PlannerReference(
                                        moduleId = "bible",
                                        itemId = BiblePlannerReferenceCodec.encode(reference)
                                    ),
                                title = title
                            )
                        )
                    },
                    onOpenPresenter = {
                        val reference = selectedReference ?: return@BibleSelectionBar
                        workspaceState.openPresenter(reference)
                    },
                    onClear = {
                        verseSelection = null
                    }
                )
            }
        }
    }
}

@Composable
private fun BibleHeader(
    translations: List<BibleTranslation>,
    selectedTranslation: BibleTranslation?,
    onTranslationSelected: (BibleTranslation) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "Библия",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold
        )

        TranslationSelector(
            translations = translations,
            selectedTranslation = selectedTranslation,
            onTranslationSelected = onTranslationSelected
        )
    }
}

@Composable
private fun TranslationSelector(
    translations: List<BibleTranslation>,
    selectedTranslation: BibleTranslation?,
    onTranslationSelected: (BibleTranslation) -> Unit
) {
    var expanded by remember {
        mutableStateOf(false)
    }

    Box {
        OutlinedButton(
            onClick = {
                expanded = true
            }
        ) {
            Text(
                selectedTranslation?.abbreviation ?: "Перевод"
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
            }
        ) {
            translations.forEach { translation ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(
                                text = translation.name
                            )

                            Text(
                                text = translation.abbreviation,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    },
                    onClick = {
                        expanded = false
                        onTranslationSelected(translation)
                    }
                )
            }
        }
    }
}

@Composable
private fun BibleBreadcrumb(
    translation: BibleTranslation?,
    book: BibleBook?,
    chapter: BibleChapter?,
    onTranslationClick: () -> Unit,
    onBookClick: () -> Unit,
    onChapterClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(
            onClick = onTranslationClick
        ) {
            Text(
                text = translation?.abbreviation ?: "Перевод"
            )
        }

        if (book != null) {
            Text("›")
            TextButton(
                onClick = onBookClick
            ) {
                Text(book.name)
            }
        }

        if (chapter != null) {
            Text("›")
            TextButton(
                onClick = onChapterClick
            ) {
                Text(
                    text = chapter.number.toString()
                )
            }
        }
    }
}

@Composable
private fun BookPicker(
    translation: BibleTranslation,
    selectedBook: BibleBook?,
    onBookSelected: (BibleBook) -> Unit,
    modifier: Modifier = Modifier
) {
    val oldTestament =
        translation.books
            .filter {
                it.testament == BibleTestament.OLD
            }
            .sortedBy {
                it.order
            }

    val newTestament =
        translation.books
            .filter {
                it.testament == BibleTestament.NEW
            }
            .sortedBy {
                it.order
            }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 110.dp),
        modifier = modifier,
        contentPadding = PaddingValues(bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item(
            span = {
                GridItemSpan(maxLineSpan)
            }
        ) {
            TestamentHeader(
                title = "Ветхий Завет"
            )
        }

        items(
            items = oldTestament,
            key = {
                it.id
            }
        ) { book ->
            BookTile(
                book = book,
                selected = selectedBook?.id == book.id,
                onClick = {
                    onBookSelected(book)
                }
            )
        }

        item(
            span = {
                GridItemSpan(maxLineSpan)
            }
        ) {
            TestamentHeader(
                title = "Новый Завет"
            )
        }

        items(
            items = newTestament,
            key = {
                it.id
            }
        ) { book ->
            BookTile(
                book = book,
                selected = selectedBook?.id == book.id,
                onClick = {
                    onBookSelected(book)
                }
            )
        }
    }
}

@Composable
private fun TestamentHeader(
    title: String
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
    )
}

@Composable
private fun BookTile(
    book: BibleBook,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(74.dp)
            .clickable(
                onClick = onClick
            ),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (selected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        when (
                            book.testament
                        ) {
                            BibleTestament.OLD ->
                                MaterialTheme.colorScheme.surfaceVariant

                            BibleTestament.NEW ->
                                MaterialTheme.colorScheme.tertiaryContainer
                        }
                    }
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = book.abbreviation,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )

            Text(
                text = book.name,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ChapterPicker(
    book: BibleBook,
    selectedChapter: BibleChapter?,
    onChapterSelected: (BibleChapter) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
    ) {
        Text(
            text = book.name,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )

        Text(
            text = "Выберите главу",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        LazyVerticalGrid(
            columns =
                GridCells.Adaptive(minSize = 68.dp),
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(
                items =
                    book.chapters
                        .sortedBy {
                            it.number
                        },
                key = {
                    it.number
                }
            ) { chapter ->
                ChapterTile(
                    chapter = chapter,
                    selected = selectedChapter?.number == chapter.number,
                    onClick = {
                        onChapterSelected(chapter)
                    }
                )
            }
        }
    }
}

@Composable
private fun ChapterTile(
    chapter: BibleChapter,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .height(64.dp)
            .widthIn(min = 64.dp)
            .clickable(
                onClick = onClick
            ),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (selected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
            )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = chapter.number.toString(),
                style = MaterialTheme.typography.titleLarge
            )
        }
    }
}

@Composable
private fun VerseList(
    book: BibleBook,
    chapter: BibleChapter,
    selection: BibleVerseSelection?,
    onVerseClick: (
        verseNumber: Int,
        extendSelection: Boolean
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    val windowInfo = LocalWindowInfo.current

    Column(
        modifier = modifier
    ) {
        Row(
            modifier =
                Modifier.fillMaxWidth(),
            verticalAlignment =
                Alignment.CenterVertically,
            horizontalArrangement =
                Arrangement.SpaceBetween
        ) {
            Text(
                text =
                    "${book.name}, ${chapter.number}",
                style =
                    MaterialTheme
                        .typography
                        .titleLarge,
                fontWeight =
                    FontWeight.SemiBold
            )

            Text(
                text =
                    "Клик — выбрать • Shift+клик — диапазон",
                style =
                    MaterialTheme
                        .typography
                        .bodySmall,
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
            )
        }

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        LazyColumn(
            modifier =
                Modifier.fillMaxSize(),
            verticalArrangement =
                Arrangement.spacedBy(4.dp),
            contentPadding =
                PaddingValues(
                    bottom = 24.dp
                )
        ) {
            items(
                items =
                    chapter.verses
                        .sortedBy {
                            it.number
                        },
                key = {
                    it.number
                }
            ) { verse ->

                val selected =
                    selection
                        ?.contains(
                            verse.number
                        )
                        ?: false

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onVerseClick(
                                verse.number,
                                windowInfo
                                    .keyboardModifiers
                                    .isShiftPressed
                            )
                        },
                    shape = MaterialTheme.shapes.medium,
                    color =
                        if (selected) {
                            MaterialTheme.colorScheme.secondaryContainer
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = 16.dp,
                                vertical = 12.dp
                            ),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = verse.number.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            color =
                                if (selected) {
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.primary
                                },
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = verse.text,
                            style = MaterialTheme.typography.bodyLarge,
                            color =
                                if (selected) {
                                    MaterialTheme
                                        .colorScheme
                                        .onSecondaryContainer
                                } else {
                                    MaterialTheme
                                        .colorScheme
                                        .onSurface
                                },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BibleSelectionBar(
    book: BibleBook,
    chapter: BibleChapter,
    selection: BibleVerseSelection,
    onAddToPlanner: () -> Unit,
    onOpenPresenter: () -> Unit,
    onClear: () -> Unit
) {
    val versePart =
        if (
            selection.start == selection.end
        ) {
            selection.start.toString()
        } else {
            "${selection.start}–${selection.end}"
        }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 3.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 16.dp,
                    vertical = 10.dp
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Выбранный отрывок",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "${book.abbreviation}. ${chapter.number}:" + versePart,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onAddToPlanner
                ) {
                    Text("+ В план")
                }

                Button(
                    onClick = onOpenPresenter
                ) {
                    Text("Открыть в Presenter")
                }

                TextButton(
                    onClick = onClear
                ) {
                    Text("Снять выбор")
                }
            }
        }
    }
}

@Composable
private fun EmptyBibleState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Переводы Библии не найдены",
            style = MaterialTheme.typography.titleMedium
        )
    }
}
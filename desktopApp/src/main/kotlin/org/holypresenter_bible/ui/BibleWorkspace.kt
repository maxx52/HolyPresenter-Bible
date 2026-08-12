package org.holypresenter_bible.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.isShiftPressed
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import holypresenter.org.platform.api.module.ModuleContext
import holypresenter.org.platform.api.planner.PlannerItem
import holypresenter.org.platform.api.planner.PlannerReference
import holypresenter.org.platform.api.planner.PlannerService
import org.holypresenter_bible.domain.BibleBook
import org.holypresenter_bible.domain.BibleChapter
import org.holypresenter_bible.domain.BiblePassage
import org.holypresenter_bible.domain.BibleReference
import org.holypresenter_bible.domain.BibleTestament
import org.holypresenter_bible.domain.BibleTranslation
import org.holypresenter_bible.planner.BiblePlannerReferenceCodec
import org.holypresenter_bible.presentation.BibleProjectionContentFactory
import org.holypresenter_bible.presentation.workspace.BibleWorkspaceState
import org.holypresenter_bible.repository.BibleRepository
import java.awt.KeyEventDispatcher
import java.awt.KeyboardFocusManager
import java.awt.event.KeyEvent

@Composable
fun BibleWorkspace(
    moduleContext: ModuleContext,
    repository: BibleRepository,
    workspaceState: BibleWorkspaceState,
    defaultTranslationId: String? = null,
    modifier: Modifier = Modifier
) {
    val translations =
        remember(repository) {
            repository.getTranslations()
        }

    var selectedTranslation by remember {
        mutableStateOf(
            translations
                .firstOrNull { translation ->
                    translation.id == defaultTranslationId
            }
                ?: translations.firstOrNull()
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

    var previewVerseNumber by remember {
        mutableStateOf<Int?>(null)
    }

    var previewBackground by remember { mutableStateOf(Color.Black) }
    var previewTextColor by remember { mutableStateOf(Color.White) }
    var previewScale by remember { mutableStateOf(1f) }
    var previewTextAlign by remember { mutableStateOf(TextAlign.Center) }

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

        previewVerseNumber = reference.verseStart
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

    val selectedPassage =
        remember(
            repository,
            selectedReference
        ) {
            selectedReference?.let { reference ->
                repository.getPassage(reference)
            }
        }

    val plannerService =
        remember(moduleContext) {
            moduleContext.services.get(
                PlannerService::class
            )
        }

    val selectAndPreviewVerse: (
        verseNumber: Int,
        extendSelection: Boolean
    ) -> Unit = { verseNumber, extendSelection ->
        val translation = selectedTranslation
        val book = selectedBook
        val chapter = selectedChapter

        if (
            translation != null &&
            book != null &&
            chapter != null
        ) {
            val newSelection =
                if (
                    extendSelection &&
                    verseSelection != null
                ) {
                    verseSelection!!.extendTo(verseNumber)
                } else {
                    BibleVerseSelection(
                        anchor = verseNumber
                    )
                }

            verseSelection = newSelection
            previewVerseNumber = verseNumber
        }
    }

    val latestChapter =
        rememberUpdatedState(selectedChapter)
    val latestPassageVisible =
        rememberUpdatedState(
            !showBooks &&
                !showChapters
        )
    val latestPreviewVerseNumber =
        rememberUpdatedState(previewVerseNumber)
    val latestSelectAndPreviewVerse =
        rememberUpdatedState(selectAndPreviewVerse)

    DisposableEffect(Unit) {
        val keyboardManager =
            KeyboardFocusManager
                .getCurrentKeyboardFocusManager()

        fun moveVerse(offset: Int): Boolean {
            if (!latestPassageVisible.value) {
                return false
            }

            val chapter = latestChapter.value ?: return false
            val currentVerseNumber =
                latestPreviewVerseNumber.value
                    ?: return false
            val verses =
                chapter.verses.sortedBy {
                    it.number
                }
            val currentIndex =
                verses.indexOfFirst {
                    it.number == currentVerseNumber
                }

            if (currentIndex < 0) {
                return false
            }

            val targetVerse =
                verses.getOrNull(
                    currentIndex + offset
                )
                    ?: return true

            latestSelectAndPreviewVerse.value(
                targetVerse.number,
                false
            )
            return true
        }

        val dispatcher =
            KeyEventDispatcher { event ->
                if (
                    event.id != KeyEvent.KEY_PRESSED ||
                    event.isControlDown ||
                    event.isAltDown ||
                    event.isMetaDown
                ) {
                    return@KeyEventDispatcher false
                }

                when (event.keyCode) {
                    KeyEvent.VK_RIGHT,
                    KeyEvent.VK_DOWN,
                    KeyEvent.VK_PAGE_DOWN,
                    KeyEvent.VK_SPACE ->
                        moveVerse(1)

                    KeyEvent.VK_LEFT,
                    KeyEvent.VK_UP,
                    KeyEvent.VK_PAGE_UP ->
                        moveVerse(-1)

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
                    previewVerseNumber = null
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
                            previewVerseNumber = null
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
                            previewVerseNumber = null
                            showBooks = false
                            showChapters = false
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                }

                selectedBook != null && selectedChapter != null -> {
                    BiblePassageWorkspace(
                        book = selectedBook!!,
                        chapter = selectedChapter!!,
                        passage = selectedPassage,
                        selection = verseSelection,
                        previewVerseNumber = previewVerseNumber,
                        onVerseClick = selectAndPreviewVerse,
                        previewBackground = previewBackground,
                        previewTextColor = previewTextColor,
                        previewScale = previewScale,
                        previewTextAlign = previewTextAlign,
                        onPreviewBackgroundChange = { previewBackground = it },
                        onPreviewTextColorChange = { previewTextColor = it },
                        onPreviewScaleChange = { previewScale = it },
                        onPreviewTextAlignChange = { previewTextAlign = it },
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
private fun BiblePassageWorkspace(
    book: BibleBook,
    chapter: BibleChapter,
    passage: BiblePassage?,
    selection: BibleVerseSelection?,
    previewVerseNumber: Int?,
    onVerseClick: (
        verseNumber: Int,
        extendSelection: Boolean
    ) -> Unit,
    previewBackground: Color,
    previewTextColor: Color,
    previewScale: Float,
    previewTextAlign: TextAlign,
    onPreviewBackgroundChange: (Color) -> Unit,
    onPreviewTextColorChange: (Color) -> Unit,
    onPreviewScaleChange: (Float) -> Unit,
    onPreviewTextAlignChange: (TextAlign) -> Unit,
    modifier: Modifier = Modifier
) {
    val previewContent =
        remember(
            passage,
            book.abbreviation
        ) {
            passage?.let { selectedPassage ->
                BibleProjectionContentFactory.create(
                    passage = selectedPassage,
                    bookAbbreviation = book.abbreviation
                )
            }
        }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        VerseList(
            book = book,
            chapter = chapter,
            selection = selection,
            previewVerseNumber = previewVerseNumber,
            onVerseClick = onVerseClick,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            Text(
                text = "Предпросмотр",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )

            Text("Оформление", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = { onPreviewBackgroundChange(if (previewBackground == Color.Black) Color(0xFF24334A) else Color.Black) }, label = { Text("Фон") })
                AssistChip(onClick = { onPreviewBackgroundChange(Color(0xFF4A284D)) }, label = { Text("Тёплый") })
                AssistChip(onClick = { onPreviewTextColorChange(if (previewTextColor == Color.White) Color(0xFFFFE9A8) else Color.White) }, label = { Text("Текст") })
                AssistChip(onClick = { onPreviewTextAlignChange(if (previewTextAlign == TextAlign.Center) TextAlign.Start else TextAlign.Center) }, label = { Text(if (previewTextAlign == TextAlign.Center) "По центру" else "Слева") })
                Text("Масштаб", modifier = Modifier.align(Alignment.CenterVertically))
                Slider(value = previewScale, onValueChange = onPreviewScaleChange, valueRange = 0.7f..1.3f, modifier = Modifier.weight(1f))
            }

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f),
                shape = MaterialTheme.shapes.large,
                tonalElevation = 2.dp
            ) {
                if (previewContent != null) {
                    BiblePreview(
                        text = previewContent.text,
                        caption = previewContent.reference,
                        background = previewBackground,
                        textColor = previewTextColor,
                        scale = previewScale,
                        textAlign = previewTextAlign
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Выберите стих — он появится\nв предпросмотре",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BiblePreview(
    text: String,
    caption: String,
    background: Color,
    textColor: Color,
    scale: Float,
    textAlign: TextAlign
) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize().background(background).padding(28.dp)
    ) {
        val maximumSize = 56f * scale
        var fontSize by remember(text, maxWidth, maxHeight, scale) {
            mutableStateOf(maximumSize)
        }
        val minimumSize = 14f

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment =
                if (textAlign == TextAlign.Center) Alignment.CenterHorizontally else Alignment.Start
        ) {
            Text(
                text = text,
                modifier = Modifier.fillMaxWidth().weight(1f),
                color = textColor,
                fontSize = fontSize.sp,
                lineHeight = (fontSize * 1.2f).sp,
                textAlign = textAlign,
                fontWeight = FontWeight.SemiBold,
                overflow = TextOverflow.Clip,
                onTextLayout = { layout ->
                    if (layout.hasVisualOverflow && fontSize > minimumSize) {
                        fontSize = (fontSize - 1f).coerceAtLeast(minimumSize)
                    }
                }
            )

            Spacer(Modifier.height(18.dp))
            Text(
                text = caption,
                color = textColor.copy(alpha = .82f),
                fontSize = (fontSize * .62f).coerceAtLeast(14f).sp
            )
        }
    }
}

@Composable
private fun VerseList(
    book: BibleBook,
    chapter: BibleChapter,
    selection: BibleVerseSelection?,
    previewVerseNumber: Int?,
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
                    "Клик — показать • Shift+клик — диапазон",
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

                val shownInPreview =
                    previewVerseNumber == verse.number

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
                        when {
                            shownInPreview ->
                                MaterialTheme.colorScheme.primaryContainer

                            selected ->
                                MaterialTheme.colorScheme.secondaryContainer

                            else ->
                                MaterialTheme.colorScheme.surface
                        },
                    border =
                        if (shownInPreview) {
                            BorderStroke(
                                width = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            null
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
                                when {
                                    shownInPreview ->
                                        MaterialTheme.colorScheme.onPrimaryContainer

                                    selected ->
                                        MaterialTheme.colorScheme.onSecondaryContainer

                                    else ->
                                        MaterialTheme.colorScheme.primary
                                },
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = verse.text,
                            style = MaterialTheme.typography.bodyLarge,
                            color =
                                when {
                                    shownInPreview ->
                                        MaterialTheme.colorScheme.onPrimaryContainer

                                    selected ->
                                        MaterialTheme.colorScheme.onSecondaryContainer

                                    else ->
                                        MaterialTheme.colorScheme.onSurface
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
    onAddToPlanner: () -> Unit
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

            OutlinedButton(
                onClick = onAddToPlanner
            ) {
                Text("+ В план")
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

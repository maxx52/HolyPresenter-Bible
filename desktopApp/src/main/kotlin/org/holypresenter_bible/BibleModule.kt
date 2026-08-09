package org.holypresenter_bible

import androidx.compose.runtime.Composable
import holypresenter.org.platform.api.module.HolyModule
import holypresenter.org.platform.api.module.ModuleContext
import holypresenter.org.platform.api.module.ModuleMetadata
import holypresenter.org.platform.api.planner.PlannerItemHandlerRegistry
import org.holypresenter_bible.planner.BiblePlannerItemHandler
import org.holypresenter_bible.presentation.workspace.BibleWorkspaceState
import org.holypresenter_bible.repository.JsonBibleRepository
import org.holypresenter_bible.ui.BibleWorkspace
import java.io.File

class BibleModule : HolyModule {
    private val repository =
        JsonBibleRepository(
            translationsDirectory = resolveTranslationsDirectory()
        )

    private val workspaceState = BibleWorkspaceState()
    private lateinit var context: ModuleContext

    private var plannerItemHandlerRegistry: PlannerItemHandlerRegistry? = null

    override val metadata =
        ModuleMetadata(
            id = "bible",
            name = "Библия",
            version = "1.0.0",
            apiVersion = "0.6.0",
            author = "HolyPresenter",
            description = "Bible presentation module",
            icon = "📖"
        )

    override fun onLoad(context: ModuleContext) {
        this.context = context
        val translations = repository.getTranslations()
        println("[Bible] Loaded translations: " + translations.joinToString { it.name })
    }

    override fun onEnable(
        context: ModuleContext
    ) {
        val registry = context.services.get(PlannerItemHandlerRegistry::class)

        plannerItemHandlerRegistry = registry

        registry?.register(
            BiblePlannerItemHandler(
                repository = repository,
                onActivateReference = workspaceState::openReference
            )
        )
    }

    override fun onDisable() {
        plannerItemHandlerRegistry
            ?.unregister(
                metadata.id
            )
        plannerItemHandlerRegistry = null
    }

    @Composable
    override fun Workspace() {
        BibleWorkspace(
            moduleContext = context,
            repository = repository,
            workspaceState = workspaceState
        )
    }

    private fun resolveTranslationsDirectory(): File {
        val localAppData =
            System.getenv("LOCALAPPDATA")
                ?.takeIf { path ->
                    path.isNotBlank()
                }
                ?.let(::File)

        val applicationDataDirectory =
            if (localAppData != null) {
                File(
                    localAppData,
                    "HolyPresenter"
                )
            } else {
                File(
                    System.getProperty("user.home"),
                    ".holypresenter"
                )
            }

        return File(
            applicationDataDirectory,
            "bible/translations"
        ).absoluteFile
    }
}
package org.holypresenter_bible

import androidx.compose.runtime.Composable
import holypresenter.org.platform.api.module.HolyModule
import holypresenter.org.platform.api.module.ModuleContext
import holypresenter.org.platform.api.module.ModuleMetadata
import org.holypresenter_bible.repository.JsonBibleRepository
import org.holypresenter_bible.ui.BibleWorkspace
import java.io.File

class BibleModule : HolyModule {
    private val repository =
        JsonBibleRepository(
            translationsDirectory = resolveTranslationsDirectory()
        )

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

    override fun onLoad(
        context: ModuleContext
    ) {
        val translations = repository.getTranslations()

        println(
            "[Bible] Loaded translations: " + translations
                .joinToString {
                    it.name
                }
        )
    }

    @Composable
    override fun Workspace() {
        BibleWorkspace(
            repository = repository
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
                    System.getProperty(
                        "user.home"
                    ),
                    ".holypresenter"
                )
            }

        return File(
            applicationDataDirectory,
            "bible/translations"
        ).absoluteFile
    }
}
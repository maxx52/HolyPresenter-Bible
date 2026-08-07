package org.holypresenter_bible

import androidx.compose.runtime.Composable
import holypresenter.org.platform.api.module.HolyModule
import holypresenter.org.platform.api.module.ModuleMetadata
import org.holypresenter_bible.ui.BibleWorkspace

class BibleModule : HolyModule {
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

    @Composable
    override fun Workspace() {
        BibleWorkspace()
    }
}
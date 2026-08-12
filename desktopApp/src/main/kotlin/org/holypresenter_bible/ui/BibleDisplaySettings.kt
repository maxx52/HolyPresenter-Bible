package org.holypresenter_bible.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.style.TextAlign
import java.util.prefs.Preferences

internal data class BibleDisplaySettings(
    val background: Color = Color.Black,
    val textColor: Color = Color.White,
    val textAlign: TextAlign = TextAlign.Center,
    val backgroundImagePath: String? = null,
    val videoPath: String? = null,
    val fontFamilyName: String = "SansSerif"
)

internal object BibleDisplaySettingsStorage {
    private val preferences =
        Preferences.userRoot().node("org/holypresenter/bible/display")

    fun load(): BibleDisplaySettings =
        BibleDisplaySettings(
            background = Color(preferences.getInt("background", Color.Black.toArgb())),
            textColor = Color(preferences.getInt("textColor", Color.White.toArgb())),
            textAlign =
                if (preferences.get("textAlign", "center") == "start") TextAlign.Start else TextAlign.Center,
            backgroundImagePath = preferences.get("backgroundImagePath", null),
            videoPath = preferences.get("videoPath", null),
            fontFamilyName = preferences.get("fontFamilyName", "SansSerif")
        )

    fun save(settings: BibleDisplaySettings) {
        preferences.putInt("background", settings.background.toArgb())
        preferences.putInt("textColor", settings.textColor.toArgb())
        preferences.put("textAlign", if (settings.textAlign == TextAlign.Start) "start" else "center")
        preferences.put("fontFamilyName", settings.fontFamilyName)
        savePath("backgroundImagePath", settings.backgroundImagePath)
        savePath("videoPath", settings.videoPath)
    }

    private fun savePath(key: String, value: String?) {
        if (value.isNullOrBlank()) preferences.remove(key) else preferences.put(key, value)
    }
}

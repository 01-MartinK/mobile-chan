package com.mk.mobilechan.ui.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.Nightlight
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mk.mobilechan.R
import com.mk.mobilechan.ui.theme.Background
import com.mk.mobilechan.ui.theme.BackgroundDark
import com.mk.mobilechan.ui.theme.MobileChanTheme
import com.mk.mobilechan.ui.navigation.AppModules
import com.mk.mobilechan.ui.theme.ThemeMode
import androidx.core.content.edit

private const val SETTINGS_PREFS = "settings"
private const val KEY_ALLOW_NSFW = "allow_nsfw"
private const val KEY_THEME = "theme"
private const val KEY_WELCOME_COMPLETED = "welcome_completed"
private const val KEY_ENABLED_MODULES = "enabled_modules"
private val ICONS_MAP = mapOf(
    Pair(ThemeMode.LIGHT, Icons.Outlined.WbSunny),
    Pair(ThemeMode.DARK, Icons.Outlined.Nightlight),
    Pair(ThemeMode.DYNAMIC, Icons.Outlined.PhoneAndroid)
)

class UserSettings internal constructor(
    allowNsfw: Boolean,
    theme: ThemeMode,
    welcomeCompleted: Boolean,
    enabledModules: Set<AppModules>,
    private val prefs: SharedPreferences,
) {
    var allowNsfw by mutableStateOf(allowNsfw)
        private set
    var theme by mutableStateOf(theme)
        private set
    var welcomeCompleted by mutableStateOf(welcomeCompleted)
        private set
    var enabledModules by mutableStateOf(enabledModules)
        private set

    fun updateAllowNsfw(value: Boolean) {
        allowNsfw = value
        prefs.edit { putBoolean(KEY_ALLOW_NSFW, value) }
    }

    fun updateTheme(value: ThemeMode) {
        theme = value
        prefs.edit { putString(KEY_THEME, value.name) }
    }

    fun completeWelcome(modules: Set<AppModules>, isAdult: Boolean) {
        enabledModules = modules
        allowNsfw = isAdult
        welcomeCompleted = true
        prefs.edit {
            putBoolean(KEY_WELCOME_COMPLETED, true)
            putBoolean(KEY_ALLOW_NSFW, isAdult)
            putStringSet(KEY_ENABLED_MODULES, modules.map { it.name }.toSet())
        }
    }
}

@Composable
fun rememberUserSettings(): UserSettings {
    val context = LocalContext.current
    return remember {
        val prefs = context.getSharedPreferences(SETTINGS_PREFS, Context.MODE_PRIVATE)
        UserSettings(
            allowNsfw = prefs.getBoolean(KEY_ALLOW_NSFW, false),
            theme = themeModeFromName(prefs.getString(KEY_THEME, null)),
            welcomeCompleted = prefs.getBoolean(KEY_WELCOME_COMPLETED, false),
            enabledModules = modulesFromNames(prefs.getStringSet(KEY_ENABLED_MODULES, null)),
            prefs = prefs,
        )
    }
}

private fun themeModeFromName(name: String?): ThemeMode =
    ThemeMode.entries.firstOrNull { it.name == name } ?: ThemeMode.LIGHT

private fun modulesFromNames(names: Set<String>?): Set<AppModules> {
    if (names == null) return AppModules.entries.toSet()
    return names.mapNotNull { name ->
        AppModules.entries.find { it.name == name }
    }.toSet()
}

@Composable
fun SettingsScreen(
    allowNsfw: Boolean,
    onAllowNsfwChange: (Boolean) -> Unit,
    theme: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        ListItem(
            headlineContent = {
                Text(
                    text = stringResource(R.string.settings_allow_nsfw),
                    style = MaterialTheme.typography.bodyLarge,
                )
            },
            trailingContent = {
                Switch(
                    checked = allowNsfw,
                    onCheckedChange = onAllowNsfwChange,
                )
            },
            modifier = Modifier.clickable { onAllowNsfwChange(!allowNsfw) },
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Text(
            text = stringResource(R.string.settings_theme),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ThemeMode.entries.forEach { mode ->
                ThemeOptionBox(
                    mode = mode,
                    selected = theme == mode,
                    onClick = { onThemeChange(mode) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun ThemeOptionBox(
    mode: ThemeMode,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(12.dp)
    val icon = if (ICONS_MAP[mode] != null) ICONS_MAP[mode] else null
    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }
    Column(
        modifier = modifier
            .clip(shape)
            .border(2.dp, borderColor, shape)
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.4f)
                .background(themePreview(mode)),
        ) {
            if (icon != null) {
                Icon(
                    modifier = Modifier.width(32.dp).height(32.dp),
                    imageVector = icon,
                    contentDescription = "",
                )
            }
        }
        Text(
            text = stringResource(mode.labelRes),
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            modifier = Modifier.padding(vertical = 10.dp),
        )
    }
}

private val ThemeMode.labelRes: Int
    get() = when (this) {
        ThemeMode.LIGHT -> R.string.settings_theme_light
        ThemeMode.DARK -> R.string.settings_theme_dark
        ThemeMode.DYNAMIC -> R.string.settings_theme_dynamic
    }

private fun themePreview(mode: ThemeMode): Brush = when (mode) {
    ThemeMode.LIGHT -> SolidColor(Background)
    ThemeMode.DARK -> SolidColor(BackgroundDark)
    ThemeMode.DYNAMIC -> Brush.linearGradient(
        listOf(Color(0xFFD0BCFF), Color(0xFF4F378B), Color(0xFF7D5260)),
    )
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    MobileChanTheme {
        SettingsScreen(
            allowNsfw = false,
            onAllowNsfwChange = {},
            theme = ThemeMode.LIGHT,
            onThemeChange = {},
        )
    }
}

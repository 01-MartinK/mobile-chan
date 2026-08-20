package com.mk.mobilechan.ui.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Nightlight
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
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
private const val KEY_ACTIVE_SOURCE = "active_source"
private const val KEY_EXCLUDED_BOARDS = "excluded_boards"
private const val KEY_SHOW_IMAGES = "show_images"
private const val KEY_SHOW_VIDEOS = "show_videos"
// Official 4chan non-worksafe boards (ws_board = 0).
private val DEFAULT_EXCLUDED_BOARDS = setOf(
    "aco",
    "b",
    "bant",
    "d",
    "e",
    "gif",
    "h",
    "hc",
    "hm",
    "hr",
    "pol",
    "r9k",
    "s",
    "s4s",
    "soc",
    "t",
    "trash",
    "u",
    "y",
)
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
    activeSource: AppModules,
    excludedBoards: Set<String>,
    showImages: Boolean,
    showVideos: Boolean,
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
    var activeSource by mutableStateOf(activeSource)
        private set
    var excludedBoards by mutableStateOf(excludedBoards)
        private set
    var showImages by mutableStateOf(showImages)
        private set
    var showVideos by mutableStateOf(showVideos)
        private set

    fun updateAllowNsfw(value: Boolean) {
        allowNsfw = value
        prefs.edit { putBoolean(KEY_ALLOW_NSFW, value) }
    }

    fun updateTheme(value: ThemeMode) {
        theme = value
        prefs.edit { putString(KEY_THEME, value.name) }
    }

    fun updateShowImages(value: Boolean) {
        showImages = value
        prefs.edit { putBoolean(KEY_SHOW_IMAGES, value) }
    }

    fun updateShowVideos(value: Boolean) {
        showVideos = value
        prefs.edit { putBoolean(KEY_SHOW_VIDEOS, value) }
    }

    fun addExcludedBoard(raw: String): Boolean {
        val tag = normalizeBoardTag(raw) ?: return false
        if (tag !in excludedBoards) {
            excludedBoards = excludedBoards + tag
            persistExcludedBoards()
        }
        return true
    }

    fun removeExcludedBoard(tag: String) {
        if (tag !in excludedBoards) return
        excludedBoards = excludedBoards - tag
        persistExcludedBoards()
    }

    fun updateActiveSource(value: AppModules) {
        activeSource = value
        prefs.edit { putString(KEY_ACTIVE_SOURCE, value.name) }
    }

    fun completeWelcome(modules: Set<AppModules>, isAdult: Boolean) {
        enabledModules = modules
        allowNsfw = isAdult
        welcomeCompleted = true
        val first = modules.firstOrNull() ?: AppModules.FOUR_CHAN
        activeSource = first
        prefs.edit {
            putBoolean(KEY_WELCOME_COMPLETED, true)
            putBoolean(KEY_ALLOW_NSFW, isAdult)
            putStringSet(KEY_ENABLED_MODULES, modules.map { it.name }.toSet())
            putString(KEY_ACTIVE_SOURCE, first.name)
        }
    }

    private fun persistExcludedBoards() {
        prefs.edit { putStringSet(KEY_EXCLUDED_BOARDS, excludedBoards) }
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
            activeSource = activeSourceFromName(
                prefs.getString(KEY_ACTIVE_SOURCE, null),
                modulesFromNames(prefs.getStringSet(KEY_ENABLED_MODULES, null)),
            ),
            excludedBoards = excludedBoardsFromNames(prefs.getStringSet(KEY_EXCLUDED_BOARDS, null)),
            showImages = prefs.getBoolean(KEY_SHOW_IMAGES, true),
            showVideos = prefs.getBoolean(KEY_SHOW_VIDEOS, true),
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

internal fun activeSourceFromName(name: String?, enabled: Set<AppModules>): AppModules {
    val requested = AppModules.entries.find { it.name == name }
    if (requested != null && (enabled.isEmpty() || requested in enabled)) return requested
    return enabled.firstOrNull() ?: AppModules.FOUR_CHAN
}

private fun excludedBoardsFromNames(names: Set<String>?): Set<String> {
    if (names == null) return DEFAULT_EXCLUDED_BOARDS
    return names.mapNotNull(::normalizeBoardTag).toSet()
}

internal fun normalizeBoardTag(raw: String): String? {
    val tag = raw.trim().trim('/').lowercase()
    return tag.takeIf { it.isNotEmpty() }
}

@Composable
fun SettingsScreen(
    allowNsfw: Boolean,
    onAllowNsfwChange: (Boolean) -> Unit,
    theme: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
    excludedBoards: Set<String>,
    onAddExcludedBoard: (String) -> Boolean,
    onRemoveExcludedBoard: (String) -> Unit,
    showImages: Boolean,
    onShowImagesChange: (Boolean) -> Unit,
    showVideos: Boolean,
    onShowVideosChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        SettingsSectionTitle(stringResource(R.string.settings_boards))
        SettingsSwitch(
            label = stringResource(R.string.settings_allow_nsfw),
            checked = allowNsfw,
            onCheckedChange = onAllowNsfwChange,
        )
        ExcludedBoardsSetting(
            excludedBoards = excludedBoards,
            onAddExcludedBoard = onAddExcludedBoard,
            onRemoveExcludedBoard = onRemoveExcludedBoard,
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        SettingsSectionTitle(stringResource(R.string.settings_threads))
        SettingsSwitch(
            label = stringResource(R.string.settings_show_images),
            checked = showImages,
            onCheckedChange = onShowImagesChange,
        )
        SettingsSwitch(
            label = stringResource(R.string.settings_show_videos),
            checked = showVideos,
            onCheckedChange = onShowVideosChange,
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        SettingsSectionTitle(stringResource(R.string.settings_theme))
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
private fun SettingsSwitch(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    ListItem(
        headlineContent = {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
            )
        },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
            )
        },
        modifier = Modifier.clickable { onCheckedChange(!checked) },
    )
}

@Composable
private fun SettingsSectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
    )
}

@Composable
private fun ExcludedBoardsSetting(
    excludedBoards: Set<String>,
    onAddExcludedBoard: (String) -> Boolean,
    onRemoveExcludedBoard: (String) -> Unit,
) {
    var boardTagInput by rememberSaveable { mutableStateOf("") }
    var excludedExpanded by rememberSaveable { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val shape = RoundedCornerShape(12.dp)

    fun submitBoardTag() {
        if (onAddExcludedBoard(boardTagInput)) {
            boardTagInput = ""
            excludedExpanded = true
            focusManager.clearFocus()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_excluded_boards),
            style = MaterialTheme.typography.bodyLarge,
        )
        OutlinedTextField(
            value = boardTagInput,
            onValueChange = { boardTagInput = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.settings_exclude_board_hint)) },
            singleLine = true,
            trailingIcon = {
                IconButton(onClick = ::submitBoardTag) {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = stringResource(R.string.settings_exclude_board_add),
                    )
                }
            },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
                keyboardType = KeyboardType.Ascii,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { submitBoardTag() }),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { excludedExpanded = !excludedExpanded }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.settings_excluded_boards),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Icon(
                    imageVector = if (excludedExpanded) {
                        Icons.Outlined.ExpandLess
                    } else {
                        Icons.Outlined.ExpandMore
                    },
                    contentDescription = stringResource(
                        if (excludedExpanded) {
                            R.string.settings_excluded_boards_collapse
                        } else {
                            R.string.settings_excluded_boards_expand
                        },
                    ),
                )
            }
            AnimatedVisibility(visible = excludedExpanded) {
                Column {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    if (excludedBoards.isEmpty()) {
                        Text(
                            text = stringResource(R.string.settings_excluded_boards_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp),
                        )
                    } else {
                        val sortedBoards = remember(excludedBoards) { excludedBoards.sorted() }
                        sortedBoards.forEachIndexed { index, tag ->
                            if (index > 0) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp, end = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = stringResource(R.string.board_tag, tag),
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.weight(1f),
                                )
                                IconButton(onClick = { onRemoveExcludedBoard(tag) }) {
                                    Icon(
                                        imageVector = Icons.Outlined.Close,
                                        contentDescription = stringResource(
                                            R.string.settings_exclude_board_remove,
                                        ),
                                    )
                                }
                            }
                        }
                    }
                }
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
            excludedBoards = setOf("b", "pol"),
            onAddExcludedBoard = { true },
            onRemoveExcludedBoard = {},
            showImages = true,
            onShowImagesChange = {},
            showVideos = true,
            onShowVideosChange = {},
        )
    }
}

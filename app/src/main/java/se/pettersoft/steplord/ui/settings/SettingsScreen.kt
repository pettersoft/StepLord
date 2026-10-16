package se.pettersoft.steplord.ui.settings

import android.Manifest
import android.app.LocaleManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import se.pettersoft.steplord.R
import se.pettersoft.steplord.data.local.UnitSystem
import se.pettersoft.steplord.data.local.UserSettings
import se.pettersoft.steplord.ui.components.DailyGoalDialog
import se.pettersoft.steplord.ui.components.formatHeight
import java.text.NumberFormat
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var notificationsEnabled by remember { mutableStateOf(context.areNotificationsEnabled()) }
    // The system only shows the permission dialog a limited number of times; after the first
    // request, send the user to the app's notification settings instead
    var notificationPermissionRequested by rememberSaveable { mutableStateOf(false) }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        notificationsEnabled = granted
        notificationPermissionRequested = true
    }
    LifecycleResumeEffect(Unit) {
        notificationsEnabled = context.areNotificationsEnabled()
        onPauseOrDispose {}
    }
    val allowNotifications = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !notificationPermissionRequested) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            context.openNotificationSettings()
        }
    }

    // Re-read on resume since the language is changed in system settings
    var appLanguage by remember { mutableStateOf(context.appLanguageName()) }
    LifecycleResumeEffect(Unit) {
        appLanguage = context.appLanguageName()
        onPauseOrDispose {}
    }

    SettingsContent(
        settings = settings,
        notificationsEnabled = notificationsEnabled,
        appLanguage = appLanguage,
        onOpenLanguageSettings = context::openAppLanguageSettings,
        onBack = onBack,
        onAllowNotifications = allowNotifications,
        onDailyGoalChange = viewModel::setDailyGoal,
        onHeightChange = viewModel::setHeightCm,
        onUnitSystemChange = viewModel::setUnitSystem,
        onSummaryEnabledChange = { enabled ->
            viewModel.setSummaryEnabled(enabled)
            if (enabled && !notificationsEnabled) allowNotifications()
        },
        onSummaryTimeChange = viewModel::setSummaryTime,
        onGoalNotificationEnabledChange = { enabled ->
            viewModel.setGoalNotificationEnabled(enabled)
            if (enabled && !notificationsEnabled) allowNotifications()
        },
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsContent(
    settings: UserSettings?,
    notificationsEnabled: Boolean,
    appLanguage: String?,
    onOpenLanguageSettings: () -> Unit,
    onBack: () -> Unit,
    onAllowNotifications: () -> Unit,
    onDailyGoalChange: (Int) -> Unit,
    onHeightChange: (Int) -> Unit,
    onUnitSystemChange: (UnitSystem) -> Unit,
    onSummaryEnabledChange: (Boolean) -> Unit,
    onSummaryTimeChange: (LocalTime) -> Unit,
    onGoalNotificationEnabledChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val numberFormat = remember { NumberFormat.getIntegerInstance() }
    val timeFormatter = remember { DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT) }
    var showGoalDialog by rememberSaveable { mutableStateOf(false) }
    var showTimeDialog by rememberSaveable { mutableStateOf(false) }
    var showHeightDialog by rememberSaveable { mutableStateOf(false) }
    var showUnitsDialog by rememberSaveable { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(stringResource(R.string.settings_title)) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_back),
                        contentDescription = stringResource(R.string.navigate_back),
                    )
                }
            },
            // The surrounding Scaffold already handles the status bar
            windowInsets = WindowInsets(0.dp),
        )
        if (settings == null) return@Column

        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            SectionHeader(stringResource(R.string.settings_section_goal))
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_daily_goal)) },
                supportingContent = {
                    Text(stringResource(R.string.settings_daily_goal_value, numberFormat.format(settings.dailyGoal)))
                },
                modifier = Modifier.clickable { showGoalDialog = true },
            )

            // Used to estimate distance walked
            SectionHeader(stringResource(R.string.settings_section_profile))
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_height)) },
                supportingContent = { Text(formatHeight(settings.heightCm, settings.unitSystem)) },
                modifier = Modifier.clickable { showHeightDialog = true },
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_units)) },
                supportingContent = { Text(stringResource(settings.unitSystem.labelRes)) },
                modifier = Modifier.clickable { showUnitsDialog = true },
            )

            SectionHeader(stringResource(R.string.settings_section_notifications))
            if (!notificationsEnabled) {
                ListItem(
                    headlineContent = {
                        Text(
                            text = stringResource(R.string.settings_notifications_blocked),
                            color = MaterialTheme.colorScheme.error,
                        )
                    },
                    trailingContent = {
                        TextButton(onClick = onAllowNotifications) {
                            Text(stringResource(R.string.settings_allow_notifications))
                        }
                    },
                )
            }
            SwitchItem(
                title = stringResource(R.string.settings_summary),
                description = stringResource(R.string.settings_summary_description),
                checked = settings.summaryEnabled,
                onCheckedChange = onSummaryEnabledChange,
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_summary_time)) },
                supportingContent = { Text(settings.summaryTime.format(timeFormatter)) },
                modifier = Modifier
                    .clickable(enabled = settings.summaryEnabled) { showTimeDialog = true }
                    .alpha(if (settings.summaryEnabled) 1f else 0.38f),
            )
            SwitchItem(
                title = stringResource(R.string.settings_goal_notification),
                description = stringResource(R.string.settings_goal_notification_description),
                checked = settings.goalNotificationEnabled,
                onCheckedChange = onGoalNotificationEnabledChange,
            )

            // The system's per-app language setting exists on Android 13+; older versions use the device language
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                SectionHeader(stringResource(R.string.settings_section_general))
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_language)) },
                    supportingContent = { Text(appLanguage ?: stringResource(R.string.settings_language_system)) },
                    modifier = Modifier.clickable(onClick = onOpenLanguageSettings),
                )
            }
        }
    }

    if (showGoalDialog && settings != null) {
        DailyGoalDialog(
            initialGoal = settings.dailyGoal,
            onDismiss = { showGoalDialog = false },
            onConfirm = { goal ->
                onDailyGoalChange(goal)
                showGoalDialog = false
            },
        )
    }
    if (showHeightDialog && settings != null) {
        HeightDialog(
            initialHeightCm = settings.heightCm,
            unitSystem = settings.unitSystem,
            onDismiss = { showHeightDialog = false },
            onConfirm = { heightCm ->
                onHeightChange(heightCm)
                showHeightDialog = false
            },
        )
    }
    if (showUnitsDialog && settings != null) {
        UnitSystemDialog(
            current = settings.unitSystem,
            onDismiss = { showUnitsDialog = false },
            onSelect = { unitSystem ->
                onUnitSystemChange(unitSystem)
                showUnitsDialog = false
            },
        )
    }
    if (showTimeDialog && settings != null) {
        SummaryTimeDialog(
            initialTime = settings.summaryTime,
            onDismiss = { showTimeDialog = false },
            onConfirm = { time ->
                onSummaryTimeChange(time)
                showTimeDialog = false
            },
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 8.dp),
    )
}

@Composable
private fun SwitchItem(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(description) },
        trailingContent = { Switch(checked = checked, onCheckedChange = null) },
        modifier = Modifier.toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SummaryTimeDialog(initialTime: LocalTime, onDismiss: () -> Unit, onConfirm: (LocalTime) -> Unit) {
    val context = LocalContext.current
    val state = rememberTimePickerState(
        initialHour = initialTime.hour,
        initialMinute = initialTime.minute,
        is24Hour = DateFormat.is24HourFormat(context),
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_summary_time)) },
        text = { TimePicker(state = state) },
        confirmButton = {
            TextButton(onClick = { onConfirm(LocalTime.of(state.hour, state.minute)) }) {
                Text(stringResource(R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

/** Name of the language chosen for this app in system settings, or null when following the device. */
private fun Context.appLanguageName(): String? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return null
    val locales = getSystemService(LocaleManager::class.java).applicationLocales
    if (locales.isEmpty) return null
    val locale = locales[0]
    return locale.getDisplayName(locale).replaceFirstChar { it.titlecase(locale) }
}

private fun Context.openAppLanguageSettings() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    startActivity(
        Intent(Settings.ACTION_APP_LOCALE_SETTINGS, Uri.fromParts("package", packageName, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}

private fun Context.areNotificationsEnabled(): Boolean = NotificationManagerCompat.from(this).areNotificationsEnabled()

private fun Context.openNotificationSettings() {
    startActivity(
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}

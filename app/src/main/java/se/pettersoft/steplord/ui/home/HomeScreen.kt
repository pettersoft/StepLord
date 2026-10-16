package se.pettersoft.steplord.ui.home

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import se.pettersoft.steplord.BuildConfig
import se.pettersoft.steplord.R
import se.pettersoft.steplord.ui.components.DailyGoalDialog
import se.pettersoft.steplord.ui.components.StepCircle
import se.pettersoft.steplord.ui.components.StepsBarChart
import se.pettersoft.steplord.ui.components.formatDistance
import se.pettersoft.steplord.ui.components.formatDuration
import se.pettersoft.steplord.util.hasActivityRecognitionPermission
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HomeScreen(
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasPermission by remember { mutableStateOf(context.hasActivityRecognitionPermission()) }
    var permissionDenied by rememberSaveable { mutableStateOf(false) }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* Notifications are optional; Settings offers to enable them later */ }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasPermission = granted
        permissionDenied = !granted
        // Ask for notifications right after step access, while the user is setting things up
        if (granted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    // The permission can change in system settings while the app is in the background
    LifecycleResumeEffect(Unit) {
        hasPermission = context.hasActivityRecognitionPermission()
        onPauseOrDispose {}
    }

    val canTrack = hasPermission && viewModel.hasStepSensor
    if (canTrack) {
        LaunchedEffect(Unit) {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) { viewModel.trackSteps() }
        }
    }

    var showGoalDialog by rememberSaveable { mutableStateOf(false) }
    val showSteps = hasPermission || !viewModel.hasStepSensor
    // Until step access is granted the circle starts the permission flow; afterwards it edits the goal
    val onCircleClick: () -> Unit = when {
        showSteps -> { { showGoalDialog = true } }
        permissionDenied -> { { context.openAppSettings() } }
        else -> { { permissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION) } }
    }

    HomeContent(
        uiState = uiState,
        showSteps = showSteps,
        message = when {
            !viewModel.hasStepSensor -> stringResource(R.string.no_step_sensor)
            permissionDenied -> stringResource(R.string.permission_denied)
            !hasPermission -> stringResource(R.string.permission_needed)
            uiState.progress >= 1f -> stringResource(R.string.goal_reached)
            else -> stringResource(R.string.goal_progress, (uiState.progress * 100).toInt())
        },
        onCircleClick = onCircleClick,
        onOpenSettings = onOpenSettings,
        onSelectPeriod = viewModel::selectPeriod,
        onPreviousPeriod = viewModel::showPreviousPeriod,
        onNextPeriod = viewModel::showNextPeriod,
        onSeedSampleData = viewModel::seedSampleData,
        modifier = modifier,
    )

    if (showGoalDialog) {
        DailyGoalDialog(
            initialGoal = uiState.goal,
            onDismiss = { showGoalDialog = false },
            onConfirm = { goal ->
                viewModel.setDailyGoal(goal)
                showGoalDialog = false
            },
        )
    }
}

@Composable
private fun HomeContent(
    uiState: HomeUiState,
    showSteps: Boolean,
    message: String,
    onCircleClick: () -> Unit,
    onOpenSettings: () -> Unit,
    onSelectPeriod: (ChartPeriod) -> Unit,
    onPreviousPeriod: () -> Unit,
    onNextPeriod: () -> Unit,
    onSeedSampleData: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val numberFormat = remember { NumberFormat.getIntegerInstance() }
    val dateText = remember(uiState.date) {
        // Locale-appropriate order and punctuation, e.g. "Thursday 15 October", "jueves, 15 de octubre"
        val locale = Locale.getDefault()
        val pattern = DateFormat.getBestDateTimePattern(locale, "EEEEdMMMM")
        uiState.date.format(DateTimeFormatter.ofPattern(pattern, locale))
            .replaceFirstChar { it.titlecase(locale) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(
                text = dateText,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            IconButton(onClick = onOpenSettings, modifier = Modifier.align(Alignment.CenterEnd)) {
                Icon(
                    painter = painterResource(R.drawable.ic_settings),
                    contentDescription = stringResource(R.string.open_settings),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        StepCircle(
            progress = if (showSteps) uiState.progress else 0f,
            onClick = onCircleClick,
            onClickLabel = if (showSteps) stringResource(R.string.edit_goal) else null,
            // Portrait-only on phones, but Android 16+ ignores the orientation lock on large
            // screens. The max width must come before fillMaxWidth or it has no effect.
            modifier = Modifier
                .widthIn(max = 340.dp)
                .fillMaxWidth(0.85f),
        ) {
            if (showSteps) {
                Text(
                    text = numberFormat.format(uiState.todaySteps),
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = pluralStringResource(R.plurals.steps_label, uiState.todaySteps),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(
                        R.string.walking_estimate,
                        formatDistance(uiState.distanceMeters, uiState.unitSystem),
                        formatDuration(uiState.walkingMinutes),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Text(
                    text = stringResource(R.string.goal_label, numberFormat.format(uiState.goal)),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            } else {
                Text(text = stringResource(R.string.tap_to_start), style = MaterialTheme.typography.headlineMedium)
                Text(text = stringResource(R.string.tap_to_start_detail), style = MaterialTheme.typography.titleMedium)
            }
        }

        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        ChartCard(
            chart = uiState.chart,
            today = uiState.date,
            onSelectPeriod = onSelectPeriod,
            onPreviousPeriod = onPreviousPeriod,
            onNextPeriod = onNextPeriod,
        )

        if (BuildConfig.DEBUG) {
            TextButton(onClick = onSeedSampleData) {
                Text(stringResource(R.string.add_sample_data))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChartCard(
    chart: ChartUiState,
    today: LocalDate,
    onSelectPeriod: (ChartPeriod) -> Unit,
    onPreviousPeriod: () -> Unit,
    onNextPeriod: () -> Unit,
) {
    val numberFormat = remember { NumberFormat.getIntegerInstance() }
    val periods = listOf(ChartPeriod.WEEK to R.string.chart_week, ChartPeriod.MONTH to R.string.chart_month)

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                periods.forEachIndexed { index, (period, label) ->
                    SegmentedButton(
                        selected = chart.period == period,
                        onClick = { onSelectPeriod(period) },
                        shape = SegmentedButtonDefaults.itemShape(index, periods.size),
                    ) {
                        Text(stringResource(label))
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onPreviousPeriod, enabled = chart.canGoBack) {
                    Icon(
                        painter = painterResource(R.drawable.ic_chevron_left),
                        contentDescription = stringResource(R.string.previous_period),
                    )
                }
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = periodLabel(chart.period, chart.range, today),
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = stringResource(
                            R.string.period_summary,
                            numberFormat.format(chart.total),
                            numberFormat.format(chart.dailyAverage),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
                IconButton(onClick = onNextPeriod, enabled = chart.canGoForward) {
                    Icon(
                        painter = painterResource(R.drawable.ic_chevron_right),
                        contentDescription = stringResource(R.string.next_period),
                    )
                }
            }
            StepsBarChart(entries = chart.bars, modifier = Modifier.fillMaxWidth())
        }
    }
}

/**
 * "12 Oct - 18 Oct" for a week and "October 2026" for a month, in the current locale's format.
 * Weeks include the year when they are not in the current year.
 */
private fun periodLabel(period: ChartPeriod, range: PeriodRange, today: LocalDate): String {
    val locale = Locale.getDefault()
    fun format(date: LocalDate, skeleton: String) =
        date.format(DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, skeleton), locale))
    return when (period) {
        ChartPeriod.WEEK -> {
            val skeleton = if (range.start.year == today.year && range.end.year == today.year) "dMMM" else "dMMMy"
            "${format(range.start, skeleton)} – ${format(range.end, skeleton)}"
        }
        ChartPeriod.MONTH -> format(range.start, "MMMMy").replaceFirstChar { it.titlecase(locale) }
    }
}

private fun Context.openAppSettings() {
    startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}

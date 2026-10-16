package se.pettersoft.steplord.ui.components

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.KeyboardType
import se.pettersoft.steplord.R
import java.text.NumberFormat

const val MIN_DAILY_GOAL = 1_000
const val MAX_DAILY_GOAL = 100_000

/** Edits the daily step goal; used from both the start page and Settings. */
@Composable
fun DailyGoalDialog(initialGoal: Int, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    val numberFormat = remember { NumberFormat.getIntegerInstance() }
    // Starts with the current goal selected, so typing replaces it
    var field by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        val text = initialGoal.toString()
        mutableStateOf(TextFieldValue(text, selection = TextRange(0, text.length)))
    }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    val goal = field.text.toIntOrNull()
    val isValid = goal != null && goal in MIN_DAILY_GOAL..MAX_DAILY_GOAL

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_daily_goal)) },
        text = {
            OutlinedTextField(
                value = field,
                onValueChange = { value -> field = value.copy(text = value.text.filter(Char::isDigit).take(6)) },
                modifier = Modifier.focusRequester(focusRequester),
                singleLine = true,
                isError = !isValid,
                supportingText = {
                    if (!isValid) {
                        Text(
                            stringResource(
                                R.string.settings_daily_goal_invalid,
                                numberFormat.format(MIN_DAILY_GOAL),
                                numberFormat.format(MAX_DAILY_GOAL),
                            ),
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
        },
        confirmButton = {
            TextButton(onClick = { goal?.let(onConfirm) }, enabled = isValid) { Text(stringResource(R.string.ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

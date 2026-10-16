package se.pettersoft.steplord.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import se.pettersoft.steplord.R
import se.pettersoft.steplord.data.local.UnitSystem
import se.pettersoft.steplord.ui.components.formatHeight
import se.pettersoft.steplord.util.MAX_HEIGHT_CM
import se.pettersoft.steplord.util.MIN_HEIGHT_CM
import se.pettersoft.steplord.util.cmToFeetAndInches
import se.pettersoft.steplord.util.feetAndInchesToCm

/** Edits the height in centimeters, or in feet and inches for imperial units. */
@Composable
fun HeightDialog(
    initialHeightCm: Int,
    unitSystem: UnitSystem,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    val (initialFeet, initialInches) = cmToFeetAndInches(initialHeightCm)
    var centimeters by rememberSaveable { mutableStateOf(initialHeightCm.toString()) }
    var feet by rememberSaveable { mutableStateOf(initialFeet.toString()) }
    var inches by rememberSaveable { mutableStateOf(initialInches.toString()) }

    val heightCm = when (unitSystem) {
        UnitSystem.METRIC -> centimeters.toIntOrNull()
        UnitSystem.IMPERIAL -> {
            val f = feet.toIntOrNull()
            val i = inches.toIntOrNull()
            if (f != null && i != null && i < 12) feetAndInchesToCm(f, i) else null
        }
    }
    val isValid = heightCm != null && heightCm in MIN_HEIGHT_CM..MAX_HEIGHT_CM
    val digits = { value: String -> value.filter(Char::isDigit).take(3) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_height)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                when (unitSystem) {
                    UnitSystem.METRIC -> NumberField(
                        value = centimeters,
                        onValueChange = { centimeters = digits(it) },
                        suffix = stringResource(R.string.unit_cm),
                        isError = !isValid,
                    )
                    UnitSystem.IMPERIAL -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumberField(
                            value = feet,
                            onValueChange = { feet = digits(it).take(1) },
                            suffix = stringResource(R.string.unit_ft),
                            isError = !isValid,
                            modifier = Modifier.weight(1f),
                        )
                        NumberField(
                            value = inches,
                            onValueChange = { inches = digits(it).take(2) },
                            suffix = stringResource(R.string.unit_in),
                            isError = !isValid,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                if (!isValid) {
                    Text(
                        text = stringResource(
                            R.string.settings_height_invalid,
                            formatHeight(MIN_HEIGHT_CM, unitSystem),
                            formatHeight(MAX_HEIGHT_CM, unitSystem),
                        ),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { heightCm?.let(onConfirm) }, enabled = isValid) { Text(stringResource(R.string.ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Composable
private fun NumberField(
    value: String,
    onValueChange: (String) -> Unit,
    suffix: String,
    isError: Boolean,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        isError = isError,
        suffix = { Text(suffix) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier,
    )
}

/** Chooses between metric and imperial units. */
@Composable
fun UnitSystemDialog(current: UnitSystem, onDismiss: () -> Unit, onSelect: (UnitSystem) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_units)) },
        text = {
            Column(modifier = Modifier.selectableGroup()) {
                UnitSystem.entries.forEach { unitSystem ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = unitSystem == current,
                                role = Role.RadioButton,
                                onClick = { onSelect(unitSystem) },
                            )
                            .padding(vertical = 8.dp),
                    ) {
                        RadioButton(selected = unitSystem == current, onClick = null)
                        Text(
                            text = stringResource(unitSystem.labelRes),
                            modifier = Modifier.padding(start = 16.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

val UnitSystem.labelRes: Int
    get() = when (this) {
        UnitSystem.METRIC -> R.string.settings_units_metric
        UnitSystem.IMPERIAL -> R.string.settings_units_imperial
    }

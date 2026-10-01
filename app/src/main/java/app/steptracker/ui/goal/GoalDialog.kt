package app.steptracker.ui.goal

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.defaultMinSize
import app.steptracker.R

/**
 * [onSave] returns true when the input was accepted. On false the dialog stays open and shows
 * the inline error, and the previous goal is kept.
 */
@Composable
fun GoalDialog(
    currentGoal: Int,
    onSave: (String) -> Boolean,
    onDismiss: () -> Unit,
) {
    var text by rememberSaveable { mutableStateOf(currentGoal.toString()) }
    var showError by rememberSaveable { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.goal_dialog_title)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = {
                    text = it
                    showError = false
                },
                label = { Text(stringResource(R.string.goal_dialog_label)) },
                isError = showError,
                supportingText = if (showError) {
                    { Text(stringResource(R.string.goal_error)) }
                } else {
                    null
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (onSave(text)) onDismiss() else showError = true },
                modifier = Modifier.defaultMinSize(minHeight = 48.dp),
            ) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.defaultMinSize(minHeight = 48.dp),
            ) { Text(stringResource(R.string.cancel)) }
        },
    )
}

package app.steptracker.ui.today

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.steptracker.R
import app.steptracker.ui.goal.GoalDialog
import app.steptracker.ui.permission.PermissionChecker
import app.steptracker.ui.permission.openAppSettingsIntent
import java.text.NumberFormat

/**
 * Stateful entry point: wires the ViewModel, lifecycle and the permission launcher. The request is
 * only launched when the status is NeedsRationale or Denied, which never happens below API 29.
 */
@SuppressLint("InlinedApi")
@Composable
fun TodayRoute(
    viewModel: TodayViewModel,
    onOpenHistory: () -> Unit,
) {
    val context = LocalContext.current
    val activity = context as Activity
    val checker = remember { PermissionChecker(context) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        checker.markAsked()
        viewModel.onResume(checker.status(activity))
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.onResume(checker.status(activity))
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    TodayScreen(
        state = state,
        onRequestPermission = { launcher.launch(Manifest.permission.ACTIVITY_RECOGNITION) },
        onOpenSettings = { context.startActivity(openAppSettingsIntent(context)) },
        onSaveGoal = { viewModel.saveGoal(it) == GoalResult.Saved },
        onOpenHistory = onOpenHistory,
    )
}

/** Stateless Today screen, so it can be tested with fake states. */
@Composable
fun TodayScreen(
    state: TodayUiState,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    onSaveGoal: (String) -> Boolean,
    onOpenHistory: () -> Unit,
) {
    var showGoalDialog by rememberSaveable { mutableStateOf(false) }
    val numbers = remember { NumberFormat.getIntegerInstance() }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when (state) {
                TodayUiState.Loading -> Text(stringResource(R.string.loading), modifier = Modifier.testTag("state_loading"))

                is TodayUiState.Active -> {
                    val stepsText = numbers.format(state.steps)
                    val goalText = numbers.format(state.goal)
                    val stepsDescription = stringResource(R.string.steps_count_description, stepsText)
                    val progressDescription = stringResource(R.string.progress_description, state.percent)
                    Column(
                        modifier = Modifier.testTag("state_active"),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        ProgressRing(
                            progress = state.progress,
                            modifier = Modifier
                                .size(280.dp)
                                .semantics { contentDescription = progressDescription },
                        ) {
                            Text(
                                text = stepsText,
                                style = MaterialTheme.typography.displayLarge,
                                modifier = Modifier
                                    .testTag("step_count")
                                    .semantics { contentDescription = stepsDescription },
                            )
                            Text(
                                stringResource(R.string.steps_label),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(Modifier.height(24.dp))
                        if (state.goalReached) {
                            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary) {
                                Text(
                                    text = stringResource(R.string.goal_reached),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier
                                        .testTag("goal_reached")
                                        .padding(horizontal = 16.dp, vertical = 6.dp),
                                )
                            }
                            Spacer(Modifier.height(12.dp))
                        }
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = stringResource(R.string.progress_text, state.percent, stepsText, goalText),
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            FilledTonalButton(
                                onClick = { showGoalDialog = true },
                                modifier = Modifier.defaultMinSize(minHeight = 52.dp),
                            ) { Text(stringResource(R.string.edit_goal)) }
                            Button(
                                onClick = onOpenHistory,
                                modifier = Modifier.defaultMinSize(minHeight = 52.dp),
                            ) { Text(stringResource(R.string.history_action)) }
                        }
                    }
                    if (showGoalDialog) {
                        GoalDialog(
                            currentGoal = state.goal,
                            onSave = onSaveGoal,
                            onDismiss = { showGoalDialog = false },
                        )
                    }
                }

                TodayUiState.NeedsPermission -> Message(
                    tag = "state_needs_permission",
                    title = stringResource(R.string.permission_rationale_title),
                    body = stringResource(R.string.permission_rationale),
                    actionLabel = stringResource(R.string.permission_continue),
                    onAction = onRequestPermission,
                )

                is TodayUiState.PermissionDenied -> Message(
                    tag = "state_permission_denied",
                    title = stringResource(R.string.permission_denied_title),
                    body = stringResource(R.string.permission_denied),
                    actionLabel = stringResource(
                        if (state.canPrompt) R.string.permission_grant else R.string.permission_open_settings,
                    ),
                    onAction = if (state.canPrompt) onRequestPermission else onOpenSettings,
                )

                TodayUiState.Unsupported -> Message(
                    tag = "state_unsupported",
                    title = stringResource(R.string.unsupported_title),
                    body = stringResource(R.string.unsupported_message),
                )
            }
        }
    }
}

/** Circular progress with a mint -> cyan sweep; the arc animates when steps change. */
@Composable
private fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 900),
        label = "ring",
    )
    val track = MaterialTheme.colorScheme.surfaceVariant
    val start = MaterialTheme.colorScheme.primary
    val end = MaterialTheme.colorScheme.secondary
    val strokeWidth = 22.dp
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize().padding(strokeWidth / 2)) {
            val stroke = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            drawArc(color = track, startAngle = 0f, sweepAngle = 360f, useCenter = false, style = stroke)
            if (animated > 0f) {
                rotate(-90f) {
                    drawArc(
                        brush = Brush.sweepGradient(listOf(end, start, end)),
                        startAngle = 0f,
                        sweepAngle = 360f * animated,
                        useCenter = false,
                        style = stroke,
                    )
                }
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, content = content)
    }
}

@Composable
private fun Message(
    tag: String,
    title: String,
    body: String,
    actionLabel: String? = null,
    onAction: () -> Unit = {},
) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth().testTag(tag),
    ) {
        Column(
            modifier = Modifier.padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
            Text(
                body,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            if (actionLabel != null) {
                Spacer(Modifier.height(4.dp))
                Button(onClick = onAction, modifier = Modifier.defaultMinSize(minHeight = 52.dp)) { Text(actionLabel) }
            }
        }
    }
}

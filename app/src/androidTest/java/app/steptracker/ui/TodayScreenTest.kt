package app.steptracker.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import app.steptracker.ui.theme.StepTrackerTheme
import app.steptracker.ui.today.TodayScreen
import app.steptracker.ui.today.TodayUiState
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class TodayScreenTest {
    @get:Rule
    val rule = createComposeRule()

    private fun show(state: TodayUiState, onRequest: () -> Unit = {}, onSettings: () -> Unit = {}) {
        rule.setContent {
            StepTrackerTheme {
                TodayScreen(
                    state = state,
                    onRequestPermission = onRequest,
                    onOpenSettings = onSettings,
                    onSaveGoal = { true },
                    onOpenHistory = {},
                )
            }
        }
    }

    @Test
    fun loading() {
        show(TodayUiState.Loading)
        rule.onNodeWithTag("state_loading").assertIsDisplayed()
    }

    @Test
    fun active_showsStepsAndNoGoalReachedBelowGoal() {
        show(TodayUiState.Active(steps = 1234, goal = 10000))
        rule.onNodeWithTag("state_active").assertIsDisplayed()
        rule.onNodeWithTag("step_count").assertIsDisplayed()
        rule.onNodeWithTag("goal_reached").assertDoesNotExist()
    }

    @Test
    fun active_showsGoalReachedTextWhenStepsReachGoal() {
        show(TodayUiState.Active(steps = 10000, goal = 10000))
        rule.onNodeWithTag("goal_reached").assertIsDisplayed()
    }

    @Test
    fun needsPermission_continueButtonRequestsPermission() {
        var requested = false
        show(TodayUiState.NeedsPermission, onRequest = { requested = true })
        rule.onNodeWithTag("state_needs_permission").assertIsDisplayed()
        rule.onNodeWithText("Continue").performClick()
        assertTrue(requested)
    }

    @Test
    fun permissionDenied_canPromptShowsGrantAndOtherwiseOpenSettings() {
        var requested = false
        show(TodayUiState.PermissionDenied(canPrompt = true), onRequest = { requested = true })
        rule.onNodeWithText("Grant permission").performClick()
        assertTrue(requested)
    }

    @Test
    fun permissionDenied_cannotPromptShowsOpenSettings() {
        var opened = false
        show(TodayUiState.PermissionDenied(canPrompt = false), onSettings = { opened = true })
        rule.onNodeWithText("Open settings").performClick()
        assertTrue(opened)
    }

    @Test
    fun unsupported_showsMessageWithoutStepValue() {
        show(TodayUiState.Unsupported)
        rule.onNodeWithTag("state_unsupported").assertIsDisplayed()
        rule.onNodeWithTag("step_count").assertDoesNotExist()
    }
}

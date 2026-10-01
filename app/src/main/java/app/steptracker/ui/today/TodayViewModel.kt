package app.steptracker.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.steptracker.data.StepRepository
import app.steptracker.domain.GoalValidator
import app.steptracker.ui.permission.PermissionStatus
import app.steptracker.ui.permission.countsSteps
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface TodayUiState {
    data object Loading : TodayUiState

    data class Active(val steps: Int, val goal: Int) : TodayUiState {
        /** Progress for display, capped at 100%. */
        val progress: Float get() = (steps.toFloat() / goal).coerceIn(0f, 1f)
        val percent: Int get() = (steps.toLong() * 100 / goal).coerceAtMost(100).toInt()
        val goalReached: Boolean get() = steps >= goal
    }

    data object NeedsPermission : TodayUiState
    data class PermissionDenied(val canPrompt: Boolean) : TodayUiState
    data object Unsupported : TodayUiState
}

enum class GoalResult { Saved, Invalid }

@OptIn(ExperimentalCoroutinesApi::class)
class TodayViewModel(private val repository: StepRepository) : ViewModel() {

    private val permission = MutableStateFlow<PermissionStatus?>(null)
    private val today = MutableStateFlow(LocalDate.now())

    private val stepsAndGoal = today
        .flatMapLatest { date -> combine(repository.observeToday(date), repository.goal) { record, goal -> (record?.steps ?: 0) to goal } }

    val uiState: StateFlow<TodayUiState> = combine(permission, stepsAndGoal) { status, (steps, goal) ->
        when {
            !repository.isSupported -> TodayUiState.Unsupported
            status == null -> TodayUiState.Loading
            status.countsSteps -> TodayUiState.Active(steps, goal)
            status is PermissionStatus.NeedsRationale -> TodayUiState.NeedsPermission
            status is PermissionStatus.Denied -> TodayUiState.PermissionDenied(status.canPrompt)
            else -> TodayUiState.Loading
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayUiState.Loading)

    /** Call on resume and after a permission result. Re-reads the sensor when counting is allowed. */
    fun onResume(status: PermissionStatus) {
        permission.value = status
        today.value = LocalDate.now()
        if (status.countsSteps) viewModelScope.launch { repository.syncNow() }
    }

    fun saveGoal(input: String): GoalResult {
        val goal = GoalValidator.parse(input) ?: return GoalResult.Invalid
        viewModelScope.launch { repository.updateGoal(goal) }
        return GoalResult.Saved
    }
}

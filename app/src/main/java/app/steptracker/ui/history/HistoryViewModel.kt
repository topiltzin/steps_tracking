package app.steptracker.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.steptracker.data.StepRepository
import app.steptracker.domain.HistoryDay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class HistoryViewModel(repository: StepRepository) : ViewModel() {
    val days: StateFlow<List<HistoryDay>> = repository.observeHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

package idont.trust.atrust.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import idont.trust.atrust.data.ProfileRepository
import idont.trust.atrust.model.ConnectionProfile
import idont.trust.atrust.service.ConnectionRuntime
import idont.trust.atrust.service.ConnectionState
import idont.trust.atrust.service.AppLog
import idont.trust.atrust.service.LogEntry
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ProfileRepository(application)

    val profile: StateFlow<ConnectionProfile> = repository.profile.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        ConnectionProfile(),
    )
    val connectionState: StateFlow<ConnectionState> = ConnectionRuntime.state
    val logs: StateFlow<List<LogEntry>> = AppLog.entries

    fun save(profile: ConnectionProfile) {
        viewModelScope.launch { repository.save(profile) }
    }

    fun clearLogs() = AppLog.clear()
}

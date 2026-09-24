package idont.trust.atrust.service

import idont.trust.atrust.model.ConnectionMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface ConnectionState {
    data object Disconnected : ConnectionState
    data class Connecting(val mode: ConnectionMode) : ConnectionState
    data class Connected(val mode: ConnectionMode, val endpoint: String) : ConnectionState
    data object Disconnecting : ConnectionState
    data class Failed(val message: String) : ConnectionState
}

object ConnectionRuntime {
    private val mutableState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val state = mutableState.asStateFlow()

    fun update(state: ConnectionState) {
        mutableState.value = state
    }
}

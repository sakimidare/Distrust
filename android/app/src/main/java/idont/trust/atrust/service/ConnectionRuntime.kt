package idont.trust.atrust.service

import idont.trust.atrust.model.ConnectionProfile
import idont.trust.atrust.model.ConnectionMode
import idont.trust.atrust.logging.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicLong

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

    @Volatile
    private var activeProfile: ConnectionProfile? = null
	private val generation = AtomicLong()

    fun update(state: ConnectionState) {
        Logger.i("ConnectionState", "${mutableState.value} -> $state")
        mutableState.value = state
		if (state is ConnectionState.Disconnected || state is ConnectionState.Failed) activeProfile = null
    }

	fun markActive(profile: ConnectionProfile) {
		activeProfile = profile.connectionConfiguration()
	}

	fun configurationChanged(profile: ConnectionProfile): Boolean =
		activeProfile?.let { it != profile.connectionConfiguration() } ?: false

	fun nextGeneration(): Long = generation.incrementAndGet()
	fun invalidateGeneration(): Long = generation.incrementAndGet()
	fun isCurrent(candidate: Long): Boolean = generation.get() == candidate

	private fun ConnectionProfile.connectionConfiguration(): ConnectionProfile = copy(
		name = "",
		clientData = "",
	)
}

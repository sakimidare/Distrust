package idont.trust.atrust.service

import idont.trust.atrust.logging.Logger
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

sealed interface SessionEvent {
    data class Expired(val reason: String) : SessionEvent
    data class ClientDataUpdated(val clientData: String) : SessionEvent
}

/** Process-wide bridge for structured session lifecycle events emitted by DistrustCore. */
object SessionRuntime {
    private val mutableEvents = MutableSharedFlow<SessionEvent>(
        replay = 0,
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val events = mutableEvents.asSharedFlow()

    fun reportExpired(reason: String) {
        val message = reason.ifBlank { "aTrust 会话已失效，需要重新认证" }
        Logger.e("Session", "aTrust session expired; reason=$message")
        ConnectionRuntime.update(ConnectionState.Failed("aTrust 会话已过期，请重新完成认证"))
        mutableEvents.tryEmit(SessionEvent.Expired(message))
    }

    fun reportClientDataUpdated(clientData: String) {
        if (clientData.isBlank()) return
        Logger.d("Session", "Received refreshed encrypted session data")
        mutableEvents.tryEmit(SessionEvent.ClientDataUpdated(clientData))
    }
}

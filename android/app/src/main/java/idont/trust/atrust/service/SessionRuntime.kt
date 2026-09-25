package idont.trust.atrust.service

import idont.trust.atrust.logging.Logger
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Instant

sealed interface SessionEvent {
    data class Expired(val reason: String) : SessionEvent
    data class ClientDataUpdated(val clientData: String) : SessionEvent
}

data class SessionHealth(
    val lastCheck: Instant? = null,
    val lastSuccess: Instant? = null,
    val latencyMillis: Long? = null,
    val consecutiveFailures: Int = 0,
    val detail: String = "等待首次探测",
)

/** Process-wide bridge for structured session lifecycle events emitted by DistrustCore. */
object SessionRuntime {
    private val mutableEvents = MutableSharedFlow<SessionEvent>(
        replay = 0,
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val events = mutableEvents.asSharedFlow()
    private val mutableHealth = MutableStateFlow(SessionHealth())
    val health = mutableHealth.asStateFlow()

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

    fun reportHealth(success: Boolean, latencyMillis: Long, detail: String) {
        val now = Instant.now()
        mutableHealth.value = if (success) {
            SessionHealth(now, now, latencyMillis, 0, detail)
        } else {
            mutableHealth.value.copy(
                lastCheck = now,
                latencyMillis = latencyMillis,
                consecutiveFailures = mutableHealth.value.consecutiveFailures + 1,
                detail = detail,
            )
        }
        Logger.d("SessionHealth", "success=$success latencyMs=$latencyMillis failures=${mutableHealth.value.consecutiveFailures} detail=$detail")
    }
}

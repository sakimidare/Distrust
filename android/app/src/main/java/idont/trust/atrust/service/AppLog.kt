package idont.trust.atrust.service

import java.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class LogLevel { INFO, WARNING, ERROR }

data class LogEntry(
    val timestamp: Instant,
    val level: LogLevel,
    val message: String,
)

object AppLog {
    private const val MAX_ENTRIES = 500
    private val mutableEntries = MutableStateFlow<List<LogEntry>>(emptyList())
    val entries = mutableEntries.asStateFlow()

    fun info(message: String) = append(LogLevel.INFO, message)
    fun warning(message: String) = append(LogLevel.WARNING, message)
    fun error(message: String) = append(LogLevel.ERROR, message)

    fun clear() {
        mutableEntries.value = emptyList()
    }

    private fun append(level: LogLevel, message: String) {
        val sanitized = message
            .replace(Regex("(?i)(password|token|sid)=([^&\\s]+)"), "$1=***")
            .take(2_000)
        mutableEntries.update { current ->
            (current + LogEntry(Instant.now(), level, sanitized)).takeLast(MAX_ENTRIES)
        }
    }
}

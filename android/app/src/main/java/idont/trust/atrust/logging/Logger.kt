package idont.trust.atrust.logging

import android.util.Log
import java.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class LogLevel(val priority: Int) {
    VERBOSE(Log.VERBOSE),
    DEBUG(Log.DEBUG),
    INFO(Log.INFO),
    WARNING(Log.WARN),
    ERROR(Log.ERROR),
    ASSERT(Log.ASSERT),
}

data class LogEntry(
    val timestamp: Instant,
    val level: LogLevel,
    val tag: String,
    val message: String,
    val throwable: String? = null,
)

/** A process-wide, Logcat-compatible logger also observable by Compose UI. */
object Logger {
    private const val ROOT_TAG = "Distrust"
    private const val MAX_ENTRIES = 1_000
    private const val MAX_UI_MESSAGE = 4_000
    private const val MAX_UI_THROWABLE = 12_000
    private const val LOGCAT_CHUNK = 3_500

    private val mutableEntries = MutableStateFlow<List<LogEntry>>(emptyList())
    val entries = mutableEntries.asStateFlow()

    fun v(tag: String, message: String, throwable: Throwable? = null) = log(LogLevel.VERBOSE, tag, message, throwable)
    fun d(tag: String, message: String, throwable: Throwable? = null) = log(LogLevel.DEBUG, tag, message, throwable)
    fun i(tag: String, message: String, throwable: Throwable? = null) = log(LogLevel.INFO, tag, message, throwable)
    fun w(tag: String, message: String, throwable: Throwable? = null) = log(LogLevel.WARNING, tag, message, throwable)
    fun e(tag: String, message: String, throwable: Throwable? = null) = log(LogLevel.ERROR, tag, message, throwable)
    fun wtf(tag: String, message: String, throwable: Throwable? = null) = log(LogLevel.ASSERT, tag, message, throwable)

    fun clear() {
        mutableEntries.value = emptyList()
        i("Logger", "UI log buffer cleared")
    }

    private fun log(level: LogLevel, tag: String, message: String, throwable: Throwable?) {
        val safeTag = tag.ifBlank { "General" }.take(48)
        val safeMessage = redact(message).take(MAX_UI_MESSAGE)
        val safeThrowable = throwable?.let { redact(Log.getStackTraceString(it)).take(MAX_UI_THROWABLE) }
        val logcatTag = "$ROOT_TAG/$safeTag"

        if (throwable != null) {
            when (level) {
                LogLevel.VERBOSE -> Log.v(logcatTag, safeMessage, throwable)
                LogLevel.DEBUG -> Log.d(logcatTag, safeMessage, throwable)
                LogLevel.INFO -> Log.i(logcatTag, safeMessage, throwable)
                LogLevel.WARNING -> Log.w(logcatTag, safeMessage, throwable)
                LogLevel.ERROR -> Log.e(logcatTag, safeMessage, throwable)
                LogLevel.ASSERT -> Log.wtf(logcatTag, safeMessage, throwable)
            }
        } else {
            safeMessage.chunked(LOGCAT_CHUNK).ifEmpty { listOf("") }.forEach {
                Log.println(level.priority, logcatTag, it)
            }
        }

        mutableEntries.update { current ->
            (current + LogEntry(Instant.now(), level, safeTag, safeMessage, safeThrowable))
                .takeLast(MAX_ENTRIES)
        }
    }

    private fun redact(value: String): String {
        var result = value
        SENSITIVE_PATTERNS.forEach { pattern ->
            result = pattern.replace(result) { match ->
                val prefix = match.groups[1]?.value.orEmpty()
                "$prefix***"
            }
        }
        return result
    }

    private val SENSITIVE_PATTERNS = listOf(
        Regex("(?i)(password\\s*[=:]\\s*|\\\"password\\\"\\s*:\\s*\\\")[^,}&\\s\\\"]+"),
        Regex("(?i)(totpSecret\\s*[=:]\\s*|\\\"totpSecret\\\"\\s*:\\s*\\\")[^,}&\\s\\\"]+"),
        Regex("(?i)((?:access[_-]?token|refresh[_-]?token|token|sid|ticket)\\s*[=:]\\s*)[^,}&\\s]+"),
        Regex("(?i)((?:code|ticket|token|sid)=)[^&#\\s]+"),
        Regex("(?i)(authorization:\\s*(?:bearer|basic)\\s+)[^\\s]+"),
        Regex("(?i)(cookie:\\s*)[^\\r\\n]+"),
    )
}

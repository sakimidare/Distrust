package idont.trust.atrust.service

import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object AuthRuntime {
    private val lock = Any()
    private val mutableChallenge = MutableStateFlow<String?>(null)
    private var response: CompletableFuture<String>? = null
    val challenge = mutableChallenge.asStateFlow()

    fun request(challengeJson: String): String {
        val pending = CompletableFuture<String>()
        synchronized(lock) {
            response?.complete("")
            response = pending
            mutableChallenge.value = challengeJson
        }
        return runCatching { pending.get(10, TimeUnit.MINUTES) }.getOrDefault("").also {
            synchronized(lock) {
                response = null
                mutableChallenge.value = null
            }
        }
    }

    fun respond(responseJson: String) {
        synchronized(lock) { response?.complete(responseJson) }
    }

    fun cancel() = respond("")
}

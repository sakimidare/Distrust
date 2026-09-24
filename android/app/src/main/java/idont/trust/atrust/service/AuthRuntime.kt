package idont.trust.atrust.service

import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import idont.trust.atrust.logging.Logger
import org.json.JSONObject

object AuthRuntime {
    private val lock = Any()
    private val mutableChallenge = MutableStateFlow<String?>(null)
    private var response: CompletableFuture<String>? = null
    val challenge = mutableChallenge.asStateFlow()

    fun request(challengeJson: String): String {
        val type = runCatching { JSONObject(challengeJson).optString("type") }.getOrDefault("unknown")
        Logger.i("Auth", "Authentication challenge received; type=$type")
        val pending = CompletableFuture<String>()
        synchronized(lock) {
            response?.complete("")
            response = pending
            mutableChallenge.value = challengeJson
        }
        return runCatching { pending.get(10, TimeUnit.MINUTES) }
            .onSuccess { Logger.i("Auth", "Authentication challenge completed; type=$type, cancelled=${it.isEmpty()}") }
            .onFailure { Logger.e("Auth", "Authentication challenge timed out or failed; type=$type", it) }
            .getOrDefault("").also {
            synchronized(lock) {
                response = null
                mutableChallenge.value = null
            }
        }
    }

    fun respond(responseJson: String) {
        Logger.d("Auth", "Submitting authentication response; empty=${responseJson.isEmpty()}")
        synchronized(lock) { response?.complete(responseJson) }
    }

    fun cancel() = respond("")
}

package idont.trust.atrust.data

import android.content.Context
import idont.trust.atrust.logging.Logger

object DnsHistoryStore {
    private const val MAX_ADDRESSES_PER_HOST = 4
    private const val MAX_AGE_MS = 30L * 24 * 60 * 60 * 1_000
    private lateinit var context: Context

    fun initialize(context: Context) {
        this.context = context.applicationContext
    }

    fun lookup(namespace: String, host: String): List<String> {
        if (!::context.isInitialized) return emptyList()
        val now = System.currentTimeMillis()
        return preferences().getString(key(namespace, host), null)
            ?.split(',')
            ?.mapNotNull { item ->
                val parts = item.split('|', limit = 2)
                val timestamp = parts.getOrNull(1)?.toLongOrNull() ?: return@mapNotNull null
                parts.firstOrNull()?.takeIf { now - timestamp <= MAX_AGE_MS }
            }
            .orEmpty()
    }

    fun record(namespace: String, host: String, address: String) {
        if (!::context.isInitialized || host.isBlank() || address.isBlank()) return
        val now = System.currentTimeMillis()
        val updated = (listOf(address) + lookup(namespace, host))
            .distinct()
            .take(MAX_ADDRESSES_PER_HOST)
            .joinToString(",") { "$it|$now" }
        preferences().edit().putString(key(namespace, host), updated).apply()
        Logger.i("DNSHistory", "Recorded successful mapping namespace=$namespace host=$host address=$address")
    }

    private fun preferences() = context.getSharedPreferences("dns_history", Context.MODE_PRIVATE)
    private fun key(namespace: String, host: String) = "${namespace.lowercase()}|${host.lowercase()}"
}

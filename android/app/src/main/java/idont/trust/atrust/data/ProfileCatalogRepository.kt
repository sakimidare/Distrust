package idont.trust.atrust.data

import android.content.Context
import idont.trust.atrust.logging.Logger
import idont.trust.atrust.model.ConnectionProfile
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
private data class ProfileCatalog(val profiles: List<ConnectionProfile>)

class ProfileCatalogRepository(context: Context) {
    private val secrets = SecretStore(context)
    private val json = Json { ignoreUnknownKeys = true }
    private val lock = Any()

    fun load(seed: ConnectionProfile): List<ConnectionProfile> = synchronized(lock) {
        val stored = secrets.readProfileCatalog()
        val profiles = runCatching { json.decodeFromString<ProfileCatalog>(stored).profiles }
            .getOrDefault(emptyList())
        if (profiles.isEmpty()) {
            listOf(seed).also(::writeLocked)
        } else {
            profiles
        }
    }

    fun upsert(profile: ConnectionProfile, seed: ConnectionProfile = profile): List<ConnectionProfile> = synchronized(lock) {
        val current = loadLocked(seed).toMutableList()
        val index = current.indexOfFirst { it.id == profile.id }
        if (index >= 0) current[index] = profile else current += profile
        current.sortedBy { it.name.lowercase() }.also(::writeLocked)
    }

    fun remove(id: String, seed: ConnectionProfile): List<ConnectionProfile> = synchronized(lock) {
        val current = loadLocked(seed)
        val updated = current.filterNot { it.id == id }.ifEmpty { listOf(seed.copy(id = "default")) }
        updated.also(::writeLocked)
    }

    private fun loadLocked(seed: ConnectionProfile): List<ConnectionProfile> {
        val stored = secrets.readProfileCatalog()
        return runCatching { json.decodeFromString<ProfileCatalog>(stored).profiles }
            .getOrDefault(emptyList()).ifEmpty { listOf(seed) }
    }

    private fun writeLocked(profiles: List<ConnectionProfile>) {
        secrets.writeProfileCatalog(json.encodeToString(ProfileCatalog(profiles)))
        Logger.d("ProfileCatalog", "Encrypted profile catalog updated; count=${profiles.size}")
    }
}

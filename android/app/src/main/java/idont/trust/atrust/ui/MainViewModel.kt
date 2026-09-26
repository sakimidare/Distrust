package idont.trust.atrust.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import idont.trust.atrust.data.ProfileRepository
import idont.trust.atrust.data.ProfileCatalogRepository
import idont.trust.atrust.core.AuthMethod
import idont.trust.atrust.core.GoMobileCoreBridge
import idont.trust.atrust.model.ConnectionProfile
import idont.trust.atrust.model.ServerScheme
import idont.trust.atrust.service.ConnectionRuntime
import idont.trust.atrust.service.ConnectionState
import idont.trust.atrust.logging.Logger
import idont.trust.atrust.service.AuthRuntime
import idont.trust.atrust.logging.LogEntry
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import java.util.UUID

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ProfileRepository(application)
    private val catalog = ProfileCatalogRepository(application)
    private val core = GoMobileCoreBridge()
    private val mutableAuthDiscovery = MutableStateFlow<AuthDiscoveryState>(AuthDiscoveryState.Idle)
    private val mutableProfiles = MutableStateFlow<List<ConnectionProfile>>(emptyList())

    val profile: StateFlow<ConnectionProfile> = repository.profile.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        ConnectionProfile(),
    )
    val connectionState: StateFlow<ConnectionState> = ConnectionRuntime.state
    val logs: StateFlow<List<LogEntry>> = Logger.entries
    val authChallenge: StateFlow<String?> = AuthRuntime.challenge
    val authDiscovery = mutableAuthDiscovery.asStateFlow()
    val profiles = mutableProfiles.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val current = repository.profile.first()
            mutableProfiles.value = catalog.load(current)
        }
    }

    fun save(profile: ConnectionProfile) {
        Logger.i("Profile", "Saving profile; mode=${profile.mode}, protocol=${profile.protocol}, server=${profile.server}:${profile.port}")
        viewModelScope.launch {
            runCatching { repository.save(profile) }
                .onSuccess {
                    mutableProfiles.value = catalog.upsert(profile)
                    Logger.d("Profile", "Profile saved")
                }
                .onFailure { Logger.e("Profile", "Failed to save profile", it) }
        }
    }

    fun clearLogs() = Logger.clear()
    fun clearSession() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearClientData()
        }
    }

    fun switchProfile(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = repository.profile.first()
            val latestCatalog = catalog.upsert(current)
            val target = latestCatalog.firstOrNull { it.id == id } ?: return@launch
            repository.save(target)
            mutableProfiles.value = latestCatalog
            Logger.i("Profile", "Switched active profile to '${target.name}'")
        }
    }

    fun duplicateProfile() {
        viewModelScope.launch(Dispatchers.IO) {
            val current = repository.profile.first()
            val existingNames = catalog.load(current).mapTo(mutableSetOf()) { it.name }
            val baseName = current.name.replace(Regex(" 副本(?: \\d+)?$"), "")
            var copyName = "$baseName 副本"
            var suffix = 2
            while (copyName in existingNames) copyName = "$baseName 副本 ${suffix++}"
            val copy = current.copy(
                id = UUID.randomUUID().toString(),
                name = copyName,
                clientData = "",
            )
            repository.save(copy)
            mutableProfiles.value = catalog.upsert(copy, current)
            Logger.i("Profile", "Created profile '${copy.name}'")
        }
    }

    fun createProfile(name: String) {
        val normalized = name.trim()
        if (normalized.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            val current = repository.profile.first()
            val created = ConnectionProfile(
                id = UUID.randomUUID().toString(),
                name = normalized,
                server = "vpn.seu.edu.cn",
                port = 443,
            )
            repository.save(created)
            mutableProfiles.value = catalog.upsert(created, current)
            Logger.i("Profile", "Created blank profile '$normalized'")
        }
    }

    fun renameProfile(id: String, name: String) {
        val normalized = name.trim()
        if (normalized.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            val current = repository.profile.first()
            val target = catalog.load(current).firstOrNull { it.id == id } ?: return@launch
            val renamed = target.copy(name = normalized)
            if (current.id == id) repository.save(renamed)
            mutableProfiles.value = catalog.upsert(renamed, current)
            Logger.i("Profile", "Renamed profile to '$normalized'")
        }
    }

    fun deleteProfile(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = repository.profile.first()
            val remaining = catalog.remove(id, current)
            val next = remaining.firstOrNull { it.id != id } ?: remaining.first()
            repository.save(next)
            mutableProfiles.value = remaining
            Logger.i("Profile", "Deleted profile id=$id")
        }
    }
    fun fakeDnsSnapshot(): Result<Map<String, String>> = core.fakeDnsSnapshot()
    fun clearFakeDns(): Result<Unit> = core.clearFakeDns()
    fun submitAuth(responseJson: String) = AuthRuntime.respond(responseJson)
    fun cancelAuth() {
        Logger.w("Auth", "Authentication challenge cancelled by user")
        AuthRuntime.cancel()
    }

    fun fetchAuthMethods(server: String, port: Int, scheme: ServerScheme = ServerScheme.HTTPS) {
        if (mutableAuthDiscovery.value is AuthDiscoveryState.Loading) return
        Logger.i("AuthDiscovery", "Fetching authentication methods from $server:$port")
        viewModelScope.launch {
            mutableAuthDiscovery.value = AuthDiscoveryState.Loading
            val result = withContext(Dispatchers.IO) { core.fetchAuthMethods(server, port, scheme) }
            mutableAuthDiscovery.value = result.fold(
                onSuccess = {
                    Logger.i("AuthDiscovery", "Server returned ${it.size} authentication methods")
                    AuthDiscoveryState.Success(it)
                },
                onFailure = {
                    Logger.e("AuthDiscovery", "Failed to fetch authentication methods", it)
                    AuthDiscoveryState.Error(it.cause?.message ?: it.message ?: "获取认证方式失败")
                },
            )
        }
    }

    fun resetAuthDiscovery() {
        mutableAuthDiscovery.value = AuthDiscoveryState.Idle
    }
}

sealed interface AuthDiscoveryState {
    data object Idle : AuthDiscoveryState
    data object Loading : AuthDiscoveryState
    data class Success(val methods: List<AuthMethod>) : AuthDiscoveryState
    data class Error(val message: String) : AuthDiscoveryState
}

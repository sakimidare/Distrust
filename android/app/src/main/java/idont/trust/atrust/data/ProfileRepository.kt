package idont.trust.atrust.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import idont.trust.atrust.model.ConnectionMode
import idont.trust.atrust.model.ConnectionProfile
import idont.trust.atrust.model.VpnProtocol
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import idont.trust.atrust.logging.Logger

private val Context.profileDataStore by preferencesDataStore(name = "profile")

class ProfileRepository(private val context: Context) {
    private val secrets = SecretStore(context)

    val profile: Flow<ConnectionProfile> = context.profileDataStore.data.map(::toProfile)

    suspend fun save(profile: ConnectionProfile) {
        Logger.d("ProfileRepository", "Persisting profile '${profile.name}'")
        secrets.writePassword(profile.password)
        secrets.writeClientData(profile.clientData)
        secrets.writeTotpSecret(profile.totpSecret)
        context.profileDataStore.edit { values ->
            values[Keys.NAME] = profile.name
            values[Keys.MODE] = profile.mode.name
            values[Keys.PROTOCOL] = profile.protocol.name
            values[Keys.SERVER] = profile.server.trim()
            values[Keys.PORT] = profile.port
            values[Keys.USERNAME] = profile.username.trim()
            values[Keys.LOGIN_DOMAIN] = profile.loginDomain.trim()
            values[Keys.AUTH_TYPE] = profile.authType.trim()
            values[Keys.LOGIN_URL] = profile.loginUrl.trim()
            values[Keys.PHONE] = profile.phone.trim()
            values[Keys.SOCKS_PORT] = profile.socksPort
            values[Keys.HTTP_PORT] = profile.httpPort
            values[Keys.ROUTES] = profile.routes.joinToString("\n")
            values[Keys.DNS] = profile.dnsServers.joinToString("\n")
            values[Keys.DNS_TTL] = profile.dnsTtl
            values[Keys.PROXY_ALL] = profile.proxyAll
            values[Keys.DISABLE_SERVER_CONFIG] = profile.disableServerConfig
            values[Keys.UPDATE_BEST_NODES] = profile.updateBestNodesInterval
            values[Keys.SESSION_REFRESH] = profile.sessionRefreshInterval
        }
    }

    fun updateClientData(clientData: String) {
        Logger.d("ProfileRepository", "Updating encrypted aTrust client session; present=${clientData.isNotEmpty()}")
        secrets.writeClientData(clientData)
    }

    private fun toProfile(values: Preferences): ConnectionProfile = ConnectionProfile(
        name = values[Keys.NAME] ?: "默认配置",
        mode = values[Keys.MODE].enumOrDefault(ConnectionMode.LOCAL_PROXY),
        protocol = values[Keys.PROTOCOL].enumOrDefault(VpnProtocol.ATRUST),
        server = values[Keys.SERVER] ?: "trust.hitsz.edu.cn",
        port = values[Keys.PORT] ?: 443,
        username = values[Keys.USERNAME] ?: "",
        password = secrets.readPassword(),
        totpSecret = secrets.readTotpSecret(),
        loginDomain = values[Keys.LOGIN_DOMAIN] ?: "hitcas",
        authType = values[Keys.AUTH_TYPE] ?: "psw",
        loginUrl = values[Keys.LOGIN_URL] ?: "",
        phone = values[Keys.PHONE] ?: "",
        socksPort = values[Keys.SOCKS_PORT] ?: 11080,
        httpPort = values[Keys.HTTP_PORT] ?: 11081,
        routes = values[Keys.ROUTES].linesOrDefault(listOf("10.0.0.0/8")),
        dnsServers = values[Keys.DNS].linesOrDefault(listOf("10.10.0.21")),
        dnsTtl = values[Keys.DNS_TTL] ?: 3600,
        proxyAll = values[Keys.PROXY_ALL] ?: false,
        disableServerConfig = values[Keys.DISABLE_SERVER_CONFIG] ?: false,
        updateBestNodesInterval = values[Keys.UPDATE_BEST_NODES] ?: 300,
        sessionRefreshInterval = values[Keys.SESSION_REFRESH] ?: 1800,
        clientData = secrets.readClientData(),
    )

    private inline fun <reified T : Enum<T>> String?.enumOrDefault(default: T): T =
        runCatching { enumValueOf<T>(this.orEmpty()) }.getOrDefault(default)

    private fun String?.linesOrDefault(default: List<String>): List<String> =
        this?.lineSequence()?.map(String::trim)?.filter(String::isNotEmpty)?.toList()
            ?.takeIf { it.isNotEmpty() } ?: default

    private object Keys {
        val NAME = stringPreferencesKey("name")
        val MODE = stringPreferencesKey("mode")
        val PROTOCOL = stringPreferencesKey("protocol")
        val SERVER = stringPreferencesKey("server")
        val PORT = intPreferencesKey("port")
        val USERNAME = stringPreferencesKey("username")
        val LOGIN_DOMAIN = stringPreferencesKey("login_domain")
        val AUTH_TYPE = stringPreferencesKey("auth_type")
        val LOGIN_URL = stringPreferencesKey("login_url")
        val PHONE = stringPreferencesKey("phone")
        val SOCKS_PORT = intPreferencesKey("socks_port")
        val HTTP_PORT = intPreferencesKey("http_port")
        val ROUTES = stringPreferencesKey("routes")
        val DNS = stringPreferencesKey("dns")
        val DNS_TTL = intPreferencesKey("dns_ttl")
        val PROXY_ALL = booleanPreferencesKey("proxy_all")
        val DISABLE_SERVER_CONFIG = booleanPreferencesKey("disable_server_config")
        val UPDATE_BEST_NODES = intPreferencesKey("update_best_nodes_interval")
        val SESSION_REFRESH = intPreferencesKey("session_refresh_interval")
    }
}

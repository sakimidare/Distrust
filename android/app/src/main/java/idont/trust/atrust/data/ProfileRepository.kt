package idont.trust.atrust.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import idont.trust.atrust.model.ConnectionMode
import idont.trust.atrust.model.ConnectionProfile
import idont.trust.atrust.model.VpnProtocol
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.profileDataStore by preferencesDataStore(name = "profile")

class ProfileRepository(private val context: Context) {
    private val secrets = SecretStore(context)

    val profile: Flow<ConnectionProfile> = context.profileDataStore.data.map(::toProfile)

    suspend fun save(profile: ConnectionProfile) {
        secrets.writePassword(profile.password)
        secrets.writeClientData(profile.clientData)
        context.profileDataStore.edit { values ->
            values[Keys.NAME] = profile.name
            values[Keys.MODE] = profile.mode.name
            values[Keys.PROTOCOL] = profile.protocol.name
            values[Keys.SERVER] = profile.server.trim()
            values[Keys.PORT] = profile.port
            values[Keys.USERNAME] = profile.username.trim()
            values[Keys.LOGIN_DOMAIN] = profile.loginDomain.trim()
            values[Keys.AUTH_TYPE] = profile.authType.trim()
            values[Keys.SOCKS_PORT] = profile.socksPort
            values[Keys.HTTP_PORT] = profile.httpPort
            values[Keys.ROUTES] = profile.routes.joinToString("\n")
            values[Keys.DNS] = profile.dnsServers.joinToString("\n")
        }
    }

    fun updateClientData(clientData: String) {
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
        loginDomain = values[Keys.LOGIN_DOMAIN] ?: "hitcas",
        authType = values[Keys.AUTH_TYPE] ?: "psw",
        socksPort = values[Keys.SOCKS_PORT] ?: 11080,
        httpPort = values[Keys.HTTP_PORT] ?: 11081,
        routes = values[Keys.ROUTES].linesOrDefault(listOf("10.0.0.0/8")),
        dnsServers = values[Keys.DNS].linesOrDefault(listOf("10.10.0.21")),
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
        val SOCKS_PORT = intPreferencesKey("socks_port")
        val HTTP_PORT = intPreferencesKey("http_port")
        val ROUTES = stringPreferencesKey("routes")
        val DNS = stringPreferencesKey("dns")
    }
}

package idont.trust.atrust.core

import idont.trust.atrust.model.ConnectionProfile

data class CoreCapabilities(
    val easyConnectVpn: Boolean,
    val aTrustVpn: Boolean,
    val localSocks5: Boolean,
    val localHttp: Boolean,
)

data class AuthMethod(
    val name: String,
    val type: String,
    val loginDomain: String,
    val loginUrl: String,
)

data class NegotiatedTunnel(
    val address: String,
    val prefixLength: Int = 8,
    val mtu: Int = 1400,
    val routes: List<String> = emptyList(),
    val dnsServers: List<String> = emptyList(),
    val clientData: String = "",
)

data class ProxySession(
    val socksAddress: String,
    val httpAddress: String,
    val clientData: String = "",
)

interface CoreBridge {
    val capabilities: CoreCapabilities

    fun fetchAuthMethods(server: String, port: Int): Result<List<AuthMethod>>
    fun login(profile: ConnectionProfile, onChallenge: (String) -> String): Result<NegotiatedTunnel>
    fun runTun(fileDescriptor: Int): Result<Unit>
    fun startLocalProxy(profile: ConnectionProfile, onChallenge: (String) -> String): Result<ProxySession>
    fun stop()
}

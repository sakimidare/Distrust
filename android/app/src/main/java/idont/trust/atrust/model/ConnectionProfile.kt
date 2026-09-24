package idont.trust.atrust.model

enum class ConnectionMode {
    VPN,
    LOCAL_PROXY,
}

enum class VpnProtocol {
    ATRUST,
    EASYCONNECT,
}

data class ConnectionProfile(
    val name: String = "默认配置",
    val mode: ConnectionMode = ConnectionMode.LOCAL_PROXY,
    val protocol: VpnProtocol = VpnProtocol.ATRUST,
    val server: String = "trust.hitsz.edu.cn",
    val port: Int = 443,
    val username: String = "",
    val password: String = "",
    val totpSecret: String = "",
    val loginDomain: String = "hitcas",
    val authType: String = "psw",
    val loginUrl: String = "",
    val phone: String = "",
    val socksPort: Int = 11080,
    val httpPort: Int = 11081,
    val routes: List<String> = listOf("10.0.0.0/8"),
    val dnsServers: List<String> = listOf("10.10.0.21"),
    val dnsTtl: Int = 3600,
    val proxyAll: Boolean = false,
    val disableServerConfig: Boolean = false,
    val updateBestNodesInterval: Int = 300,
    val sessionRefreshInterval: Int = 1800,
    val clientData: String = "",
)

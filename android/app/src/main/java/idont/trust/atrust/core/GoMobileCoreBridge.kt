package idont.trust.atrust.core

import idont.trust.atrust.model.ConnectionProfile
import idont.trust.atrust.model.VpnProtocol
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import org.json.JSONObject
import idont.trust.atrust.logging.Logger

/**
 * Compatibility bridge for the current upstream gomobile AAR.
 *
 * Reflection keeps the Android UI buildable when no locally built AAR is
 * present. The intentionally narrow capability report prevents the legacy
 * EasyConnect-only API from being presented as aTrust or proxy support.
 */
class GoMobileCoreBridge : CoreBridge {
    private val mobileClass = runCatching { Class.forName("mobile.Mobile") }.getOrNull()
    private val login: Method? = mobileClass.method("login", String::class.java, String::class.java, String::class.java)
    private val prepare: Method? = mobileClass.method("prepare", String::class.java)
    private val startProxy: Method? = mobileClass.method("startProxy", String::class.java)
    private val prepareWithCallback: Method? = mobileClass.methodNamed("prepareWithCallback", 2)
    private val startProxyWithCallback: Method? = mobileClass.methodNamed("startProxyWithCallback", 2)
    private val fetchAuthMethods: Method? = mobileClass.methodNamed("fetchAuthMethods", 2)
    private val startStack: Method? = mobileClass.method("startStack", Long::class.javaPrimitiveType!!)
        ?: mobileClass.method("startStack", Int::class.javaPrimitiveType!!)
    private val logout: Method? = mobileClass.method("logout")

    override val capabilities = CoreCapabilities(
        easyConnectVpn = (prepare != null || login != null) && startStack != null,
        aTrustVpn = prepare != null && startStack != null,
        localSocks5 = startProxy != null,
        localHttp = startProxy != null,
    )

    init {
        Logger.i(
            "GoCore",
            "Core bridge initialized; loaded=${mobileClass != null}, easyConnect=${capabilities.easyConnectVpn}, aTrust=${capabilities.aTrustVpn}, proxy=${capabilities.localSocks5}",
        )
    }

    override fun fetchAuthMethods(server: String, port: Int): Result<List<AuthMethod>> = runCatching {
        Logger.d("GoCore", "Invoking FetchAuthMethods; server=$server:$port")
        val method = checkNotNull(fetchAuthMethods) { "当前核心不支持获取服务器认证方式" }
        val result = JSONObject(method.invoke(null, server, port.toLong())?.toString().orEmpty())
        check(result.optBoolean("ok")) { result.optString("errorMessage", "获取认证方式失败") }
        val methods = result.optJSONArray("authMethods") ?: return@runCatching emptyList()
        buildList {
            for (index in 0 until methods.length()) {
                val item = methods.optJSONObject(index) ?: continue
                add(
                    AuthMethod(
                        name = item.optString("authName"),
                        type = item.optString("authType"),
                        loginDomain = item.optString("loginDomain"),
                        loginUrl = item.optString("loginUrl"),
                    ),
                )
            }
        }
            .also { Logger.d("GoCore", "Decoded ${it.size} authentication methods") }
    }

    override fun login(profile: ConnectionProfile, onChallenge: (String) -> String): Result<NegotiatedTunnel> = runCatching {
        Logger.i("GoCore", "Preparing tunnel; protocol=${profile.protocol}")
        if (prepareWithCallback != null || prepare != null) {
            val result = invokeJson(prepareWithCallback ?: checkNotNull(prepare), profile, onChallenge)
            return@runCatching NegotiatedTunnel(
                address = result.getString("address"),
                prefixLength = result.optInt("prefixLength", 32),
                mtu = result.optInt("mtu", 1400),
                routes = result.stringList("routes"),
                dnsServers = result.stringList("dnsServers"),
                clientData = result.optString("clientData"),
            )
        }
        require(profile.protocol == VpnProtocol.EASYCONNECT) { "当前 Go AAR 尚未导出 aTrust 移动接口" }
        val method = checkNotNull(login) { "未安装 zju-connect Android AAR" }
        val address = method.invoke(null, "${profile.server}:${profile.port}", profile.username, profile.password)
            ?.toString().orEmpty()
        check(address.isNotBlank()) { "核心登录失败，请检查服务器和凭据" }
        NegotiatedTunnel(address = address)
    }

    override fun runTun(fileDescriptor: Int): Result<Unit> = runCatching {
        Logger.i("GoCore", "Starting TUN stack; fd=$fileDescriptor")
        val method = checkNotNull(startStack) { "当前核心不支持 Android TUN" }
        val parameter = method.parameterTypes.singleOrNull()
        if (parameter == java.lang.Long.TYPE) {
            method.invoke(null, fileDescriptor.toLong())
        } else {
            method.invoke(null, fileDescriptor)
        }
    }

    override fun startLocalProxy(profile: ConnectionProfile, onChallenge: (String) -> String): Result<ProxySession> = runCatching {
        Logger.i("GoCore", "Starting local proxy; protocol=${profile.protocol}, socks=127.0.0.1:${profile.socksPort}, http=127.0.0.1:${profile.httpPort}")
        val result = invokeJson(
            startProxyWithCallback ?: checkNotNull(startProxy) { "当前核心尚未导出 SOCKS5/HTTP 代理服务" },
            profile,
            onChallenge,
        )
        ProxySession(
            socksAddress = result.optString("socksAddress"),
            httpAddress = result.optString("httpAddress"),
            clientData = result.optString("clientData"),
        )
    }

    override fun stop() {
        Logger.i("GoCore", "Stopping active core session")
        runCatching { logout?.invoke(null) }
            .onFailure { Logger.e("GoCore", "Core stop failed", it) }
    }

    private fun Class<*>?.method(name: String, vararg types: Class<*>): Method? =
        this?.methods?.firstOrNull { candidate ->
            candidate.name.equals(name, ignoreCase = true) &&
                candidate.parameterTypes.contentEquals(types)
        }

    private fun Class<*>?.methodNamed(name: String, parameterCount: Int): Method? =
        this?.methods?.firstOrNull {
            it.name.equals(name, ignoreCase = true) && it.parameterCount == parameterCount
        }

    private fun invokeJson(
        method: Method,
        profile: ConnectionProfile,
        onChallenge: (String) -> String = { "" },
    ): JSONObject {
        val config = JSONObject()
            .put("protocol", if (profile.protocol == VpnProtocol.ATRUST) "atrust" else "easyconnect")
            .put("server", profile.server)
            .put("port", profile.port)
            .put("username", profile.username)
            .put("password", profile.password)
            .put("totpSecret", profile.totpSecret)
            .put("authType", profile.authType)
            .put("loginDomain", profile.loginDomain)
            .put("phone", profile.phone)
            .put("clientData", profile.clientData)
            .put("socksBind", "127.0.0.1:${profile.socksPort}")
            .put("httpBind", "127.0.0.1:${profile.httpPort}")
            .put("remoteDns", profile.dnsServers.firstOrNull().orEmpty())
            .put("secondaryDns", profile.dnsServers.getOrNull(1).orEmpty())
            .put("dnsTtl", profile.dnsTtl)
            .put("proxyAll", profile.proxyAll)
            .put("disableServerConfig", profile.disableServerConfig)
            .put("updateBestNodesInterval", profile.updateBestNodesInterval)
            .put("sessionRefreshInterval", profile.sessionRefreshInterval)
        val arguments = if (method.parameterCount == 2) {
            val callbackType = method.parameterTypes[1]
            val callback = Proxy.newProxyInstance(
                callbackType.classLoader,
                arrayOf(callbackType),
            ) { _, invoked, values ->
                when {
                    invoked.name.equals("onChallenge", ignoreCase = true) ->
                        onChallenge(values?.firstOrNull()?.toString().orEmpty())
                    invoked.name == "toString" -> "DistrustChallengeCallback"
                    else -> null
                }
            }
            arrayOf(config.toString(), callback)
        } else {
            arrayOf(config.toString())
        }
        val result = JSONObject(method.invoke(null, *arguments)?.toString().orEmpty())
        check(result.optBoolean("ok")) {
            result.optString("errorMessage", "核心操作失败")
        }
        Logger.d("GoCore", "Core operation succeeded; method=${method.name}")
        return result
    }

    private fun JSONObject.stringList(name: String): List<String> {
        val values = optJSONArray(name) ?: return emptyList()
        return buildList {
            for (index in 0 until values.length()) {
                values.optString(index).takeIf(String::isNotBlank)?.let(::add)
            }
        }
    }
}

package idont.trust.atrust.core

import idont.trust.atrust.model.ConnectionProfile
import idont.trust.atrust.model.VpnProtocol
import java.lang.reflect.Method

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
    private val startStack: Method? = mobileClass.method("startStack", Long::class.javaPrimitiveType!!)
        ?: mobileClass.method("startStack", Int::class.javaPrimitiveType!!)
    private val logout: Method? = mobileClass.method("logout")

    override val capabilities = CoreCapabilities(
        easyConnectVpn = login != null && startStack != null,
        aTrustVpn = false,
        localSocks5 = false,
        localHttp = false,
    )

    override fun login(profile: ConnectionProfile): Result<NegotiatedTunnel> = runCatching {
        require(profile.protocol == VpnProtocol.EASYCONNECT) {
            "当前 Go AAR 尚未导出 aTrust 移动接口"
        }
        val method = checkNotNull(login) { "未安装 zju-connect Android AAR" }
        val address = method.invoke(null, "${profile.server}:${profile.port}", profile.username, profile.password)
            ?.toString().orEmpty()
        check(address.isNotBlank()) { "核心登录失败，请检查服务器和凭据" }
        NegotiatedTunnel(address = address)
    }

    override fun runTun(fileDescriptor: Int): Result<Unit> = runCatching {
        val method = checkNotNull(startStack) { "当前核心不支持 Android TUN" }
        val parameter = method.parameterTypes.singleOrNull()
        if (parameter == java.lang.Long.TYPE) {
            method.invoke(null, fileDescriptor.toLong())
        } else {
            method.invoke(null, fileDescriptor)
        }
    }

    override fun startLocalProxy(profile: ConnectionProfile): Result<Unit> = Result.failure(
        UnsupportedOperationException("上游移动核心尚未导出 SOCKS5/HTTP 代理服务"),
    )

    override fun stop() {
        runCatching { logout?.invoke(null) }
    }

    private fun Class<*>?.method(name: String, vararg types: Class<*>): Method? =
        this?.methods?.firstOrNull { candidate ->
            candidate.name.equals(name, ignoreCase = true) &&
                candidate.parameterTypes.contentEquals(types)
        }
}

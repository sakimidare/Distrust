package idont.trust.atrust.model

import java.net.InetAddress
import java.net.URI

data class ValidationIssue(val field: String, val message: String)

object ProfileValidator {
    fun validate(profile: ConnectionProfile): List<ValidationIssue> = buildList {
        if (profile.server.isBlank()) add(ValidationIssue("server", "服务器地址不能为空"))
        if (profile.port !in 1..65535) add(ValidationIssue("port", "服务器端口必须在 1～65535 之间"))
        val passwordAuth = (profile.protocol == VpnProtocol.EASYCONNECT && profile.easyConnectTwfId.isBlank()) ||
            profile.authType.removePrefix("auth/") == "psw"
        if (passwordAuth && profile.username.isBlank()) add(ValidationIssue("username", "账号不能为空"))
        if (passwordAuth && profile.password.isBlank()) add(ValidationIssue("password", "密码不能为空"))
        if (profile.authType.removePrefix("auth/") == "smsCheckCode" && profile.phone.isBlank()) {
            add(ValidationIssue("phone", "短信认证需要手机号码"))
        }
        if (profile.socksPort !in 1..65535) add(ValidationIssue("socksPort", "SOCKS5 端口无效"))
        if (profile.httpPort !in 1..65535) add(ValidationIssue("httpPort", "HTTP 端口无效"))
        if (profile.socksPort == profile.httpPort) add(ValidationIssue("httpPort", "SOCKS5 和 HTTP 端口不能相同"))
        if (profile.socksUsername.isBlank() != profile.socksPassword.isBlank()) {
            add(ValidationIssue("socksAuth", "SOCKS5 用户名和密码必须同时填写"))
        }
        if (profile.dialDirectProxy.isNotBlank() && !Regex("^(http|socks)://[^:/\\s]+:[0-9]{1,5}$").matches(profile.dialDirectProxy)) {
            add(ValidationIssue("dialDirectProxy", "直连上游代理格式无效"))
        }
        if (profile.keepAliveUrl.isNotBlank() && !isHttpUrl(profile.keepAliveUrl)) {
            add(ValidationIssue("keepAliveUrl", "保活 URL 必须是有效的 HTTP/HTTPS 地址"))
        }
        if (profile.tcpTunnelOnly && profile.protocol != VpnProtocol.ATRUST) {
            add(ValidationIssue("tcpTunnelOnly", "TCP Tunnel 模式需要 aTrust 协议"))
        }
        profile.routes.filterNot(::isCidr).forEach {
            add(ValidationIssue("routes", "无效的 CIDR：$it"))
        }
        profile.dnsServers.filterNot(::isIpAddress).forEach {
            add(ValidationIssue("dns", "无效的 DNS 地址：$it"))
        }
        profile.customDns.forEach { (domain, address) ->
            if (domain.isBlank() || !domain.contains('.')) add(ValidationIssue("customDns", "无效的自定义 DNS 域名：$domain"))
            if (!isIpAddress(address)) add(ValidationIssue("customDns", "无效的自定义 DNS 地址：$address"))
        }
        profile.customProxyDomains.filter { it.isBlank() || !it.contains('.') || it.contains(' ') }.forEach {
            add(ValidationIssue("customProxyDomains", "无效的强制代理域名：$it"))
        }
    }

    private fun isCidr(value: String): Boolean {
        val parts = value.split('/', limit = 2)
        if (parts.size != 2 || !isIpAddress(parts[0])) return false
        val prefix = parts[1].toIntOrNull() ?: return false
        val maximum = if (parts[0].contains(':')) 128 else 32
        return prefix in 0..maximum
    }

    private fun isIpAddress(value: String): Boolean = runCatching {
        // Avoid accepting host names: DNS configuration must be deterministic.
        require(value.contains('.') || value.contains(':'))
        InetAddress.getByName(value)
    }.isSuccess

    private fun isHttpUrl(value: String): Boolean = runCatching {
        val uri = URI(value)
        uri.scheme in setOf("http", "https") && !uri.host.isNullOrBlank()
    }.getOrDefault(false)
}

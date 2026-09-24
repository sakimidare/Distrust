package idont.trust.atrust.util

import idont.trust.atrust.model.ConnectionProfile

object ProxyConfigFormatter {
    fun mihomo(profile: ConnectionProfile): String = """
        proxies:
          - name: Distrust-aTrust
            type: socks5
            server: 127.0.0.1
            port: ${profile.socksPort}
            udp: true

        proxy-groups:
          - name: 校园网
            type: select
            proxies:
              - Distrust-aTrust
              - DIRECT

        rules:
        ${profile.routes.joinToString("\n") { "  - IP-CIDR,$it,校园网,no-resolve" }}
          - MATCH,你的代理组
    """.trimIndent()
}

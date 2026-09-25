package idont.trust.atrust.ui.navigation

import kotlinx.serialization.Serializable
import top.yukonga.miuix.kmp.nav.core.NavKey

/** Serializable MIUIX navigation keys, mirroring ReSukiSU's public sealed Route hierarchy. */
@Serializable
sealed interface AppRoute : NavKey {
    @Serializable
    data object Main : AppRoute

    @Serializable
    data object Sso : AppRoute

    @Serializable
    data object Wizard : AppRoute

    @Serializable
    data object ConnectionSettings : AppRoute

    @Serializable
    data object ProxySettings : AppRoute

    @Serializable
    data object PolicySettings : AppRoute

    @Serializable
    data object SessionSettings : AppRoute

    @Serializable
    data object AppRoutingSettings : AppRoute

    @Serializable
    data object DnsCacheSettings : AppRoute
}

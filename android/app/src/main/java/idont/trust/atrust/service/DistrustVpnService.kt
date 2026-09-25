package idont.trust.atrust.service

import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import androidx.core.content.ContextCompat
import idont.trust.atrust.core.CoreBridge
import idont.trust.atrust.core.GoMobileCoreBridge
import idont.trust.atrust.data.ProfileRepository
import idont.trust.atrust.model.ConnectionMode
import idont.trust.atrust.model.AppRoutingMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import idont.trust.atrust.logging.Logger

class DistrustVpnService : VpnService() {
    private val scope = CoroutineScope(Job() + Dispatchers.IO)
    private val core: CoreBridge = GoMobileCoreBridge()
    private var tun: ParcelFileDescriptor? = null

    override fun onCreate() {
        super.onCreate()
        Logger.i("VpnService", "Service created")
        ServiceNotifications.createChannels(this)
        scope.launch {
            val repository = ProfileRepository(applicationContext)
            SessionRuntime.events.collect { event ->
                when (event) {
                    is SessionEvent.ClientDataUpdated -> repository.updateClientData(event.clientData)
                    is SessionEvent.Expired -> {
                        Logger.w("VpnService", "Stopping VPN because the aTrust session expired: ${event.reason}")
                        repository.updateClientData("")
                        fail("aTrust 会话已过期，请重新完成认证")
                    }
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Logger.d("VpnService", "onStartCommand; action=${intent?.action}, startId=$startId")
        when (intent?.action) {
            ACTION_STOP -> disconnect()
            ACTION_START -> connect()
        }
        if (intent == null && ConnectionRuntime.state.value is ConnectionState.Disconnected) {
            Logger.i("VpnService", "Android recreated sticky VPN service; reconnecting from saved profile")
            connect()
        }
        return START_STICKY
    }

    override fun onRevoke() {
        Logger.w("VpnService", "VPN permission revoked by Android")
        disconnect()
        super.onRevoke()
    }

    override fun onDestroy() {
        Logger.i("VpnService", "Service destroyed")
        closeResources()
        scope.cancel()
        super.onDestroy()
    }

    private fun connect() {
        if (ConnectionRuntime.state.value is ConnectionState.Connecting ||
            ConnectionRuntime.state.value is ConnectionState.Connected
        ) {
            Logger.w("VpnService", "Ignoring duplicate connect request; state=${ConnectionRuntime.state.value}")
            return
        }
        startForeground(
            ServiceNotifications.VPN_NOTIFICATION,
            ServiceNotifications.build(
                this,
                ServiceNotifications.VPN_CHANNEL,
                "Distrust",
                "正在建立 VPN 连接",
                stopIntent(this),
            ),
        )
        ConnectionRuntime.update(ConnectionState.Connecting(ConnectionMode.VPN))
        Logger.i("VpnService", "Starting system VPN connection")

        scope.launch {
            val repository = ProfileRepository(applicationContext)
            val profile = repository.profile.first()
            if (profile.appRoutingMode == AppRoutingMode.ALLOW_ONLY && profile.routedPackages.isEmpty()) {
                fail("按应用包含模式至少需要选择一个应用")
                return@launch
            }
            val negotiated = core.login(profile, AuthRuntime::request).getOrElse { error ->
                Logger.e("VpnService", "Core login failed", error)
                fail(error.userMessage())
                return@launch
            }
            if (negotiated.clientData.isNotEmpty()) {
                repository.updateClientData(negotiated.clientData)
            }
            if (negotiated.domainResources.isNotEmpty()) {
                Logger.w(
                    "VpnService",
                    "Server advertised ${negotiated.domainResources.size} domain resources. " +
                        "The current Android L3 TUN path cannot preserve domain routing metadata; " +
                        "domain-only resources may return HTTP 421. Prefer local proxy mode until tun2socks/Fake-IP integration is complete.",
                )
                Logger.d("VpnService", "Domain resources=${negotiated.domainResources.joinToString()}")
            }

            val builder = Builder()
                .setSession("Distrust · ${profile.name}")
                .setMtu(negotiated.mtu)
                .addAddress(negotiated.address, negotiated.prefixLength)

            // The core's own gateway sockets must never re-enter this VPN.
            when (profile.appRoutingMode) {
                AppRoutingMode.ALL -> runCatching { builder.addDisallowedApplication(packageName) }
                AppRoutingMode.EXCLUDE -> (profile.routedPackages + packageName).forEach { appPackage ->
                    runCatching { builder.addDisallowedApplication(appPackage) }
                        .onFailure { Logger.w("VpnService", "Ignoring unavailable excluded app '$appPackage'", it) }
                }
                AppRoutingMode.ALLOW_ONLY -> profile.routedPackages.filterNot { it == packageName }.forEach { appPackage ->
                    runCatching { builder.addAllowedApplication(appPackage) }
                        .onFailure { Logger.w("VpnService", "Ignoring unavailable allowed app '$appPackage'", it) }
                }
            }
            val routes = negotiated.routes.ifEmpty { profile.routes }
            routes.forEach { route ->
                parseCidr(route)?.let { (address, prefix) -> builder.addRoute(address, prefix) }
                    ?: Logger.w("VpnService", "Ignoring invalid route '$route'")
            }
            if (negotiated.domainResources.isNotEmpty()) {
                builder.addRoute("198.18.0.0", 16)
                Logger.d("VpnService", "Added FakeDNS route 198.18.0.0/16")
            }
            val dnsServers = negotiated.dnsServers.ifEmpty { profile.dnsServers }
            dnsServers.forEach { dns ->
                runCatching { builder.addDnsServer(dns) }
                    .onFailure { Logger.w("VpnService", "Ignoring invalid DNS '$dns'", it) }
            }

            tun = builder.establish()
            if (tun == null) {
                fail("Android TUN 接口创建失败，请重新授权 VPN")
                return@launch
            }
            Logger.d("VpnService", "Android TUN established; mtu=${negotiated.mtu}")

            ConnectionRuntime.update(
                ConnectionState.Connected(ConnectionMode.VPN, negotiated.address),
            )
            Logger.i("VpnService", "VPN connected; address=${negotiated.address}, routes=${routes.size}, dns=${dnsServers.size}")
            val goOwnedDescriptor = ParcelFileDescriptor.dup(checkNotNull(tun).fileDescriptor)
            val goOwnedFd = goOwnedDescriptor.detachFd()
            Logger.d("VpnService", "Transferred duplicated TUN descriptor to Go; fd=$goOwnedFd")
            val result = core.runTun(goOwnedFd)
            if (result.isFailure && ConnectionRuntime.state.value is ConnectionState.Connected) {
                fail(result.exceptionOrNull().userMessage())
            } else if (ConnectionRuntime.state.value is ConnectionState.Failed) {
                Logger.d("VpnService", "TUN exited after a reported failure; preserving Failed state")
                return@launch
            } else {
                disconnect()
            }
        }
    }

    private fun disconnect() {
        ConnectionRuntime.update(ConnectionState.Disconnecting)
        Logger.i("VpnService", "Disconnecting VPN")
        closeResources()
        ConnectionRuntime.update(ConnectionState.Disconnected)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun fail(message: String) {
        closeResources()
        Logger.e("VpnService", message)
        ConnectionRuntime.update(ConnectionState.Failed(message))
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun closeResources() {
        Logger.d("VpnService", "Closing TUN and core resources")
        runCatching { tun?.close() }
        tun = null
        core.stop()
    }

    private fun parseCidr(value: String): Pair<String, Int>? {
        val pieces = value.trim().split('/', limit = 2)
        if (pieces.size != 2) return null
        val prefix = pieces[1].toIntOrNull() ?: return null
        return pieces[0] to prefix
    }

    private fun Throwable?.userMessage(): String =
        this?.cause?.message ?: this?.message ?: "连接核心发生未知错误"

    companion object {
        private const val ACTION_START = "idont.trust.atrust.action.START_VPN"
        private const val ACTION_STOP = "idont.trust.atrust.action.STOP_VPN"

        fun start(context: Context) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, DistrustVpnService::class.java).setAction(ACTION_START),
            )
        }

        fun stopIntent(context: Context): Intent =
            Intent(context, DistrustVpnService::class.java).setAction(ACTION_STOP)
    }
}

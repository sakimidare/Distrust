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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
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
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Logger.d("VpnService", "onStartCommand; action=${intent?.action}, startId=$startId")
        when (intent?.action) {
            ACTION_STOP -> disconnect()
            ACTION_START -> connect()
        }
        return START_NOT_STICKY
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
            val negotiated = core.login(profile, AuthRuntime::request).getOrElse { error ->
                Logger.e("VpnService", "Core login failed", error)
                fail(error.userMessage())
                return@launch
            }
            if (negotiated.clientData.isNotEmpty()) {
                repository.updateClientData(negotiated.clientData)
            }

            val builder = Builder()
                .setSession("Distrust · ${profile.name}")
                .setMtu(negotiated.mtu)
                .addAddress(negotiated.address, negotiated.prefixLength)

            // The core's own gateway sockets must never re-enter this VPN.
            runCatching { builder.addDisallowedApplication(packageName) }
            val routes = negotiated.routes.ifEmpty { profile.routes }
            routes.forEach { route ->
                parseCidr(route)?.let { (address, prefix) -> builder.addRoute(address, prefix) }
                    ?: Logger.w("VpnService", "Ignoring invalid route '$route'")
            }
            val dnsServers = negotiated.dnsServers.ifEmpty { profile.dnsServers }
            dnsServers.forEach { dns ->
                runCatching { builder.addDnsServer(dns) }
                    .onFailure { Logger.w("VpnService", "Ignoring invalid DNS '$dns'", it) }
            }

            tun = builder.establish()
            if (tun == null) {
                fail("Android 未能创建 TUN 接口，请重新授权 VPN")
                return@launch
            }
            Logger.d("VpnService", "Android TUN established; mtu=${negotiated.mtu}")

            ConnectionRuntime.update(
                ConnectionState.Connected(ConnectionMode.VPN, negotiated.address),
            )
            Logger.i("VpnService", "VPN connected; address=${negotiated.address}, routes=${routes.size}, dns=${dnsServers.size}")
            val result = core.runTun(checkNotNull(tun).fd)
            if (result.isFailure && ConnectionRuntime.state.value is ConnectionState.Connected) {
                fail(result.exceptionOrNull().userMessage())
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

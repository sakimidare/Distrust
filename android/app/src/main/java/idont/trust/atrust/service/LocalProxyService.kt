package idont.trust.atrust.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
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
import kotlinx.coroutines.flow.collect
import idont.trust.atrust.logging.Logger

class LocalProxyService : Service() {
    private val scope = CoroutineScope(Job() + Dispatchers.IO)
    private val core: CoreBridge = GoMobileCoreBridge()

    override fun onCreate() {
        super.onCreate()
        Logger.i("ProxyService", "Service created")
        ServiceNotifications.createChannels(this)
        scope.launch {
            val repository = ProfileRepository(applicationContext)
            SessionRuntime.events.collect { event ->
                when (event) {
                    is SessionEvent.ClientDataUpdated -> repository.updateClientData(event.clientData)
                    is SessionEvent.Expired -> {
                        Logger.w("ProxyService", "Stopping proxy because the aTrust session expired: ${event.reason}")
                        repository.updateClientData("")
                        core.stop()
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        stopSelf()
                    }
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Logger.d("ProxyService", "onStartCommand; action=${intent?.action}, startId=$startId")
        when (intent?.action) {
            ACTION_STOP -> disconnect()
            ACTION_START -> connect()
        }
        if (intent == null && ConnectionRuntime.state.value is ConnectionState.Disconnected) {
            Logger.i("ProxyService", "Android recreated sticky proxy service; reconnecting from saved profile")
            connect()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        Logger.i("ProxyService", "Service destroyed")
        core.stop()
        scope.cancel()
        super.onDestroy()
    }

    private fun connect() {
        SessionRuntime.resetHealth()
        startForeground(
            ServiceNotifications.PROXY_NOTIFICATION,
            ServiceNotifications.build(
                this,
                ServiceNotifications.PROXY_CHANNEL,
                "Distrust 本地代理",
                "正在启动 SOCKS5/HTTP 代理",
                stopIntent(this),
            ),
        )
        ConnectionRuntime.update(ConnectionState.Connecting(ConnectionMode.LOCAL_PROXY))
        Logger.i("ProxyService", "Starting local proxy service")
        scope.launch {
            val repository = ProfileRepository(applicationContext)
            val profile = repository.profile.first()
            core.startLocalProxy(profile, AuthRuntime::request).fold(
                onSuccess = { proxy ->
                    if (proxy.clientData.isNotEmpty()) {
                        repository.updateClientData(proxy.clientData)
                    }
                    ConnectionRuntime.update(
                        ConnectionState.Connected(
                            ConnectionMode.LOCAL_PROXY,
                            proxy.socksAddress.ifEmpty { proxy.httpAddress },
                        ),
                    )
                    Logger.i("ProxyService", "Local proxy listening; socks=${proxy.socksAddress}, http=${proxy.httpAddress}")
                },
                onFailure = { error ->
                    Logger.e("ProxyService", error.cause?.message ?: error.message ?: "本地代理启动失败", error)
                    ConnectionRuntime.update(
                        ConnectionState.Failed(
                            error.cause?.message ?: error.message ?: "本地代理启动失败",
                        ),
                    )
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                },
            )
        }
    }

    private fun disconnect() {
        ConnectionRuntime.update(ConnectionState.Disconnecting)
        Logger.i("ProxyService", "Stopping local proxy service")
        core.stop()
        ConnectionRuntime.update(ConnectionState.Disconnected)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    companion object {
        private const val ACTION_START = "idont.trust.atrust.action.START_PROXY"
        private const val ACTION_STOP = "idont.trust.atrust.action.STOP_PROXY"

        fun start(context: Context) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, LocalProxyService::class.java).setAction(ACTION_START),
            )
        }

        fun stopIntent(context: Context): Intent =
            Intent(context, LocalProxyService::class.java).setAction(ACTION_STOP)
    }
}

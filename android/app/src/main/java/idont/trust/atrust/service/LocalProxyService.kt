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

class LocalProxyService : Service() {
    private val scope = CoroutineScope(Job() + Dispatchers.IO)
    private val core: CoreBridge = GoMobileCoreBridge()

    override fun onCreate() {
        super.onCreate()
        ServiceNotifications.createChannels(this)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> disconnect()
            ACTION_START -> connect()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        core.stop()
        scope.cancel()
        super.onDestroy()
    }

    private fun connect() {
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
        AppLog.info("开始启动本地代理服务")
        scope.launch {
            val repository = ProfileRepository(applicationContext)
            val profile = repository.profile.first()
            core.startLocalProxy(profile).fold(
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
                    AppLog.info("本地代理已监听 127.0.0.1:${profile.socksPort}")
                },
                onFailure = { error ->
                    AppLog.error(error.cause?.message ?: error.message ?: "本地代理启动失败")
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
        AppLog.info("正在停止本地代理")
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

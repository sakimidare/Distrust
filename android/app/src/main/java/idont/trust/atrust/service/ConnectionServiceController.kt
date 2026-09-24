package idont.trust.atrust.service

import android.content.Context
import idont.trust.atrust.model.ConnectionMode
import idont.trust.atrust.logging.Logger

object ConnectionServiceController {
    fun start(context: Context, mode: ConnectionMode) {
        Logger.i("ServiceController", "Starting connection service; mode=$mode")
        when (mode) {
            ConnectionMode.VPN -> DistrustVpnService.start(context)
            ConnectionMode.LOCAL_PROXY -> LocalProxyService.start(context)
        }
    }

    fun stopAll(context: Context) {
        Logger.i("ServiceController", "Stopping all connection services")
        context.startService(DistrustVpnService.stopIntent(context))
        context.startService(LocalProxyService.stopIntent(context))
    }
}

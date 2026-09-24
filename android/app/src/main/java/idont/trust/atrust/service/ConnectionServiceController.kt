package idont.trust.atrust.service

import android.content.Context
import idont.trust.atrust.model.ConnectionMode

object ConnectionServiceController {
    fun start(context: Context, mode: ConnectionMode) {
        when (mode) {
            ConnectionMode.VPN -> DistrustVpnService.start(context)
            ConnectionMode.LOCAL_PROXY -> LocalProxyService.start(context)
        }
    }

    fun stopAll(context: Context) {
        context.startService(DistrustVpnService.stopIntent(context))
        context.startService(LocalProxyService.stopIntent(context))
    }
}

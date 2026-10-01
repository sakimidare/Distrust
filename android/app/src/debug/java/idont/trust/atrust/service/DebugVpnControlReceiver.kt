package idont.trust.atrust.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.VpnService
import idont.trust.atrust.logging.Logger

/** ADB-only control surface used by the native TUN lifecycle stress test. */
class DebugVpnControlReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_START -> {
                if (VpnService.prepare(context) != null) {
                    Logger.e("VpnStress", "VPN permission has not been granted; open the app once first")
                    return
                }
                Logger.i("VpnStress", "ADB requested VPN start")
                DistrustVpnService.start(context)
            }
            ACTION_STOP -> {
                Logger.i("VpnStress", "ADB requested VPN stop")
                context.startService(DistrustVpnService.stopIntent(context))
            }
        }
    }

    companion object {
        private const val ACTION_START = "idont.trust.atrust.DEBUG_START_VPN"
        private const val ACTION_STOP = "idont.trust.atrust.DEBUG_STOP_VPN"
    }
}

package idont.trust.atrust.service

import android.content.Context
import idont.trust.atrust.logging.Logger

object ConnectionServiceController {
	fun start(context: Context) {
		Logger.i("ServiceController", "Starting shared connection service")
		DistrustVpnService.start(context)
	}

    fun stopAll(context: Context) {
        Logger.i("ServiceController", "Stopping all connection services")
        context.startService(DistrustVpnService.stopIntent(context))
		// The VPN service owns the shared session and all frontends.
    }
}

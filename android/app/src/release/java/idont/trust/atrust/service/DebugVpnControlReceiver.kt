package idont.trust.atrust.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Release builds intentionally ignore the debug-only ADB actions. */
class DebugVpnControlReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = Unit
}

package idont.trust.atrust

import android.Manifest
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import idont.trust.atrust.model.ConnectionMode
import idont.trust.atrust.model.ConnectionProfile
import idont.trust.atrust.service.ConnectionServiceController
import idont.trust.atrust.service.DistrustVpnService
import idont.trust.atrust.ui.DistrustApp
import idont.trust.atrust.ui.MainViewModel
import idont.trust.atrust.ui.theme.DistrustTheme
import idont.trust.atrust.logging.Logger

class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<MainViewModel>()

    private val vpnPermission = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        Logger.i("MainActivity", "VPN permission result=${result.resultCode}")
        if (result.resultCode == RESULT_OK) {
            DistrustVpnService.start(this)
        }
    }

    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Logger.i("MainActivity", "onCreate; restored=${savedInstanceState != null}")
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent {
            val profile by viewModel.profile.collectAsStateWithLifecycle()
            val state by viewModel.connectionState.collectAsStateWithLifecycle()
            val logs by viewModel.logs.collectAsStateWithLifecycle()
            val authChallenge by viewModel.authChallenge.collectAsStateWithLifecycle()
            val authDiscovery by viewModel.authDiscovery.collectAsStateWithLifecycle()
            DistrustTheme {
                DistrustApp(
                    profile = profile,
                    connectionState = state,
                    logs = logs,
                    authChallenge = authChallenge,
                    authDiscovery = authDiscovery,
                    onSaveProfile = viewModel::save,
                    onClearLogs = viewModel::clearLogs,
                    onClearSession = {
                        ConnectionServiceController.stopAll(this)
                        viewModel.clearSession()
                    },
                    onSubmitAuth = viewModel::submitAuth,
                    onCancelAuth = viewModel::cancelAuth,
                    onFetchAuthMethods = viewModel::fetchAuthMethods,
                    onResetAuthDiscovery = viewModel::resetAuthDiscovery,
                    onConnect = ::connect,
                    onDisconnect = {
                        ConnectionServiceController.stopAll(this)
                    },
                )
            }
        }
    }

    private fun connect(profile: ConnectionProfile) {
        Logger.i("MainActivity", "Connect requested; mode=${profile.mode}, protocol=${profile.protocol}, server=${profile.server}:${profile.port}")
        viewModel.save(profile)
        if (profile.mode == ConnectionMode.LOCAL_PROXY) {
            ConnectionServiceController.start(this, ConnectionMode.LOCAL_PROXY)
            return
        }
        val permissionIntent: Intent? = VpnService.prepare(this)
        if (permissionIntent == null) {
            Logger.d("MainActivity", "VPN permission already granted")
            DistrustVpnService.start(this)
        } else {
            Logger.i("MainActivity", "Requesting Android VPN permission")
            vpnPermission.launch(permissionIntent)
        }
    }

    override fun onStart() {
        super.onStart()
        Logger.d("MainActivity", "onStart")
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        Logger.d("MainActivity", "Reused singleTask activity; action=${intent.action}, data=${intent.data}")
    }

    override fun onStop() {
        Logger.d("MainActivity", "onStop")
        super.onStop()
    }

    override fun onDestroy() {
        Logger.d("MainActivity", "onDestroy; changingConfigurations=$isChangingConfigurations")
        super.onDestroy()
    }
}

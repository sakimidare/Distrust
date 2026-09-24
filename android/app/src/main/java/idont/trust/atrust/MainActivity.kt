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

class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<MainViewModel>()

    private val vpnPermission = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            DistrustVpnService.start(this)
        }
    }

    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
        viewModel.save(profile)
        if (profile.mode == ConnectionMode.LOCAL_PROXY) {
            ConnectionServiceController.start(this, ConnectionMode.LOCAL_PROXY)
            return
        }
        val permissionIntent: Intent? = VpnService.prepare(this)
        if (permissionIntent == null) {
            DistrustVpnService.start(this)
        } else {
            vpnPermission.launch(permissionIntent)
        }
    }
}

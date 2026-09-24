package idont.trust.atrust.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import idont.trust.atrust.core.AuthMethod
import idont.trust.atrust.model.ConnectionMode
import idont.trust.atrust.model.ConnectionProfile
import idont.trust.atrust.model.ProfileValidator
import idont.trust.atrust.model.VpnProtocol

private enum class WizardStep(val route: String, val title: String) {
    PROTOCOL("wizard_protocol", "选择协议"),
    SERVER("wizard_server", "服务器"),
    AUTH("wizard_auth", "认证方式"),
    CREDENTIALS("wizard_credentials", "账号凭据"),
    MODE("wizard_mode", "运行方式"),
}

@Composable
fun ConfigurationWizardScreen(
    initial: ConnectionProfile,
    authDiscovery: AuthDiscoveryState,
    onFetchAuthMethods: (String, Int) -> Unit,
    onResetAuthDiscovery: () -> Unit,
    onFinish: (ConnectionProfile) -> Unit,
) {
    val navController = rememberNavController()
    var draft by remember(initial) { mutableStateOf(initial) }
    var step by remember { mutableStateOf(WizardStep.PROTOCOL) }

    Column(Modifier.fillMaxSize()) {
        LinearProgressIndicator(
            progress = { (WizardStep.entries.indexOf(step) + 1f) / WizardStep.entries.size },
            modifier = Modifier.fillMaxWidth(),
        )
        Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
            Text("第 ${WizardStep.entries.indexOf(step) + 1} 步，共 ${WizardStep.entries.size} 步", style = MaterialTheme.typography.labelMedium)
            Text(step.title, style = MaterialTheme.typography.headlineSmall)
        }
        NavHost(
            navController = navController,
            startDestination = WizardStep.PROTOCOL.route,
            modifier = Modifier.weight(1f),
        ) {
            composable(WizardStep.PROTOCOL.route) {
                WizardPage {
                    Text("选择服务器使用的协议。aTrust 可以主动读取服务器公开的认证配置。")
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        VpnProtocol.entries.forEachIndexed { index, protocol ->
                            SegmentedButton(
                                selected = draft.protocol == protocol,
                                onClick = { draft = draft.copy(protocol = protocol) },
                                shape = SegmentedButtonDefaults.itemShape(index, VpnProtocol.entries.size),
                            ) { Text(if (protocol == VpnProtocol.ATRUST) "aTrust" else "EasyConnect") }
                        }
                    }
                    WizardNext { step = WizardStep.SERVER; navController.navigate(step.route) }
                }
            }
            composable(WizardStep.SERVER.route) {
                WizardPage {
                    OutlinedTextField(draft.server, { draft = draft.copy(server = it) }, label = { Text("服务器地址") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(draft.port.toString(), { value -> value.toIntOrNull()?.let { draft = draft.copy(port = it.coerceIn(1, 65535)) } }, label = { Text("端口") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), singleLine = true)
                    WizardNavigation(
                        onBack = { step = WizardStep.PROTOCOL; navController.popBackStack() },
                        onNext = {
                            step = if (draft.protocol == VpnProtocol.ATRUST) WizardStep.AUTH else WizardStep.CREDENTIALS
                            navController.navigate(step.route)
                        },
                        nextEnabled = draft.server.isNotBlank(),
                    )
                }
            }
            composable(WizardStep.AUTH.route) {
                WizardPage {
                    Text("Distrust 会直接请求服务器的 aTrust 认证配置，不会尝试登录。")
                    FilledTonalButton(
                        onClick = { onFetchAuthMethods(draft.server, draft.port) },
                        enabled = authDiscovery !is AuthDiscoveryState.Loading,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (authDiscovery is AuthDiscoveryState.Loading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        else Icon(Icons.Default.CloudSync, null)
                        Spacer(Modifier.size(8.dp))
                        Text(if (authDiscovery is AuthDiscoveryState.Loading) "正在读取" else "从服务器获取认证方式")
                    }
                    when (authDiscovery) {
                        AuthDiscoveryState.Idle, AuthDiscoveryState.Loading -> Unit
                        is AuthDiscoveryState.Error -> Text(authDiscovery.message, color = MaterialTheme.colorScheme.error)
                        is AuthDiscoveryState.Success -> {
                            if (authDiscovery.methods.isEmpty()) Text("服务器没有返回可用认证方式")
                            authDiscovery.methods.forEach { method ->
                                AuthMethodCard(method, draft) {
                                    draft = draft.copy(
                                        authType = method.type.removePrefix("auth/"),
                                        loginDomain = method.loginDomain,
                                        loginUrl = method.loginUrl,
                                    )
                                }
                            }
                        }
                    }
                    WizardNavigation(
                        onBack = { onResetAuthDiscovery(); step = WizardStep.SERVER; navController.popBackStack() },
                        onNext = { step = WizardStep.CREDENTIALS; navController.navigate(step.route) },
                    )
                }
            }
            composable(WizardStep.CREDENTIALS.route) {
                WizardPage {
                    val normalized = draft.authType.removePrefix("auth/")
                    if (normalized == "smsCheckCode") {
                        OutlinedTextField(draft.phone, { draft = draft.copy(phone = it) }, label = { Text("手机号码（如 86-13800000000）") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    } else if (normalized == "cas" || normalized == "httpsOauth2") {
                        Text("连接时将在应用内打开 SSO 登录页面，无需在此填写密码。")
                    } else {
                        OutlinedTextField(draft.username, { draft = draft.copy(username = it) }, label = { Text("账号") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        OutlinedTextField(draft.password, { draft = draft.copy(password = it) }, label = { Text("密码") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), singleLine = true)
                        OutlinedTextField(draft.totpSecret, { draft = draft.copy(totpSecret = it) }, label = { Text("TOTP 密钥（可选）") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), singleLine = true)
                    }
                    WizardNavigation(
                        onBack = { step = if (draft.protocol == VpnProtocol.ATRUST) WizardStep.AUTH else WizardStep.SERVER; navController.popBackStack() },
                        onNext = { step = WizardStep.MODE; navController.navigate(step.route) },
                    )
                }
            }
            composable(WizardStep.MODE.route) {
                WizardPage {
                    Text("本地代理模式不会占用 Android 的 VPN 槽位，推荐与 Clash/Mihomo 或抓包软件配合。")
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        ConnectionMode.entries.forEachIndexed { index, mode ->
                            SegmentedButton(
                                selected = draft.mode == mode,
                                onClick = { draft = draft.copy(mode = mode) },
                                shape = SegmentedButtonDefaults.itemShape(index, ConnectionMode.entries.size),
                            ) { Text(if (mode == ConnectionMode.LOCAL_PROXY) "本地代理" else "系统 VPN") }
                        }
                    }
                    if (draft.mode == ConnectionMode.LOCAL_PROXY) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(draft.socksPort.toString(), { it.toIntOrNull()?.let { port -> draft = draft.copy(socksPort = port) } }, label = { Text("SOCKS5") }, modifier = Modifier.weight(1f), singleLine = true)
                            OutlinedTextField(draft.httpPort.toString(), { it.toIntOrNull()?.let { port -> draft = draft.copy(httpPort = port) } }, label = { Text("HTTP") }, modifier = Modifier.weight(1f), singleLine = true)
                        }
                    }
                    val issues = ProfileValidator.validate(draft)
                    issues.firstOrNull()?.let { Text(it.message, color = MaterialTheme.colorScheme.error) }
                    WizardNavigation(
                        onBack = { step = WizardStep.CREDENTIALS; navController.popBackStack() },
                        onNext = { onFinish(draft) },
                        nextEnabled = issues.isEmpty(),
                        finish = true,
                    )
                }
            }
        }
    }
}

@Composable
private fun WizardPage(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        content = content,
    )
}

@Composable
private fun AuthMethodCard(method: AuthMethod, profile: ConnectionProfile, onSelect: () -> Unit) {
    val selected = profile.authType.removePrefix("auth/") == method.type.removePrefix("auth/") && profile.loginDomain == method.loginDomain
    OutlinedCard(Modifier.fillMaxWidth().clickable(onClick = onSelect)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected, onClick = onSelect)
            Column {
                Text(method.name.ifBlank { method.type }, style = MaterialTheme.typography.titleMedium)
                Text("${method.type} · ${method.loginDomain}", style = MaterialTheme.typography.bodySmall)
                if (method.loginUrl.isNotBlank()) Text(method.loginUrl, maxLines = 1, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun WizardNext(onNext: () -> Unit) = WizardNavigation(null, onNext)

@Composable
private fun WizardNavigation(
    onBack: (() -> Unit)?,
    onNext: () -> Unit,
    nextEnabled: Boolean = true,
    finish: Boolean = false,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        if (onBack != null) {
            FilledTonalButton(onClick = onBack, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.ArrowBack, null)
                Text("上一步")
            }
        }
        Button(onClick = onNext, enabled = nextEnabled, modifier = Modifier.weight(1f)) {
            Text(if (finish) "完成" else "下一步")
            Icon(if (finish) Icons.Default.Check else Icons.Default.ArrowForward, null)
        }
    }
}

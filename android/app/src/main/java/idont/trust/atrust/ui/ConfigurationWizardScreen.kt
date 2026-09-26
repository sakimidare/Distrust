package idont.trust.atrust.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.util.Base64
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Lan
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.platform.LocalContext
import idont.trust.atrust.core.AuthMethod
import idont.trust.atrust.model.ConnectionMode
import idont.trust.atrust.model.ConnectionProfile
import idont.trust.atrust.model.ProfileValidator
import idont.trust.atrust.model.VpnProtocol
import idont.trust.atrust.ui.component.SectionTextField
import idont.trust.atrust.ui.component.SegmentedColumn
import idont.trust.atrust.ui.component.SettingsBaseWidget
import idont.trust.atrust.ui.component.SegmentedControlWidget
import idont.trust.atrust.ui.theme.DistrustTheme
import idont.trust.atrust.logging.Logger

private enum class WizardStep(val title: String) {
    PROTOCOL("选择协议"), SERVER("服务器"), AUTH("认证方式"), CREDENTIALS("账号凭据"), MODE("运行方式")
}

@Composable
fun ConfigurationWizardScreen(
    initial: ConnectionProfile,
    authDiscovery: AuthDiscoveryState,
    onFetchAuthMethods: (String, Int) -> Unit,
    onResetAuthDiscovery: () -> Unit,
    onFinish: (ConnectionProfile) -> Unit,
    previewStep: Int = 0,
) {
    var draft by remember(initial) { mutableStateOf(initial) }
    val context = LocalContext.current
    val certificateLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        runCatching {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: error("证书读取失败")
            draft = draft.copy(certificateBase64 = Base64.encodeToString(bytes, Base64.NO_WRAP))
        }.onFailure { Logger.e("Certificate", "Failed to read EasyConnect certificate", it) }
    }
    var step by remember(previewStep) { mutableStateOf(WizardStep.entries.getOrElse(previewStep) { WizardStep.PROTOCOL }) }
    val previous: () -> Unit = {
        step = when (step) {
            WizardStep.PROTOCOL -> WizardStep.PROTOCOL
            WizardStep.SERVER -> WizardStep.PROTOCOL
            WizardStep.AUTH -> WizardStep.SERVER
            WizardStep.CREDENTIALS -> if (draft.protocol == VpnProtocol.ATRUST) WizardStep.AUTH else WizardStep.SERVER
            WizardStep.MODE -> WizardStep.CREDENTIALS
        }
    }
    BackHandler(step != WizardStep.PROTOCOL) { previous() }
    val progress by animateFloatAsState(
        targetValue = (step.ordinal + 1f) / WizardStep.entries.size,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = 420f),
        label = "wizardProgress",
    )
    Column(Modifier.fillMaxSize()) {
        androidx.compose.material3.LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth(),
        )
        Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
            Text("第 ${step.ordinal + 1} 步，共 ${WizardStep.entries.size} 步", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Text(step.title, style = MaterialTheme.typography.headlineSmall, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
        }
        AnimatedContent(
            targetState = step,
            modifier = Modifier.weight(1f),
            transitionSpec = {
                val forward = targetState.ordinal > initialState.ordinal
                val enter = slideInHorizontally(spring(0.76f, 480f)) { if (forward) it / 3 else -it / 3 } + fadeIn()
                val exit = slideOutHorizontally(spring(0.8f, 600f)) { if (forward) -it / 6 else it / 6 } + fadeOut()
                (enter togetherWith exit).using(SizeTransform(clip = false))
            },
            label = "wizardStep",
        ) { current ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().imePadding(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                when (current) {
                    WizardStep.PROTOCOL -> {
                        item {
                            SegmentedColumn("协议") {
                                item {
                                    SegmentedControlWidget("VPN 协议") {
                                        Text("选择服务器使用的协议。aTrust 可主动读取公开认证配置。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(top = 16.dp)) {
                                            VpnProtocol.entries.forEachIndexed { index, protocol ->
                                                SegmentedButton(draft.protocol == protocol, { draft = draft.copy(protocol = protocol) }, SegmentedButtonDefaults.itemShape(index, VpnProtocol.entries.size)) {
                                                    Text(if (protocol == VpnProtocol.ATRUST) "aTrust" else "EasyConnect")
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        item { WizardNavigation(null, { step = WizardStep.SERVER }) }
                    }
                    WizardStep.SERVER -> {
                        item {
                            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                SectionTextField(draft.server, { draft = draft.copy(server = it) }, "服务器地址")
                                SectionTextField(draft.port.toString(), { it.toIntOrNull()?.let { v -> draft = draft.copy(port = v.coerceIn(1, 65535)) } }, "端口", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                            }
                        }
                        item { WizardNavigation(previous, { step = if (draft.protocol == VpnProtocol.ATRUST) WizardStep.AUTH else WizardStep.CREDENTIALS }, draft.server.isNotBlank()) }
                    }
                    WizardStep.AUTH -> {
                        item {
                            SegmentedColumn("服务器认证") {
                                item {
                                    SettingsBaseWidget(
                                        title = if (authDiscovery is AuthDiscoveryState.Loading) "正在读取认证方式" else "从服务器获取认证方式",
                                        description = "读取服务器公开认证配置",
                                        icon = Icons.Rounded.CloudSync,
                                        onClick = { if (authDiscovery !is AuthDiscoveryState.Loading) onFetchAuthMethods(draft.server, draft.port) },
                                        trailingContent = if (authDiscovery is AuthDiscoveryState.Loading) ({ LoadingIndicator(Modifier.size(28.dp)) }) else null,
                                    )
                                }
                            }
                        }
                        when (authDiscovery) {
                            is AuthDiscoveryState.Error -> item { Text(authDiscovery.message, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 20.dp)) }
                            is AuthDiscoveryState.Success -> {
                                item {
                                    SegmentedColumn("可用认证方式") {
                                        authDiscovery.methods.forEachIndexed { index, method ->
                                            item(key = "${method.type}-${method.loginDomain}-$index") {
                                                AuthMethodItem(method, draft) {
                                                    draft = draft.copy(
                                                        authType = method.type.removePrefix("auth/"),
                                                        loginDomain = method.loginDomain,
                                                        loginUrl = method.loginUrl,
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            else -> Unit
                        }
                        item { WizardNavigation({ onResetAuthDiscovery(); previous() }, { step = WizardStep.CREDENTIALS }) }
                    }
                    WizardStep.CREDENTIALS -> {
                        item {
                            val normalized = draft.authType.removePrefix("auth/")
                            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                if (draft.protocol == VpnProtocol.EASYCONNECT) {
                                    SectionTextField(draft.username, { draft = draft.copy(username = it) }, "账号")
                                    SectionTextField(draft.password, { draft = draft.copy(password = it) }, "密码", visualTransformation = PasswordVisualTransformation())
                                    SectionTextField(draft.totpSecret, { draft = draft.copy(totpSecret = it) }, "TOTP 密钥（可选）", visualTransformation = PasswordVisualTransformation())
                                    SectionTextField(draft.easyConnectTwfId, { draft = draft.copy(easyConnectTwfId = it) }, "TwfID（可选）")
                                    SettingsBaseWidget(
                                        if (draft.certificateBase64.isBlank()) "选择 P12/PFX 证书" else "证书已载入",
                                        "服务器要求证书认证时使用",
                                        Icons.Rounded.Key,
                                        onClick = { certificateLauncher.launch(arrayOf("application/x-pkcs12", "application/octet-stream", "*/*")) },
                                    )
                                    SectionTextField(draft.certificatePassword, { draft = draft.copy(certificatePassword = it) }, "证书密码（可选）", enabled = draft.certificateBase64.isNotBlank(), visualTransformation = PasswordVisualTransformation())
                                } else when (normalized) {
                                    "smsCheckCode" -> SectionTextField(draft.phone, { draft = draft.copy(phone = it) }, "手机号码")
                                    "cas", "httpsOauth2" -> SettingsBaseWidget("SSO 登录", "连接时将在应用内打开安全登录页面", Icons.Rounded.Security)
                                    else -> {
                                        SectionTextField(draft.username, { draft = draft.copy(username = it) }, "账号")
                                        SectionTextField(draft.password, { draft = draft.copy(password = it) }, "密码", visualTransformation = PasswordVisualTransformation())
                                        SectionTextField(draft.totpSecret, { draft = draft.copy(totpSecret = it) }, "TOTP 密钥（可选）", visualTransformation = PasswordVisualTransformation())
                                    }
                                }
                            }
                        }
                        item { WizardNavigation(previous, { step = WizardStep.MODE }) }
                    }
                    WizardStep.MODE -> {
                        item {
                            SegmentedColumn("运行方式") {
                                item {
                                    SegmentedControlWidget("运行模式") {
                                        Text("本地代理适合搭配 Clash/Mihomo；系统 VPN 可直接接管应用流量。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(top = 16.dp)) {
                                            ConnectionMode.entries.forEachIndexed { index, mode ->
                                                SegmentedButton(draft.mode == mode, { draft = draft.copy(mode = mode) }, SegmentedButtonDefaults.itemShape(index, ConnectionMode.entries.size)) { Text(if (mode == ConnectionMode.LOCAL_PROXY) "本地代理" else "系统 VPN") }
                                            }
                                        }
                                    }
                                }
                                item(visible = draft.mode == ConnectionMode.LOCAL_PROXY) {
                                    Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        SectionTextField(draft.socksPort.toString(), { it.toIntOrNull()?.let { v -> draft = draft.copy(socksPort = v) } }, "SOCKS5", Modifier.weight(1f))
                                        SectionTextField(draft.httpPort.toString(), { it.toIntOrNull()?.let { v -> draft = draft.copy(httpPort = v) } }, "HTTP", Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                        val issues = ProfileValidator.validate(draft)
                        issues.firstOrNull()?.let { issue -> item { Text(issue.message, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 20.dp)) } }
                        item { WizardNavigation(previous, { onFinish(draft) }, issues.isEmpty(), finish = true) }
                    }
                }
            }
        }
    }
}

@Composable
private fun AuthMethodItem(method: AuthMethod, profile: ConnectionProfile, onSelect: () -> Unit) {
    val selected = profile.authType.removePrefix("auth/") == method.type.removePrefix("auth/") && profile.loginDomain == method.loginDomain
    SettingsBaseWidget(
        title = method.name.ifBlank { method.type },
        description = "${method.type} · ${method.loginDomain}",
        icon = Icons.Rounded.Key,
        selected = selected,
        onClick = onSelect,
    )
}

@Composable
private fun WizardNavigation(
    onBack: (() -> Unit)?,
    onNext: () -> Unit,
    nextEnabled: Boolean = true,
    finish: Boolean = false,
) {
    Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        if (onBack != null) {
            FilledTonalButton(onClick = onBack, modifier = Modifier.weight(1f), shape = RoundedCornerShape(20.dp)) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, null)
                Spacer(Modifier.width(6.dp))
                Text("上一步")
            }
        }
        Button(onClick = onNext, enabled = nextEnabled, modifier = Modifier.weight(1f), shape = RoundedCornerShape(20.dp)) {
            Text(if (finish) "完成" else "下一步")
            Spacer(Modifier.width(6.dp))
            Icon(if (finish) Icons.Rounded.Check else Icons.AutoMirrored.Rounded.ArrowForward, null)
        }
    }
}

private val wizardPreviewProfile = ConnectionProfile(
    name = "东南大学",
    mode = ConnectionMode.VPN,
    server = "vpn.seu.edu.cn",
    username = "preview-user",
    loginDomain = "seucas",
)

@Preview(name = "Wizard · protocol", showSystemUi = true)
@Composable
private fun WizardProtocolPreview() = DistrustTheme {
    ConfigurationWizardScreen(wizardPreviewProfile, AuthDiscoveryState.Idle, { _, _ -> }, {}, {}, 0)
}

@Preview(name = "Wizard · server", showSystemUi = true)
@Composable
private fun WizardServerPreview() = DistrustTheme {
    ConfigurationWizardScreen(wizardPreviewProfile, AuthDiscoveryState.Idle, { _, _ -> }, {}, {}, 1)
}

@Preview(name = "Wizard · auth", showSystemUi = true)
@Composable
private fun WizardAuthPreview() = DistrustTheme {
    ConfigurationWizardScreen(
        wizardPreviewProfile,
        AuthDiscoveryState.Success(
            listOf(
                AuthMethod("统一身份认证", "auth/cas", "seucas", "/passport/cas"),
                AuthMethod("账号密码", "auth/psw", "", ""),
            ),
        ),
        { _, _ -> }, {}, {}, 2,
    )
}

@Preview(name = "Wizard · credentials", showSystemUi = true)
@Composable
private fun WizardCredentialsPreview() = DistrustTheme {
    ConfigurationWizardScreen(wizardPreviewProfile, AuthDiscoveryState.Idle, { _, _ -> }, {}, {}, 3)
}

@Preview(name = "Wizard · mode", showSystemUi = true)
@Composable
private fun WizardModePreview() = DistrustTheme {
    ConfigurationWizardScreen(wizardPreviewProfile, AuthDiscoveryState.Idle, { _, _ -> }, {}, {}, 4)
}

package idont.trust.atrust.ui

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Switch
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.ui.layout.ContentScale
import idont.trust.atrust.model.ConnectionMode
import idont.trust.atrust.model.ConnectionProfile
import idont.trust.atrust.model.ProfileValidator
import idont.trust.atrust.model.VpnProtocol
import idont.trust.atrust.service.ConnectionState
import idont.trust.atrust.logging.LogEntry
import idont.trust.atrust.logging.LogLevel
import idont.trust.atrust.util.ProxyConfigFormatter
import idont.trust.atrust.logging.Logger
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import org.json.JSONObject
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

private enum class Destination(val route: String, val label: String, val icon: ImageVector) {
    HOME("home", "连接", Icons.Default.Home),
    PROFILE("profile", "配置", Icons.Default.Settings),
    LOGS("logs", "日志", Icons.AutoMirrored.Filled.ReceiptLong),
    ABOUT("about", "关于", Icons.Default.Info),
}

private const val SSO_ROUTE = "sso"
private const val WIZARD_ROUTE = "configuration_wizard"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DistrustApp(
    profile: ConnectionProfile,
    connectionState: ConnectionState,
    logs: List<LogEntry>,
    authChallenge: String?,
    authDiscovery: AuthDiscoveryState,
    onSaveProfile: (ConnectionProfile) -> Unit,
    onClearLogs: () -> Unit,
    onSubmitAuth: (String) -> Unit,
    onCancelAuth: () -> Unit,
    onFetchAuthMethods: (String, Int) -> Unit,
    onResetAuthDiscovery: () -> Unit,
    onConnect: (ConnectionProfile) -> Unit,
    onDisconnect: () -> Unit,
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    LaunchedEffect(currentDestination?.route) {
        currentDestination?.route?.let { Logger.d("Navigation", "Destination changed; route=$it") }
    }
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(connectionState) {
        if (connectionState is ConnectionState.Failed) {
            snackbar.showSnackbar(connectionState.message)
        }
    }
    val externalChallenge = remember(authChallenge) {
        authChallenge?.let { raw ->
            runCatching { JSONObject(raw) }.getOrNull()
                ?.takeIf { it.optString("type") == "externalLogin" }
        }
    }
    LaunchedEffect(externalChallenge, currentDestination?.route) {
        if (externalChallenge != null && currentDestination?.route != SSO_ROUTE) {
            navController.navigate(SSO_ROUTE) { launchSingleTop = true }
        }
    }
    authChallenge?.takeUnless { externalChallenge != null }?.let {
        AuthChallengeDialog(it, profile, onSubmitAuth, onCancelAuth)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (currentDestination?.route == SSO_ROUTE || currentDestination?.route == WIZARD_ROUTE) {
                        Text(if (currentDestination?.route == SSO_ROUTE) "SSO 登录" else "配置向导", fontWeight = FontWeight.SemiBold)
                    } else {
                        Column {
                            Text("Distrust", fontWeight = FontWeight.SemiBold)
                            Text(
                                "更透明的校园网络连接",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                navigationIcon = {
                    if (currentDestination?.route == SSO_ROUTE || currentDestination?.route == WIZARD_ROUTE) {
                        IconButton(onClick = {
                            if (currentDestination?.route == SSO_ROUTE) onCancelAuth()
                            else onResetAuthDiscovery()
                            navController.popBackStack()
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (currentDestination?.route != SSO_ROUTE && currentDestination?.route != WIZARD_ROUTE) NavigationBar {
                Destination.entries.forEach { item ->
                    NavigationBarItem(
                        selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(item.icon, contentDescription = null) },
                        label = { Text(item.label) },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Destination.HOME.route,
            modifier = Modifier.padding(padding),
        ) {
            composable(Destination.HOME.route) {
                HomeScreen(
                    profile,
                    connectionState,
                    onConnect,
                    onDisconnect,
                    onOpenWizard = {
                        onResetAuthDiscovery()
                        navController.navigate(WIZARD_ROUTE) { launchSingleTop = true }
                    },
                    Modifier,
                )
            }
            composable(Destination.PROFILE.route) {
                ProfileScreen(
                    profile,
                    onSaveProfile,
                    Modifier,
                )
            }
            composable(Destination.LOGS.route) {
                LogScreen(logs, onClearLogs, Modifier)
            }
            composable(Destination.ABOUT.route) {
                AboutScreen(Modifier)
            }
            composable(SSO_ROUTE) {
                val payload = externalChallenge?.optJSONObject("payload")
                SsoLoginScreen(
                    loginUrl = payload?.optString("loginUrl").orEmpty(),
                    profile = profile,
                    onCallback = { callback ->
                        onSubmitAuth(JSONObject().put("CallbackURL", callback).toString())
                        navController.popBackStack()
                    },
                    onCancel = {
                        onCancelAuth()
                        navController.popBackStack()
                    },
                )
            }
            composable(WIZARD_ROUTE) {
                ConfigurationWizardScreen(
                    initial = profile,
                    authDiscovery = authDiscovery,
                    onFetchAuthMethods = onFetchAuthMethods,
                    onResetAuthDiscovery = onResetAuthDiscovery,
                    onFinish = { configured ->
                        onSaveProfile(configured)
                        onResetAuthDiscovery()
                        navController.popBackStack()
                    },
                )
            }
        }
    }
}

@Composable
private fun AuthChallengeDialog(
    challengeJson: String,
    profile: ConnectionProfile,
    onSubmit: (String) -> Unit,
    onCancel: () -> Unit,
) {
    val challenge = remember(challengeJson) { runCatching { JSONObject(challengeJson) }.getOrNull() }
    val type = challenge?.optString("type").orEmpty()
    val payload = challenge?.optJSONObject("payload")
    var value by remember(challengeJson) { mutableStateOf("") }
    var skipSecondary by remember(challengeJson) { mutableStateOf(false) }
    val title = when (type) {
        "code" -> when (payload?.optString("kind")) {
            "sms" -> "短信验证码"
            "totp" -> "TOTP 验证码"
            "radius" -> "RADIUS 动态口令"
            else -> "认证验证码"
        }
        "textCaptcha" -> "图形验证码"
        "externalLogin" -> "浏览器认证"
        "clickCaptcha" -> "点选验证码"
        else -> "需要继续认证"
    }
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                payload?.optString("message")?.takeIf(String::isNotBlank)?.let { Text(it) }
                payload?.optString("imageBase64")?.takeIf(String::isNotBlank)?.let { encoded ->
                    runCatching {
                        val bytes = Base64.decode(encoded, Base64.DEFAULT)
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size).asImageBitmap()
                    }.getOrNull()?.let { image ->
                        Image(
                            bitmap = image,
                            contentDescription = "验证码图片",
                            modifier = Modifier.fillMaxWidth().height(180.dp),
                            contentScale = ContentScale.Fit,
                        )
                    }
                }
                if (type == "clickCaptcha") {
                    Text("点选验证码画布将在下一里程碑实现。", color = MaterialTheme.colorScheme.error)
                } else {
                    OutlinedTextField(
                        value = value,
                        onValueChange = { value = it },
                        label = { Text("认证响应") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = type != "externalLogin",
                    )
                }
                if (payload?.optBoolean("canSkipSecondaryAuth") == true) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(skipSecondary, { skipSecondary = it })
                        Text("跳过后续二次认证")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = value.isNotBlank() && type != "clickCaptcha",
                onClick = {
                    val response = when (type) {
                        "code" -> JSONObject().put("Code", value).put("SkipSecondaryAuth", skipSecondary)
                        else -> JSONObject().put("Code", value)
                    }
                    onSubmit(response.toString())
                },
            ) { Text("提交") }
        },
        dismissButton = { TextButton(onClick = onCancel) { Text("取消") } },
    )
}

@Composable
private fun HomeScreen(
    profile: ConnectionProfile,
    state: ConnectionState,
    onConnect: (ConnectionProfile) -> Unit,
    onDisconnect: () -> Unit,
    onOpenWizard: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        StatusCard(profile, state)
        ModeCard(profile)
        FilledTonalButton(onClick = onOpenWizard, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Settings, null)
            Spacer(Modifier.size(8.dp))
            Text("配置向导")
        }
        ProxyEndpointsCard(profile)

        val active = state is ConnectionState.Connecting || state is ConnectionState.Connected
        Button(
            onClick = { if (active) onDisconnect() else onConnect(profile) },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            contentPadding = PaddingValues(horizontal = 24.dp),
        ) {
            Icon(if (active) Icons.Default.Stop else Icons.Default.Shield, contentDescription = null)
            Spacer(Modifier.size(8.dp))
            Text(if (active) "断开连接" else "开始连接")
        }
        Text(
            if (profile.mode == ConnectionMode.LOCAL_PROXY) {
                "本地代理模式不会占用 Android 的 VPN 槽位，可作为 Clash/Mihomo 的上游代理。"
            } else {
                "VPN 模式会申请系统 VPN 权限，同一用户空间不能同时运行其他 VpnService。"
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun StatusCard(profile: ConnectionProfile, state: ConnectionState) {
    val (title, detail, color) = when (state) {
        ConnectionState.Disconnected -> Triple("尚未连接", "${profile.protocol.label} · ${profile.server}", MaterialTheme.colorScheme.outline)
        is ConnectionState.Connecting -> Triple("正在连接", "正在准备 ${state.mode.label}", MaterialTheme.colorScheme.tertiary)
        is ConnectionState.Connected -> Triple("已连接", state.endpoint, Color(0xFF2E7D32))
        ConnectionState.Disconnecting -> Triple("正在断开", "正在释放网络资源", MaterialTheme.colorScheme.tertiary)
        is ConnectionState.Failed -> Triple("连接失败", state.message, MaterialTheme.colorScheme.error)
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(Modifier.size(14.dp), contentAlignment = Alignment.Center) {
                Card(
                    modifier = Modifier.size(14.dp),
                    shape = CircleShape,
                    colors = CardDefaults.cardColors(containerColor = color),
                ) {}
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text(detail, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun ModeCard(profile: ConnectionProfile) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        ListItem(
            headlineContent = { Text(profile.mode.label) },
            supportingContent = {
                Text(if (profile.mode == ConnectionMode.LOCAL_PROXY) "兼容二级代理，不申请 VpnService" else "由 Distrust 接管系统流量")
            },
            leadingContent = { Icon(if (profile.mode == ConnectionMode.LOCAL_PROXY) Icons.Default.Lan else Icons.Default.Shield, null) },
        )
        HorizontalDivider()
        ListItem(
            headlineContent = { Text(profile.name) },
            supportingContent = { Text("${profile.protocol.label} · ${profile.server}:${profile.port}") },
            leadingContent = { Icon(Icons.Default.Key, null) },
        )
    }
}

@Composable
private fun ProxyEndpointsCard(profile: ConnectionProfile) {
    val clipboard = LocalClipboardManager.current
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("本地代理端口", style = MaterialTheme.typography.titleMedium)
            Text("SOCKS5  ·  127.0.0.1:${profile.socksPort}")
            Text("HTTP     ·  127.0.0.1:${profile.httpPort}")
            Text(
                "本地代理不会占用 VpnService；请在 Clash/Mihomo 中排除 Distrust 应用以避免回环。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FilledTonalButton(
                onClick = { clipboard.setText(AnnotatedString(ProxyConfigFormatter.mihomo(profile))) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.ContentCopy, null)
                Spacer(Modifier.size(8.dp))
                Text("复制 Mihomo 上游配置")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileScreen(
    storedProfile: ConnectionProfile,
    onSave: (ConnectionProfile) -> Unit,
    modifier: Modifier = Modifier,
) {
    var draft by remember(storedProfile) { mutableStateOf(storedProfile) }
    val issues = ProfileValidator.validate(draft)
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("连接配置", style = MaterialTheme.typography.headlineSmall)
        Text("运行模式", style = MaterialTheme.typography.titleMedium)
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            ConnectionMode.entries.forEachIndexed { index, mode ->
                SegmentedButton(
                    selected = draft.mode == mode,
                    onClick = { draft = draft.copy(mode = mode) },
                    shape = SegmentedButtonDefaults.itemShape(index, ConnectionMode.entries.size),
                ) { Text(mode.shortLabel) }
            }
        }
        Text("VPN 协议", style = MaterialTheme.typography.titleMedium)
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            VpnProtocol.entries.forEachIndexed { index, protocol ->
                SegmentedButton(
                    selected = draft.protocol == protocol,
                    onClick = { draft = draft.copy(protocol = protocol) },
                    shape = SegmentedButtonDefaults.itemShape(index, VpnProtocol.entries.size),
                ) { Text(protocol.label) }
            }
        }
        OutlinedTextField(draft.server, { draft = draft.copy(server = it) }, label = { Text("服务器") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(draft.port.toString(), { it.toIntOrNull()?.let { port -> draft = draft.copy(port = port.coerceIn(1, 65535)) } }, label = { Text("端口") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(draft.username, { draft = draft.copy(username = it) }, label = { Text("账号") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(draft.password, { draft = draft.copy(password = it) }, label = { Text("密码") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), singleLine = true)
        if (draft.protocol == VpnProtocol.ATRUST) {
            OutlinedTextField(draft.loginDomain, { draft = draft.copy(loginDomain = it) }, label = { Text("登录域") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(draft.authType, { draft = draft.copy(authType = it) }, label = { Text("认证类型") }, supportingText = { Text("例如 cas、psw、smsCheckCode") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(draft.socksPort.toString(), { it.toIntOrNull()?.let { port -> draft = draft.copy(socksPort = port.coerceIn(1, 65535)) } }, label = { Text("SOCKS5") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), singleLine = true)
            OutlinedTextField(draft.httpPort.toString(), { it.toIntOrNull()?.let { port -> draft = draft.copy(httpPort = port.coerceIn(1, 65535)) } }, label = { Text("HTTP") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), singleLine = true)
        }
        OutlinedTextField(draft.routes.joinToString("\n"), { value -> draft = draft.copy(routes = value.lines().map(String::trim).filter(String::isNotEmpty)) }, label = { Text("分流网段") }, supportingText = { Text("每行一个 CIDR，例如 10.0.0.0/8") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
        OutlinedTextField(draft.dnsServers.joinToString("\n"), { value -> draft = draft.copy(dnsServers = value.lines().map(String::trim).filter(String::isNotEmpty)) }, label = { Text("DNS 服务器") }, supportingText = { Text("每行一个 IP") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
        Text("DNS 与服务端策略", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(draft.dnsTtl.toString(), { it.toIntOrNull()?.let { value -> draft = draft.copy(dnsTtl = value.coerceAtLeast(1)) } }, label = { Text("DNS 缓存时间（秒）") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), singleLine = true)
        SettingSwitch("代理全部流量", "忽略服务端分流边界，将所有请求送入校园 VPN", draft.proxyAll) { draft = draft.copy(proxyAll = it) }
        SettingSwitch("忽略服务器配置", "不使用服务端下发的资源与分流策略", draft.disableServerConfig) { draft = draft.copy(disableServerConfig = it) }
        Text("aTrust 会话", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(draft.updateBestNodesInterval.toString(), { it.toIntOrNull()?.let { value -> draft = draft.copy(updateBestNodesInterval = value.coerceAtLeast(0)) } }, label = { Text("节点优选间隔（秒）") }, supportingText = { Text("0 表示禁用") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(draft.sessionRefreshInterval.toString(), { it.toIntOrNull()?.let { value -> draft = draft.copy(sessionRefreshInterval = value.coerceAtLeast(0)) } }, label = { Text("会话刷新间隔（秒）") }, supportingText = { Text("0 表示禁用") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), singleLine = true)
        if (issues.isNotEmpty()) {
            Text(
                issues.first().message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        FilledTonalButton(
            onClick = { if (issues.isEmpty()) onSave(draft) },
            enabled = issues.isEmpty(),
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) {
            Icon(Icons.Default.Save, null)
            Spacer(Modifier.size(8.dp))
            Text("保存配置")
        }
    }
}

@Composable
private fun SettingSwitch(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    OutlinedCard(Modifier.fillMaxWidth().clickable { onCheckedChange(!checked) }) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

@Composable
private fun LogScreen(
    logs: List<LogEntry>,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val formatter = remember { DateTimeFormatter.ofPattern("HH:mm:ss") }
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text("运行日志", style = MaterialTheme.typography.headlineSmall)
                Text("仅保留最近 500 条，敏感字段会自动脱敏", style = MaterialTheme.typography.bodySmall)
            }
            FilledTonalButton(onClick = onClear) {
                Icon(Icons.Default.DeleteSweep, null)
                Spacer(Modifier.size(6.dp))
                Text("清空")
            }
        }
        if (logs.isEmpty()) {
            Text("暂无日志", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        logs.asReversed().forEach { entry ->
            val color = when (entry.level) {
                LogLevel.VERBOSE, LogLevel.DEBUG -> MaterialTheme.colorScheme.onSurfaceVariant
                LogLevel.INFO -> MaterialTheme.colorScheme.onSurface
                LogLevel.WARNING -> MaterialTheme.colorScheme.tertiary
                LogLevel.ERROR, LogLevel.ASSERT -> MaterialTheme.colorScheme.error
            }
            OutlinedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "${entry.timestamp.atZone(ZoneId.systemDefault()).format(formatter)} · ${entry.level} · ${entry.tag}",
                        style = MaterialTheme.typography.labelSmall,
                        color = color,
                    )
                    Text(entry.message, style = MaterialTheme.typography.bodyMedium)
                    entry.throwable?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
private fun AboutScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(Icons.Default.Shield, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
        Text("Distrust", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
        Text("EZ4Connect 的原生 Android 客户端", style = MaterialTheme.typography.titleMedium)
        Text("已集成固定版本的 DistrustCore：支持 EasyConnect、aTrust 密码认证、系统 VPN 以及不占用 VpnService 的 SOCKS5/HTTP 本地代理。交互式认证将在后续里程碑接入。")
        HorizontalDivider()
        Text("包名  idont.trust.atrust", style = MaterialTheme.typography.bodyMedium)
        Text("许可证  GPL-3.0 / 上游核心 AGPL-3.0", style = MaterialTheme.typography.bodyMedium)
    }
}

private val VpnProtocol.label: String
    get() = if (this == VpnProtocol.ATRUST) "aTrust" else "EasyConnect"

private val ConnectionMode.label: String
    get() = if (this == ConnectionMode.LOCAL_PROXY) "本地代理模式" else "系统 VPN 模式"

private val ConnectionMode.shortLabel: String
    get() = if (this == ConnectionMode.LOCAL_PROXY) "本地代理" else "系统 VPN"

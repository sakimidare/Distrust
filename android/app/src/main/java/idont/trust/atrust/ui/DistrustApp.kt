package idont.trust.atrust.ui

import androidx.compose.animation.AnimatedContent
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
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import idont.trust.atrust.model.ConnectionMode
import idont.trust.atrust.model.ConnectionProfile
import idont.trust.atrust.model.ProfileValidator
import idont.trust.atrust.model.VpnProtocol
import idont.trust.atrust.service.ConnectionState
import idont.trust.atrust.service.LogEntry
import idont.trust.atrust.service.LogLevel
import idont.trust.atrust.util.ProxyConfigFormatter
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private enum class Destination(val label: String, val icon: ImageVector) {
    HOME("连接", Icons.Default.Home),
    PROFILE("配置", Icons.Default.Settings),
    LOGS("日志", Icons.Default.ReceiptLong),
    ABOUT("关于", Icons.Default.Info),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DistrustApp(
    profile: ConnectionProfile,
    connectionState: ConnectionState,
    logs: List<LogEntry>,
    onSaveProfile: (ConnectionProfile) -> Unit,
    onClearLogs: () -> Unit,
    onConnect: (ConnectionProfile) -> Unit,
    onDisconnect: () -> Unit,
) {
    var destination by remember { mutableStateOf(Destination.HOME) }
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(connectionState) {
        if (connectionState is ConnectionState.Failed) {
            snackbar.showSnackbar(connectionState.message)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Distrust", fontWeight = FontWeight.SemiBold)
                        Text(
                            "更透明的校园网络连接",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            NavigationBar {
                Destination.entries.forEach { item ->
                    NavigationBarItem(
                        selected = destination == item,
                        onClick = { destination = item },
                        icon = { Icon(item.icon, contentDescription = null) },
                        label = { Text(item.label) },
                    )
                }
            }
        },
    ) { padding ->
        AnimatedContent(destination, label = "main destination") { selected ->
            when (selected) {
                Destination.HOME -> HomeScreen(
                    profile,
                    connectionState,
                    onConnect,
                    onDisconnect,
                    Modifier.padding(padding),
                )
                Destination.PROFILE -> ProfileScreen(
                    profile,
                    onSaveProfile,
                    Modifier.padding(padding),
                )
                Destination.LOGS -> LogScreen(logs, onClearLogs, Modifier.padding(padding))
                Destination.ABOUT -> AboutScreen(Modifier.padding(padding))
            }
        }
    }
}

@Composable
private fun HomeScreen(
    profile: ConnectionProfile,
    state: ConnectionState,
    onConnect: (ConnectionProfile) -> Unit,
    onDisconnect: () -> Unit,
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
                "需要扩展上游 Go 移动接口后才能实际监听端口。",
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
                LogLevel.INFO -> MaterialTheme.colorScheme.onSurface
                LogLevel.WARNING -> MaterialTheme.colorScheme.tertiary
                LogLevel.ERROR -> MaterialTheme.colorScheme.error
            }
            OutlinedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "${entry.timestamp.atZone(ZoneId.systemDefault()).format(formatter)} · ${entry.level}",
                        style = MaterialTheme.typography.labelSmall,
                        color = color,
                    )
                    Text(entry.message, style = MaterialTheme.typography.bodyMedium)
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
        Text("当前里程碑已建立 Compose、配置安全存储、VpnService 和本地代理服务边界。aTrust、认证回调及 SOCKS5/HTTP 核心能力将在后续里程碑接入。")
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

package idont.trust.atrust.ui

import android.content.res.Configuration
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.twotone.ContentCopy
import androidx.compose.material.icons.twotone.Edit
import androidx.compose.material.icons.twotone.Home
import androidx.compose.material.icons.twotone.Info
import androidx.compose.material.icons.twotone.Key
import androidx.compose.material.icons.twotone.Lan
import androidx.compose.material.icons.twotone.Error
import androidx.compose.material.icons.twotone.LinkOff
import androidx.compose.material.icons.twotone.Settings
import androidx.compose.material.icons.twotone.Shield
import androidx.compose.material.icons.twotone.Sync
import androidx.compose.material.icons.twotone.TaskAlt
import androidx.compose.material.icons.twotone.Tune
import androidx.compose.material.icons.twotone.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FlexibleBottomAppBar
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.WideNavigationRail
import androidx.compose.material3.WideNavigationRailColors
import androidx.compose.material3.WideNavigationRailDefaults
import androidx.compose.material3.WideNavigationRailItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import idont.trust.atrust.logging.LogEntry
import idont.trust.atrust.logging.LogLevel
import idont.trust.atrust.logging.Logger
import idont.trust.atrust.model.ConnectionMode
import idont.trust.atrust.model.ConnectionProfile
import idont.trust.atrust.model.ProfileValidator
import idont.trust.atrust.model.VpnProtocol
import idont.trust.atrust.service.ConnectionState
import idont.trust.atrust.ui.component.SectionTextField
import idont.trust.atrust.ui.component.SegmentedColumn
import idont.trust.atrust.ui.component.SettingsBaseWidget
import idont.trust.atrust.ui.component.SettingsSwitchWidget
import idont.trust.atrust.ui.component.SegmentedControlWidget
import idont.trust.atrust.ui.component.SettingsJumpPageWidget
import idont.trust.atrust.ui.theme.ThemeConfig
import idont.trust.atrust.util.ProxyConfigFormatter
import java.time.ZoneId
import java.time.Instant
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch
import org.json.JSONObject
import top.yukonga.miuix.kmp.nav.core.NavCornerClipMode
import top.yukonga.miuix.kmp.nav.core.NavDisplay
import top.yukonga.miuix.kmp.nav.core.NavDisplayEffects
import top.yukonga.miuix.kmp.nav.core.rememberNavBackStack
import top.yukonga.miuix.kmp.nav.transition.NavSwipeDirection
import top.yukonga.miuix.kmp.nav.transition.NavTransitions
import idont.trust.atrust.ui.theme.DistrustTheme
import idont.trust.atrust.ui.navigation.AppRoute

private enum class Destination(val label: String, val icon: ImageVector) {
    HOME("连接", Icons.TwoTone.Home),
    PROFILE("配置", Icons.TwoTone.Settings),
    LOGS("日志", Icons.AutoMirrored.Rounded.ReceiptLong),
    ABOUT("关于", Icons.TwoTone.Info),
}

private val ConnectedContainerLight = Color(0xFFD1F4D1)
private val ConnectedContainerDark = Color(0xFF193C20)

@Composable
fun DistrustApp(
    profile: ConnectionProfile,
    connectionState: ConnectionState,
    logs: List<LogEntry>,
    authChallenge: String?,
    authDiscovery: AuthDiscoveryState,
    onSaveProfile: (ConnectionProfile) -> Unit,
    onClearLogs: () -> Unit,
    onClearSession: () -> Unit,
    onSubmitAuth: (String) -> Unit,
    onCancelAuth: () -> Unit,
    onFetchAuthMethods: (String, Int) -> Unit,
    onResetAuthDiscovery: () -> Unit,
    onConnect: (ConnectionProfile) -> Unit,
    onDisconnect: () -> Unit,
) {
    val backStack = rememberNavBackStack<AppRoute>(AppRoute.Main)
    val currentRoute = backStack.lastOrNull()
    val pop: () -> Unit = { if (backStack.size > 1) backStack.removeLastOrNull() }
    val push: (AppRoute) -> Unit = { route -> if (backStack.lastOrNull() != route) backStack.add(route) }
    val snackbar = remember { SnackbarHostState() }
    val externalChallenge = remember(authChallenge) {
        authChallenge?.let { runCatching { JSONObject(it) }.getOrNull() }
            ?.takeIf { it.optString("type") == "externalLogin" }
    }
    LaunchedEffect(connectionState) {
        if (connectionState is ConnectionState.Failed) snackbar.showSnackbar(connectionState.message)
    }
    LaunchedEffect(externalChallenge, currentRoute) {
        if (externalChallenge != null && currentRoute != AppRoute.Sso) push(AppRoute.Sso)
    }
    authChallenge?.takeUnless { externalChallenge != null }?.let {
        AuthChallengeDialog(it, onSubmitAuth, onCancelAuth)
    }

    val swipeDirection = if (LocalLayoutDirection.current == androidx.compose.ui.unit.LayoutDirection.Ltr) {
        NavSwipeDirection.LeftToRight
    } else NavSwipeDirection.RightToLeft
    NavDisplay(
        backStack = backStack,
        onBack = pop,
        transition = NavTransitions.MiuixDefault,
        effects = NavDisplayEffects(
            enableCornerClip = true,
            cornerClipRadius = 32.dp,
            cornerClipMode = NavCornerClipMode.All,
            dimAmount = 0.5f,
            backdropColor = MaterialTheme.colorScheme.surfaceContainer,
            blockInputDuringTransition = false,
        ),
    ) {
        entry<AppRoute.Main>(swipeDismiss = NavSwipeDirection.None) {
            MainShell(
                profile, connectionState, logs, snackbar,
                onConnect, onDisconnect, onClearLogs, onSaveProfile,
                onNavigate = push,
                onOpenWizard = {
                    onResetAuthDiscovery()
                    push(AppRoute.Wizard)
                },
            )
        }
        entry<AppRoute.Wizard>(swipeDismiss = swipeDirection) {
            SubPage(title = "配置向导", onBack = {
                onResetAuthDiscovery()
                pop()
            }) {
                ConfigurationWizardScreen(
                    initial = profile,
                    authDiscovery = authDiscovery,
                    onFetchAuthMethods = onFetchAuthMethods,
                    onResetAuthDiscovery = onResetAuthDiscovery,
                    onFinish = {
                        onSaveProfile(it)
                        onResetAuthDiscovery()
                        pop()
                    },
                )
            }
        }
        entry<AppRoute.Sso>(swipeDismiss = swipeDirection) {
            val payload = externalChallenge?.optJSONObject("payload")
            SubPage(title = "SSO 登录", onBack = {
                onCancelAuth()
                pop()
            }) {
                SsoLoginScreen(
                    loginUrl = payload?.optString("loginUrl").orEmpty(),
                    profile = profile,
                    onCallback = {
                        onSubmitAuth(JSONObject().put("CallbackURL", it).toString())
                        pop()
                    },
                    onCancel = {
                        onCancelAuth()
                        pop()
                    },
                )
            }
        }
        entry<AppRoute.ConnectionSettings>(swipeDismiss = swipeDirection) {
            ConnectionSettingsPage(profile, onSaveProfile, pop)
        }
        entry<AppRoute.ProxySettings>(swipeDismiss = swipeDirection) {
            ProxySettingsPage(profile, onSaveProfile, pop)
        }
        entry<AppRoute.PolicySettings>(swipeDismiss = swipeDirection) {
            PolicySettingsPage(profile, onSaveProfile, pop)
        }
        entry<AppRoute.SessionSettings>(swipeDismiss = swipeDirection) {
            SessionSettingsPage(profile, onSaveProfile, onClearSession, pop)
        }
    }
}

@Composable
private fun MainShell(
    profile: ConnectionProfile,
    state: ConnectionState,
    logs: List<LogEntry>,
    snackbar: SnackbarHostState,
    onConnect: (ConnectionProfile) -> Unit,
    onDisconnect: () -> Unit,
    onClearLogs: () -> Unit,
    onSaveProfile: (ConnectionProfile) -> Unit,
    onNavigate: (AppRoute) -> Unit,
    onOpenWizard: () -> Unit,
) {
    val destinations = Destination.entries
    val pager = rememberPagerState { destinations.size }
    val scope = rememberCoroutineScope()
    val selectPage: (Int) -> Unit = { index ->
        scope.launch {
            pager.animateScrollToPage(index, animationSpec = spring(dampingRatio = 0.72f, stiffness = 420f))
        }
    }
    BackHandler(pager.currentPage != 0) { selectPage(0) }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val portrait = maxHeight > maxWidth
        val pages: @Composable (Dp) -> Unit = { bottomPadding ->
            HorizontalPager(
                state = pager,
                beyondViewportPageCount = 1,
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                when (destinations[page]) {
                    Destination.HOME -> HomePage(profile, state, onConnect, onDisconnect, onSaveProfile, onNavigate, onOpenWizard, bottomPadding)
                    Destination.PROFILE -> ProfilePage(profile, onNavigate, onOpenWizard, bottomPadding)
                    Destination.LOGS -> LogPage(logs, onClearLogs, bottomPadding)
                    Destination.ABOUT -> AboutPage(bottomPadding)
                }
            }
        }
        if (portrait) {
            Scaffold(
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                snackbarHost = { SnackbarHost(snackbar) },
                bottomBar = {
                    FlexibleBottomAppBar(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = ThemeConfig.cardAlpha),
                    ) {
                        destinations.forEachIndexed { index, destination ->
                            NavigationBarItem(
                                selected = pager.currentPage == index,
                                onClick = { selectPage(index) },
                                icon = { Icon(destination.icon, destination.label) },
                                label = { Text(destination.label) },
                                alwaysShowLabel = false,
                            )
                        }
                    }
                },
            ) { padding -> pages(padding.calculateBottomPadding()) }
        } else {
            Row(Modifier.fillMaxSize()) {
                WideNavigationRail(
                    colors = WideNavigationRailColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        modalContainerColor = WideNavigationRailDefaults.colors().modalContainerColor,
                        modalScrimColor = WideNavigationRailDefaults.colors().modalScrimColor,
                        modalContentColor = WideNavigationRailDefaults.colors().modalContentColor,
                    ),
                ) {
                    destinations.forEachIndexed { index, destination ->
                        WideNavigationRailItem(
                            railExpanded = false,
                            selected = pager.currentPage == index,
                            onClick = { selectPage(index) },
                            icon = { Icon(destination.icon, destination.label) },
                            label = { Text(destination.label) },
                        )
                    }
                }
                Box(Modifier.weight(1f)) { pages(0.dp) }
            }
        }
    }
}

@Composable
private fun PageScaffold(
    title: String,
    bottomPadding: Dp,
    actions: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {},
    content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    AnimatedVisibility(true, enter = fadeIn(), exit = fadeOut()) {
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
            topBar = {
                LargeFlexibleTopAppBar(
                    title = { Text(title) },
                    actions = actions,
                    scrollBehavior = scrollBehavior,
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0f),
                        scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = ThemeConfig.cardAlpha),
                    ),
                )
            },
        ) { padding ->
            LazyColumn(
                contentPadding = PaddingValues(bottom = bottomPadding + 16.dp),
                modifier = Modifier.fillMaxSize().padding(padding).imePadding(),
                content = content,
            )
        }
    }
}

@Composable
private fun HomePage(
    profile: ConnectionProfile,
    state: ConnectionState,
    onConnect: (ConnectionProfile) -> Unit,
    onDisconnect: () -> Unit,
    onSaveProfile: (ConnectionProfile) -> Unit,
    onNavigate: (AppRoute) -> Unit,
    onOpenWizard: () -> Unit,
    bottomPadding: Dp,
) {
    var showModeDialog by remember { mutableStateOf(false) }
    var editingPort by remember { mutableStateOf<ProxyPort?>(null) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val scrollState = rememberScrollState()
    val active = state is ConnectionState.Connecting || state is ConnectionState.Connected
    val darkTheme = ThemeConfig.forceDarkMode ?: isSystemInDarkTheme()
    val statusTitle: String
    val statusDetail: String
    val statusIcon: ImageVector
    val statusContainer: Color?
    val statusError: Boolean
    when (state) {
        ConnectionState.Disconnected -> {
            statusTitle = "尚未连接"
            statusDetail = "点击连接以启用 ${profile.mode.label}"
            statusIcon = Icons.TwoTone.LinkOff
            statusContainer = null
            statusError = false
        }
        is ConnectionState.Connecting -> {
            statusTitle = "正在连接"
            statusDetail = "正在准备 ${state.mode.label}"
            statusIcon = Icons.TwoTone.Sync
            statusContainer = null
            statusError = false
        }
        is ConnectionState.Connected -> {
            statusTitle = "连接工作正常"
            statusDetail = state.endpoint
            statusIcon = Icons.TwoTone.TaskAlt
            statusContainer = if (darkTheme) ConnectedContainerDark else ConnectedContainerLight
            statusError = false
        }
        ConnectionState.Disconnecting -> {
            statusTitle = "正在断开"
            statusDetail = "正在释放网络资源"
            statusIcon = Icons.TwoTone.Sync
            statusContainer = null
            statusError = false
        }
        is ConnectionState.Failed -> {
            statusTitle = "连接失败"
            statusDetail = state.message
            statusIcon = Icons.TwoTone.Error
            statusContainer = MaterialTheme.colorScheme.errorContainer
            statusError = true
        }
    }
    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("Distrust") },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            )
        },
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
    ) { innerPadding ->
        Column(
            Modifier.fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .verticalScroll(scrollState)
                .padding(top = innerPadding.calculateTopPadding() + 2.dp, start = 16.dp, end = 16.dp),
        ) {
            SettingsBaseWidget(
                title = statusTitle,
                description = statusDetail,
                icon = statusIcon,
                iconSize = 18.dp,
                isError = statusError,
                containerColor = statusContainer,
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                FilledTonalButton(
                    onClick = onOpenWizard,
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(20.dp),
                ) {
                    Icon(Icons.TwoTone.Tune, null)
                    Spacer(Modifier.width(6.dp))
                    Text("配置向导")
                }
                Button(
                    onClick = { if (active) onDisconnect() else onConnect(profile) },
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = if (statusError) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error) else ButtonDefaults.buttonColors(),
                ) {
                    Icon(if (active) Icons.Rounded.Stop else Icons.TwoTone.Shield, null)
                    Spacer(Modifier.width(6.dp))
                    Text(if (active) "断开" else "连接")
                }
            }
            Spacer(Modifier.height(10.dp))
            SegmentedColumn("连接信息", contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp)) {
                item { SettingsBaseWidget(profile.mode.label, if (profile.mode == ConnectionMode.LOCAL_PROXY) "SOCKS5/HTTP 二级代理" else "接管系统选定流量", if (profile.mode == ConnectionMode.LOCAL_PROXY) Icons.TwoTone.Lan else Icons.TwoTone.Shield, onClick = { showModeDialog = true }, trailingContent = { Icon(Icons.TwoTone.Edit, "修改") }) }
                item { SettingsJumpPageWidget("协议与服务器", "${profile.protocol.label} · ${profile.server}:${profile.port}", Icons.TwoTone.Key) { onNavigate(AppRoute.ConnectionSettings) } }
                item(visible = state is ConnectionState.Connected) { SettingsBaseWidget("会话地址", (state as? ConnectionState.Connected)?.endpoint.orEmpty(), Icons.TwoTone.Shield) }
            }
            SegmentedColumn("本地代理", contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp)) {
                item { SettingsBaseWidget("SOCKS5", "127.0.0.1:${profile.socksPort}", Icons.TwoTone.Lan, onClick = { editingPort = ProxyPort.SOCKS5 }, trailingContent = { Icon(Icons.TwoTone.Edit, "修改") }) }
                item { SettingsBaseWidget("HTTP", "127.0.0.1:${profile.httpPort}", Icons.TwoTone.Lan, onClick = { editingPort = ProxyPort.HTTP }, trailingContent = { Icon(Icons.TwoTone.Edit, "修改") }) }
                item { ProxyCopyWidget(profile) }
            }
            Spacer(Modifier.height(bottomPadding + 16.dp))
        }
    }
    if (showModeDialog) {
        ModeSelectionDialog(
            selected = profile.mode,
            onDismiss = { showModeDialog = false },
            onSelect = { onSaveProfile(profile.copy(mode = it)); showModeDialog = false },
        )
    }
    editingPort?.let { port ->
        PortEditorDialog(
            type = port,
            current = if (port == ProxyPort.SOCKS5) profile.socksPort else profile.httpPort,
            onDismiss = { editingPort = null },
            onSave = { value ->
                onSaveProfile(if (port == ProxyPort.SOCKS5) profile.copy(socksPort = value) else profile.copy(httpPort = value))
                editingPort = null
            },
        )
    }
}

@Composable
private fun ProxyCopyWidget(profile: ConnectionProfile) {
    val clipboard = LocalClipboardManager.current
    SettingsBaseWidget("复制 Mihomo 配置", "作为二级代理的上游配置", Icons.TwoTone.ContentCopy, onClick = {
        clipboard.setText(AnnotatedString(ProxyConfigFormatter.mihomo(profile)))
    })
}

@Composable
private fun ProfilePage(
    stored: ConnectionProfile,
    onNavigate: (AppRoute) -> Unit,
    onOpenWizard: () -> Unit,
    bottomPadding: Dp,
) {
    PageScaffold("设置", bottomPadding) {
        item {
            SegmentedColumn("连接") {
                item { SettingsJumpPageWidget("服务器与认证", "${stored.protocol.label} · ${stored.server}:${stored.port}", Icons.TwoTone.Key) { onNavigate(AppRoute.ConnectionSettings) } }
                item { SettingsJumpPageWidget("本地代理", "${stored.mode.label} · SOCKS5 ${stored.socksPort} · HTTP ${stored.httpPort}", Icons.TwoTone.Lan) { onNavigate(AppRoute.ProxySettings) } }
                item { SettingsJumpPageWidget("配置向导", "重新发现认证方式并创建连接配置", Icons.TwoTone.Tune, onClick = onOpenWizard) }
            }
        }
        item {
            SegmentedColumn("高级") {
                item { SettingsJumpPageWidget("DNS 与分流", "资源策略、自定义 DNS 与路由", Icons.TwoTone.Shield) { onNavigate(AppRoute.PolicySettings) } }
                item { SettingsJumpPageWidget("aTrust 会话", "节点优选和会话刷新间隔", Icons.TwoTone.Settings) { onNavigate(AppRoute.SessionSettings) } }
            }
        }
    }
}

private enum class ProxyPort(val title: String) {
    SOCKS5("SOCKS5 端口"),
    HTTP("HTTP 端口"),
}

@Composable
private fun ModeSelectionDialog(
    selected: ConnectionMode,
    onDismiss: () -> Unit,
    onSelect: (ConnectionMode) -> Unit,
) {
    var choice by remember(selected) { mutableStateOf(selected) }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(32.dp),
        title = { Text("运行模式") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("选择 Distrust 如何向其他应用提供连接。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    ConnectionMode.entries.forEachIndexed { index, mode ->
                        SegmentedButton(
                            selected = choice == mode,
                            onClick = { choice = mode },
                            shape = SegmentedButtonDefaults.itemShape(index, ConnectionMode.entries.size),
                        ) { Text(mode.shortLabel) }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onSelect(choice) }) { Text("保存") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun PortEditorDialog(
    type: ProxyPort,
    current: Int,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit,
) {
    var value by remember(type, current) { mutableStateOf(current.toString()) }
    val port = value.toIntOrNull()
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(32.dp),
        title = { Text(type.title) },
        text = {
            SectionTextField(
                value = value,
                onValueChange = { value = it.filter(Char::isDigit).take(5) },
                label = "端口",
                supportingText = if (port == null || port !in 1..65535) "请输入 1–65535" else "监听地址 127.0.0.1:$port",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
        },
        confirmButton = { TextButton(enabled = port in 1..65535, onClick = { onSave(requireNotNull(port)) }) { Text("保存") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun EditorScaffold(
    title: String,
    saveEnabled: Boolean = true,
    onBack: () -> Unit,
    onSave: () -> Unit,
    content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(title) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "返回") } },
                actions = { IconButton(enabled = saveEnabled, onClick = onSave) { Icon(Icons.Rounded.Save, "保存") } },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).imePadding(),
            contentPadding = PaddingValues(bottom = 24.dp),
            content = content,
        )
    }
}

@Composable
private fun EditorFields(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

@Composable
private fun ConnectionSettingsPage(stored: ConnectionProfile, onSave: (ConnectionProfile) -> Unit, onBack: () -> Unit) {
    var draft by remember(stored) { mutableStateOf(stored) }
    val valid = draft.server.isNotBlank() && draft.port in 1..65535
    EditorScaffold("服务器与认证", valid, onBack, { onSave(draft); onBack() }) {
        item {
            SegmentedColumn("协议") {
                item {
                    SegmentedControlWidget("VPN 协议") {
                        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                            VpnProtocol.entries.forEachIndexed { index, protocol ->
                                SegmentedButton(draft.protocol == protocol, { draft = draft.copy(protocol = protocol) }, SegmentedButtonDefaults.itemShape(index, VpnProtocol.entries.size)) { Text(protocol.label) }
                            }
                        }
                    }
                }
            }
        }
        item {
            EditorFields {
                SectionTextField(draft.name, { draft = draft.copy(name = it) }, "配置名称")
                SectionTextField(draft.server, { draft = draft.copy(server = it) }, "服务器")
                SectionTextField(draft.port.toString(), { it.toIntOrNull()?.let { v -> draft = draft.copy(port = v.coerceIn(1, 65535)) } }, "端口", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            }
        }
        item {
            EditorFields {
                SectionTextField(draft.username, { draft = draft.copy(username = it) }, "账号")
                SectionTextField(draft.password, { draft = draft.copy(password = it) }, "密码", visualTransformation = PasswordVisualTransformation())
                if (draft.protocol == VpnProtocol.ATRUST) {
                    SectionTextField(draft.loginDomain, { draft = draft.copy(loginDomain = it) }, "登录域")
                    SectionTextField(draft.authType, { draft = draft.copy(authType = it) }, "认证类型", supportingText = "cas、psw、smsCheckCode")
                    SectionTextField(draft.phone, { draft = draft.copy(phone = it) }, "手机号码（可选）")
                    SectionTextField(draft.totpSecret, { draft = draft.copy(totpSecret = it) }, "TOTP 密钥（可选）", visualTransformation = PasswordVisualTransformation())
                }
            }
        }
    }
}

@Composable
private fun ProxySettingsPage(stored: ConnectionProfile, onSave: (ConnectionProfile) -> Unit, onBack: () -> Unit) {
    var draft by remember(stored) { mutableStateOf(stored) }
    EditorScaffold("本地代理", true, onBack, { onSave(draft); onBack() }) {
        item {
            SegmentedColumn("运行方式") {
                item {
                    SegmentedControlWidget("运行模式") {
                        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                            ConnectionMode.entries.forEachIndexed { index, mode ->
                                SegmentedButton(draft.mode == mode, { draft = draft.copy(mode = mode) }, SegmentedButtonDefaults.itemShape(index, ConnectionMode.entries.size)) { Text(mode.shortLabel) }
                            }
                        }
                    }
                }
            }
        }
        item {
            EditorFields {
                SectionTextField(draft.socksPort.toString(), { it.toIntOrNull()?.let { v -> draft = draft.copy(socksPort = v.coerceIn(1, 65535)) } }, "SOCKS5 端口", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                SectionTextField(draft.httpPort.toString(), { it.toIntOrNull()?.let { v -> draft = draft.copy(httpPort = v.coerceIn(1, 65535)) } }, "HTTP 端口", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            }
        }
    }
}

@Composable
private fun PolicySettingsPage(stored: ConnectionProfile, onSave: (ConnectionProfile) -> Unit, onBack: () -> Unit) {
    var draft by remember(stored) { mutableStateOf(stored) }
    EditorScaffold("DNS 与分流", true, onBack, { onSave(draft); onBack() }) {
        item {
            SegmentedColumn("策略") {
                item { SettingsSwitchWidget("代理全部流量", "忽略服务端分流边界", Icons.TwoTone.Shield, draft.proxyAll) { draft = draft.copy(proxyAll = it) } }
                item { SettingsSwitchWidget("忽略服务器配置", "仅使用本地配置", Icons.TwoTone.Settings, draft.disableServerConfig) { draft = draft.copy(disableServerConfig = it) } }
            }
        }
        item {
            EditorFields {
                SectionTextField(draft.routes.joinToString("\n"), { draft = draft.copy(routes = it.lines().map(String::trim).filter(String::isNotBlank)) }, "分流网段", singleLine = false, supportingText = "每行一个 CIDR")
                SectionTextField(draft.dnsServers.joinToString("\n"), { draft = draft.copy(dnsServers = it.lines().map(String::trim).filter(String::isNotBlank)) }, "DNS 服务器", singleLine = false)
                SectionTextField(draft.customDns.entries.joinToString("\n") { "${it.key}=${it.value}" }, { value ->
                    draft = draft.copy(customDns = value.lineSequence().mapNotNull { line -> line.split('=', limit = 2).takeIf { it.size == 2 }?.let { it[0].trim() to it[1].trim() } }.toMap())
                }, "自定义 DNS", singleLine = false, supportingText = "高级覆盖：域名=IP")
                SectionTextField(draft.dnsTtl.toString(), { it.toIntOrNull()?.let { v -> draft = draft.copy(dnsTtl = v.coerceAtLeast(1)) } }, "DNS 缓存时间（秒）", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            }
        }
    }
}

@Composable
private fun SessionSettingsPage(
    stored: ConnectionProfile,
    onSave: (ConnectionProfile) -> Unit,
    onClearSession: () -> Unit,
    onBack: () -> Unit,
) {
    var draft by remember(stored) { mutableStateOf(stored) }
    var confirmClear by remember { mutableStateOf(false) }
    EditorScaffold("aTrust 会话", true, onBack, { onSave(draft); onBack() }) {
        item {
            EditorFields {
                SectionTextField(draft.updateBestNodesInterval.toString(), { it.toIntOrNull()?.let { v -> draft = draft.copy(updateBestNodesInterval = v.coerceAtLeast(0)) } }, "节点优选间隔（秒）", supportingText = "0 表示禁用", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                SectionTextField(draft.sessionRefreshInterval.toString(), { it.toIntOrNull()?.let { v -> draft = draft.copy(sessionRefreshInterval = v.coerceAtLeast(0)) } }, "会话刷新间隔（秒）", supportingText = "0 表示禁用", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            }
        }
        item {
            SegmentedColumn("已保存会话") {
                item {
                    SettingsBaseWidget(
                        title = if (stored.clientData.isBlank()) "没有保存的会话" else "清除保存的会话",
                        description = if (stored.clientData.isBlank()) "下次连接需要重新认证" else "删除加密 Cookie，并停止当前连接",
                        icon = Icons.Rounded.DeleteSweep,
                        enabled = stored.clientData.isNotBlank(),
                        isError = stored.clientData.isNotBlank(),
                        onClick = if (stored.clientData.isNotBlank()) ({ confirmClear = true }) else null,
                    )
                }
            }
        }
    }
    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            shape = RoundedCornerShape(32.dp),
            title = { Text("清除 aTrust 会话？") },
            text = { Text("当前 VPN/代理会被停止，下次连接需要重新完成认证。") },
            confirmButton = {
                TextButton(onClick = {
                    confirmClear = false
                    onClearSession()
                    onBack()
                }) { Text("清除") }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("取消") } },
        )
    }
}

@Composable
private fun LogPage(logs: List<LogEntry>, onClear: () -> Unit, bottomPadding: Dp) {
    val formatter = remember { DateTimeFormatter.ofPattern("HH:mm:ss") }
    PageScaffold("运行日志", bottomPadding, actions = {
        IconButton(onClick = onClear) { Icon(Icons.Rounded.DeleteSweep, "清空日志") }
    }) {
        if (logs.isEmpty()) item { Text("暂无日志", Modifier.padding(24.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) }
        items(logs.asReversed(), key = { "${it.timestamp}-${it.tag}-${it.message.hashCode()}" }) { entry ->
            val color = when (entry.level) {
                LogLevel.VERBOSE, LogLevel.DEBUG -> MaterialTheme.colorScheme.onSurfaceVariant
                LogLevel.INFO -> MaterialTheme.colorScheme.primary
                LogLevel.WARNING -> MaterialTheme.colorScheme.tertiary
                LogLevel.ERROR, LogLevel.ASSERT -> MaterialTheme.colorScheme.error
            }
            Surface(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 3.dp),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = ThemeConfig.cardAlpha),
                contentColor = MaterialTheme.colorScheme.onSurface,
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("${entry.timestamp.atZone(ZoneId.systemDefault()).format(formatter)} · ${entry.level} · ${entry.tag}", style = MaterialTheme.typography.labelSmall, color = color)
                    Text(entry.message, style = MaterialTheme.typography.bodyMedium)
                    entry.throwable?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
                }
            }
        }
    }
}

@Composable
private fun AboutPage(bottomPadding: Dp) {
    PageScaffold("关于", bottomPadding) {
        item {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(88.dp)) {
                    Box(contentAlignment = Alignment.Center) { Icon(Icons.TwoTone.Shield, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary) }
                }
                Text("Distrust", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("透明、原生的校园网络连接", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            SegmentedColumn("应用") {
                item { SettingsBaseWidget("原生 Android 客户端", "aTrust / EasyConnect · VPN / SOCKS5 / HTTP", Icons.TwoTone.Info) }
                item { SettingsBaseWidget("包名", "idont.trust.atrust", Icons.TwoTone.Key) }
                item { SettingsBaseWidget("许可证", "GPL-3.0 / Core AGPL-3.0", Icons.TwoTone.Info) }
            }
        }
    }
}

@Composable
private fun SubPage(title: String, onBack: () -> Unit, content: @Composable () -> Unit) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "返回") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = ThemeConfig.cardAlpha),
                ),
            )
        },
    ) { padding -> Box(Modifier.fillMaxSize().padding(padding)) { content() } }
}

@Composable
private fun AuthChallengeDialog(challengeJson: String, onSubmit: (String) -> Unit, onCancel: () -> Unit) {
    val challenge = remember(challengeJson) { runCatching { JSONObject(challengeJson) }.getOrNull() }
    val type = challenge?.optString("type").orEmpty()
    val payload = challenge?.optJSONObject("payload")
    var value by remember(challengeJson) { mutableStateOf("") }
    var skipSecondary by remember(challengeJson) { mutableStateOf(false) }
    val title = when (payload?.optString("kind")) {
        "sms" -> "短信验证码"
        "totp" -> "TOTP 验证码"
        "radius" -> "RADIUS 动态口令"
        else -> if (type == "textCaptcha") "图形验证码" else "继续认证"
    }
    AlertDialog(
        onDismissRequest = onCancel,
        shape = RoundedCornerShape(32.dp),
        icon = { Icon(Icons.TwoTone.Shield, null) },
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                payload?.optString("message")?.takeIf(String::isNotBlank)?.let { Text(it) }
                payload?.optString("imageBase64")?.takeIf(String::isNotBlank)?.let { encoded ->
                    runCatching { Base64.decode(encoded, Base64.DEFAULT).let { BitmapFactory.decodeByteArray(it, 0, it.size).asImageBitmap() } }.getOrNull()?.let {
                        Image(it, "验证码", Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(20.dp)), contentScale = ContentScale.Fit)
                    }
                }
                SectionTextField(value, { value = it }, "认证响应")
                if (payload?.optBoolean("canSkipSecondaryAuth") == true) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(skipSecondary, { skipSecondary = it })
                        Text("跳过后续二次认证")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = value.isNotBlank(), onClick = {
                onSubmit(JSONObject().put("Code", value).put("SkipSecondaryAuth", skipSecondary).toString())
            }) { Text("提交") }
        },
        dismissButton = { TextButton(onClick = onCancel) { Text("取消") } },
    )
}

private val VpnProtocol.label get() = if (this == VpnProtocol.ATRUST) "aTrust" else "EasyConnect"
private val ConnectionMode.label get() = if (this == ConnectionMode.LOCAL_PROXY) "本地代理模式" else "系统 VPN 模式"
private val ConnectionMode.shortLabel get() = if (this == ConnectionMode.LOCAL_PROXY) "本地代理" else "系统 VPN"

private val previewProfile = ConnectionProfile(
    name = "东南大学",
    mode = ConnectionMode.VPN,
    protocol = VpnProtocol.ATRUST,
    server = "vpn.seu.edu.cn",
    username = "preview-user",
    loginDomain = "seucas",
    socksPort = 11080,
    httpPort = 11081,
)

private val previewLogs = listOf(
    LogEntry(Instant.now(), LogLevel.INFO, "VpnService", "VPN connected; address=10.85.4.117"),
    LogEntry(Instant.now().minusSeconds(1), LogLevel.DEBUG, "Policy", "Resource snapshot loaded"),
    LogEntry(Instant.now().minusSeconds(2), LogLevel.WARNING, "DNS", "Primary DNS timed out; using policy secondary"),
)

@Preview(name = "Home · disconnected", showSystemUi = true)
@Preview(name = "Home · disconnected dark", showSystemUi = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun HomeDisconnectedPreview() = DistrustTheme {
    HomePage(previewProfile, ConnectionState.Disconnected, {}, {}, {}, {}, {}, 0.dp)
}

@Preview(name = "Home · connected", showSystemUi = true)
@Composable
private fun HomeConnectedPreview() = DistrustTheme {
    HomePage(
        previewProfile,
        ConnectionState.Connected(ConnectionMode.VPN, "10.85.4.117"),
        {}, {}, {}, {}, {}, 0.dp,
    )
}

@Preview(name = "Home · error", showSystemUi = true)
@Composable
private fun HomeErrorPreview() = DistrustTheme {
    HomePage(previewProfile, ConnectionState.Failed("服务器会话已过期，请重新认证"), {}, {}, {}, {}, {}, 0.dp)
}

@Preview(name = "Profile page", showSystemUi = true)
@Composable
private fun ProfilePagePreview() = DistrustTheme {
    ProfilePage(previewProfile, {}, {}, 0.dp)
}

@Preview(name = "Connection settings", showSystemUi = true)
@Composable
private fun ConnectionSettingsPagePreview() = DistrustTheme {
    ConnectionSettingsPage(previewProfile, {}, {})
}

@Preview(name = "Proxy settings", showSystemUi = true)
@Composable
private fun ProxySettingsPagePreview() = DistrustTheme {
    ProxySettingsPage(previewProfile, {}, {})
}

@Preview(name = "Policy settings", showSystemUi = true)
@Composable
private fun PolicySettingsPagePreview() = DistrustTheme {
    PolicySettingsPage(previewProfile, {}, {})
}

@Preview(name = "Session settings", showSystemUi = true)
@Composable
private fun SessionSettingsPagePreview() = DistrustTheme {
    SessionSettingsPage(previewProfile, {}, {}, {})
}

@Preview(name = "Mode dialog", showSystemUi = true)
@Composable
private fun ModeSelectionDialogPreview() = DistrustTheme {
    ModeSelectionDialog(ConnectionMode.VPN, {}, {})
}

@Preview(name = "Port dialog", showSystemUi = true)
@Composable
private fun PortEditorDialogPreview() = DistrustTheme {
    PortEditorDialog(ProxyPort.SOCKS5, 11080, {}, {})
}

@Preview(name = "Log page", showSystemUi = true)
@Composable
private fun LogPagePreview() = DistrustTheme {
    LogPage(previewLogs, {}, 0.dp)
}

@Preview(name = "About page", showSystemUi = true)
@Composable
private fun AboutPagePreview() = DistrustTheme {
    AboutPage(0.dp)
}

@Preview(name = "Main shell", showSystemUi = true)
@Composable
private fun MainShellPreview() = DistrustTheme {
    MainShell(
        previewProfile,
        ConnectionState.Disconnected,
        previewLogs,
        remember { SnackbarHostState() },
        {}, {}, {}, {}, {}, {},
    )
}

@Preview(name = "Authentication dialog", showSystemUi = true)
@Composable
private fun AuthChallengeDialogPreview() = DistrustTheme {
    AuthChallengeDialog(
        JSONObject()
            .put("type", "code")
            .put("payload", JSONObject().put("kind", "sms").put("message", "请输入发送到手机的验证码"))
            .toString(),
        {}, {},
    )
}

package idont.trust.atrust.ui

import android.content.res.Configuration
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.twotone.ContentCopy
import androidx.compose.material.icons.twotone.Edit
import androidx.compose.material.icons.twotone.Error
import androidx.compose.material.icons.twotone.Home
import androidx.compose.material.icons.twotone.Info
import androidx.compose.material.icons.twotone.Key
import androidx.compose.material.icons.twotone.Lan
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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FlexibleBottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LoadingIndicator
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.core.graphics.drawable.toBitmap
import androidx.core.content.pm.PackageInfoCompat
import idont.trust.atrust.logging.LogEntry
import idont.trust.atrust.logging.LogLevel
import idont.trust.atrust.logging.Logger
import idont.trust.atrust.model.ConnectionMode
import idont.trust.atrust.model.ConnectionProfile
import idont.trust.atrust.model.AppRoutingMode
import idont.trust.atrust.model.VpnProtocol
import idont.trust.atrust.model.ServerScheme
import idont.trust.atrust.data.DnsHistoryStore
import idont.trust.atrust.service.ConnectionState
import idont.trust.atrust.service.SessionRuntime
import idont.trust.atrust.ui.component.SectionTextField
import idont.trust.atrust.ui.component.SegmentedColumn
import idont.trust.atrust.ui.component.SegmentedControlWidget
import idont.trust.atrust.ui.component.SettingsBaseWidget
import idont.trust.atrust.ui.component.SettingsJumpPageWidget
import idont.trust.atrust.ui.component.SettingsSwitchWidget
import idont.trust.atrust.ui.navigation.AppRoute
import idont.trust.atrust.ui.theme.DistrustTheme
import idont.trust.atrust.ui.theme.ThemeConfig
import idont.trust.atrust.util.ProxyConfigFormatter
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.json.JSONObject
import top.yukonga.miuix.kmp.nav.core.NavCornerClipMode
import top.yukonga.miuix.kmp.nav.core.NavDisplay
import top.yukonga.miuix.kmp.nav.core.NavDisplayEffects
import top.yukonga.miuix.kmp.nav.core.rememberNavBackStack
import top.yukonga.miuix.kmp.nav.transition.NavSwipeDirection
import top.yukonga.miuix.kmp.nav.transition.NavTransitions
import java.net.URI
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

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
    profiles: List<ConnectionProfile>,
    connectionState: ConnectionState,
    logs: List<LogEntry>,
    authChallenge: String?,
    authDiscovery: AuthDiscoveryState,
    onSaveProfile: (ConnectionProfile) -> Unit,
    onSwitchProfile: (String) -> Unit,
    onDuplicateProfile: () -> Unit,
    onCreateProfile: (String) -> Unit,
    onRenameProfile: (String, String) -> Unit,
    onDeleteProfile: (String) -> Unit,
    onClearLogs: () -> Unit,
    onExportLogs: (List<LogEntry>) -> Unit,
    onClearSession: () -> Unit,
    onFakeDnsSnapshot: () -> Result<Map<String, String>>,
    onClearFakeDns: () -> Result<Unit>,
    onSubmitAuth: (String) -> Unit,
    onCancelAuth: () -> Unit,
    onFetchAuthMethods: (String, Int, ServerScheme) -> Unit,
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
                profile, profiles, connectionState, logs, snackbar,
                onConnect, onDisconnect, onClearLogs, onExportLogs, onSaveProfile,
                onSwitchProfile, onDuplicateProfile, onCreateProfile, onRenameProfile, onDeleteProfile,
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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceContainer),
            ) {
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
        entry<AppRoute.AppRoutingSettings>(swipeDismiss = swipeDirection) {
            AppRoutingSettingsPage(profile, onSaveProfile, pop)
        }
        entry<AppRoute.DnsCacheSettings>(swipeDismiss = swipeDirection) {
            DnsCacheSettingsPage(profile, onFakeDnsSnapshot, onClearFakeDns, pop)
        }
    }
}

@Composable
private fun MainShell(
    profile: ConnectionProfile,
    profiles: List<ConnectionProfile>,
    state: ConnectionState,
    logs: List<LogEntry>,
    snackbar: SnackbarHostState,
    onConnect: (ConnectionProfile) -> Unit,
    onDisconnect: () -> Unit,
    onClearLogs: () -> Unit,
    onExportLogs: (List<LogEntry>) -> Unit,
    onSaveProfile: (ConnectionProfile) -> Unit,
    onSwitchProfile: (String) -> Unit,
    onDuplicateProfile: () -> Unit,
    onCreateProfile: (String) -> Unit,
    onRenameProfile: (String, String) -> Unit,
    onDeleteProfile: (String) -> Unit,
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
                    Destination.PROFILE -> ProfilePage(profile, profiles, onSaveProfile, onSwitchProfile, onDuplicateProfile, onCreateProfile, onRenameProfile, onDeleteProfile, onNavigate, onOpenWizard, bottomPadding)
                    Destination.LOGS -> LogPage(logs, onClearLogs, onExportLogs, bottomPadding)
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
    val health by SessionRuntime.health.collectAsStateWithLifecycle()
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
            statusTitle = "未连接"
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
            val healthWarning = health.consecutiveFailures > 0
            statusTitle = if (healthWarning) "连接质量波动" else "已连接"
            statusDetail = buildString {
                append(state.endpoint).append(" · ").append(health.detail)
                health.latencyMillis?.let { append(" · ${it}ms") }
                if (health.consecutiveFailures > 0) append(" · 连续 ${health.consecutiveFailures} 次异常")
            }
            statusIcon = if (healthWarning) Icons.TwoTone.Warning else Icons.TwoTone.TaskAlt
            statusContainer = if (healthWarning) MaterialTheme.colorScheme.tertiaryContainer else if (darkTheme) ConnectedContainerDark else ConnectedContainerLight
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
                trailingContent = if (state is ConnectionState.Connecting || state is ConnectionState.Disconnecting) {
                    { LoadingIndicator(Modifier.size(32.dp)) }
                } else null,
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
                    if (state is ConnectionState.Connecting || state is ConnectionState.Disconnecting) {
                        LoadingIndicator(Modifier.size(24.dp))
                    } else {
                        Icon(if (active) Icons.Rounded.Stop else Icons.TwoTone.Shield, null)
                    }
                    Spacer(Modifier.width(6.dp))
                    Text(if (active) "断开" else "连接")
                }
            }
            Spacer(Modifier.height(10.dp))
            SegmentedColumn("连接信息", contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp)) {
                item { SettingsBaseWidget("连接方式", profile.mode.label, if (profile.mode == ConnectionMode.LOCAL_PROXY) Icons.TwoTone.Lan else Icons.TwoTone.Shield, onClick = { showModeDialog = true }, trailingContent = { Icon(Icons.TwoTone.Edit, "修改") }) }
                item { SettingsJumpPageWidget("协议与服务器", "${profile.protocol.label} · ${profile.server}:${profile.port}", Icons.TwoTone.Key) { onNavigate(AppRoute.ConnectionSettings) } }
                item(visible = state is ConnectionState.Connected) { SettingsBaseWidget("会话地址", (state as? ConnectionState.Connected)?.endpoint.orEmpty(), Icons.TwoTone.Shield) }
            }
            if (profile.mode == ConnectionMode.LOCAL_PROXY) {
                SegmentedColumn("本地代理", contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp)) {
                    item { SettingsBaseWidget("SOCKS5", "127.0.0.1:${profile.socksPort}", Icons.TwoTone.Lan, onClick = { editingPort = ProxyPort.SOCKS5 }, trailingContent = { Icon(Icons.TwoTone.Edit, "修改") }) }
                    item { SettingsBaseWidget("HTTP", "127.0.0.1:${profile.httpPort}", Icons.TwoTone.Lan, onClick = { editingPort = ProxyPort.HTTP }, trailingContent = { Icon(Icons.TwoTone.Edit, "修改") }) }
                    item { ProxyCopyWidget(profile) }
                }
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
    profiles: List<ConnectionProfile>,
    onSave: (ConnectionProfile) -> Unit,
    onSwitch: (String) -> Unit,
    onDuplicate: () -> Unit,
    onCreate: (String) -> Unit,
    onRename: (String, String) -> Unit,
    onDelete: (String) -> Unit,
    onNavigate: (AppRoute) -> Unit,
    onOpenWizard: () -> Unit,
    bottomPadding: Dp,
) {
    val context = LocalContext.current
    var showProfiles by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var showCreate by remember { mutableStateOf(false) }
    var showRename by remember { mutableStateOf(false) }
    var profileName by remember(stored.id, stored.name) { mutableStateOf(stored.name) }
    val json = remember { Json { prettyPrint = true; ignoreUnknownKeys = true } }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        runCatching {
            val safeProfile = stored.copy(
                password = "",
                totpSecret = "",
                easyConnectTwfId = "",
                certificateBase64 = "",
                certificatePassword = "",
                socksPassword = "",
                clientData = "",
            )
            context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(json.encodeToString(safeProfile)) }
        }.onFailure { Logger.e("Profile", "Failed to export profile", it) }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        runCatching {
            val content = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                ?: error("配置文件读取失败")
            onSave(json.decodeFromString<ConnectionProfile>(content).copy(clientData = ""))
        }.onFailure { Logger.e("Profile", "Failed to import profile", it) }
    }
    PageScaffold("设置", bottomPadding) {
        item {
            SegmentedColumn("配置档案") {
                item { SettingsJumpPageWidget("当前档案", stored.name, Icons.TwoTone.Key) { showProfiles = true } }
                item { SettingsBaseWidget("新建档案", "创建新的初始连接配置", Icons.TwoTone.Key, onClick = { profileName = ""; showCreate = true }) }
                item { SettingsBaseWidget("重命名当前档案", stored.name, Icons.TwoTone.Edit, onClick = { profileName = stored.name; showRename = true }) }
                item { SettingsBaseWidget("创建副本", "复制当前连接与凭据并切换", Icons.TwoTone.ContentCopy, onClick = onDuplicate) }
                item { SettingsBaseWidget("删除当前档案", "切换到其余可用档案", Icons.Rounded.DeleteSweep, enabled = profiles.size > 1, isError = true, onClick = if (profiles.size > 1) ({ confirmDelete = true }) else null) }
            }
        }
        item {
            SegmentedColumn("导入与导出") {
                item { SettingsBaseWidget("导出当前配置", "生成 Distrust JSON", Icons.TwoTone.Info, onClick = { exportLauncher.launch("distrust-${stored.name}.json") }) }
                item { SettingsBaseWidget("导入配置", "从 Distrust JSON 导入配置，并覆盖当前档案", Icons.TwoTone.Settings, onClick = { importLauncher.launch(arrayOf("application/json", "text/plain")) }) }
            }
        }
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
                item { SettingsJumpPageWidget("DNS 缓存", "查看和清除历史成功地址与 FakeDNS", Icons.TwoTone.Settings) { onNavigate(AppRoute.DnsCacheSettings) } }
                item { SettingsJumpPageWidget("按应用路由", "选择进入或绕过系统 VPN 的应用", Icons.TwoTone.Shield) { onNavigate(AppRoute.AppRoutingSettings) } }
                item { SettingsJumpPageWidget("连接与会话", if (stored.protocol == VpnProtocol.ATRUST) "保活、节点优选和会话刷新" else "保活与 EasyConnect 会话参数", Icons.TwoTone.Settings) { onNavigate(AppRoute.SessionSettings) } }
            }
        }
    }
    if (showProfiles) {
        AlertDialog(
            onDismissRequest = { showProfiles = false },
            shape = RoundedCornerShape(32.dp),
            title = { Text("切换配置档案") },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(profiles, key = { it.id }) { saved ->
                        SettingsBaseWidget(saved.name, "${saved.protocol.label} · ${saved.server}:${saved.port}", Icons.TwoTone.Key, selected = saved.id == stored.id, onClick = {
                            showProfiles = false
                            onSwitch(saved.id)
                        })
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showProfiles = false }) { Text("完成") } },
        )
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            shape = RoundedCornerShape(32.dp),
            title = { Text("删除 ${stored.name}？") },
            text = { Text("档案目录将切换到其余可用配置。") },
            confirmButton = { TextButton(onClick = { confirmDelete = false; onDelete(stored.id) }) { Text("删除") } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("取消") } },
        )
    }
    if (showCreate || showRename) {
        val creating = showCreate
        AlertDialog(
            onDismissRequest = { showCreate = false; showRename = false },
            shape = RoundedCornerShape(32.dp),
            title = { Text(if (creating) "新建配置档案" else "重命名配置档案") },
            text = { SectionTextField(profileName, { profileName = it }, "档案名称") },
            confirmButton = {
                TextButton(enabled = profileName.isNotBlank(), onClick = {
                    if (creating) onCreate(profileName) else onRename(stored.id, profileName)
                    showCreate = false
                    showRename = false
                }) { Text(if (creating) "创建" else "保存") }
            },
            dismissButton = { TextButton(onClick = { showCreate = false; showRename = false }) { Text("取消") } },
        )
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
    val context = LocalContext.current
    val certificateLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        runCatching {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: error("证书读取失败")
            draft = draft.copy(certificateBase64 = Base64.encodeToString(bytes, Base64.NO_WRAP))
        }.onFailure { Logger.e("Certificate", "Failed to read PKCS#12 certificate", it) }
    }
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
                item {
                    SegmentedControlWidget("服务器传输") {
                        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                            ServerScheme.entries.forEachIndexed { index, scheme ->
                                SegmentedButton(draft.serverScheme == scheme, { draft = draft.copy(serverScheme = scheme) }, SegmentedButtonDefaults.itemShape(index, ServerScheme.entries.size)) { Text(scheme.name) }
                            }
                        }
                        if (draft.serverScheme == ServerScheme.HTTP) {
                            Text("HTTP 以明文方式传输认证与控制数据", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp))
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
                } else {
                    SectionTextField(draft.totpSecret, { draft = draft.copy(totpSecret = it) }, "TOTP 密钥（可选）", visualTransformation = PasswordVisualTransformation())
                    SectionTextField(draft.easyConnectTwfId, { draft = draft.copy(easyConnectTwfId = it) }, "TwfID（可选）", supportingText = "填写后直接恢复已授权 EasyConnect 会话")
                }
            }
        }
        if (draft.protocol == VpnProtocol.EASYCONNECT) {
            item {
                SegmentedColumn("客户端证书") {
                    item {
                        SettingsBaseWidget(
                            title = if (draft.certificateBase64.isBlank()) "选择 P12/PFX 证书" else "证书已载入",
                            description = "用于服务器返回证书认证要求时继续登录",
                            icon = Icons.TwoTone.Key,
                            onClick = { certificateLauncher.launch(arrayOf("application/x-pkcs12", "application/octet-stream", "*/*")) },
                        )
                    }
                    item(visible = draft.certificateBase64.isNotBlank()) {
                        SettingsBaseWidget("移除证书", "清理当前档案中的证书数据", Icons.Rounded.DeleteSweep, isError = true, onClick = { draft = draft.copy(certificateBase64 = "", certificatePassword = "") })
                    }
                }
            }
            item {
                EditorFields {
                    SectionTextField(draft.certificatePassword, { draft = draft.copy(certificatePassword = it) }, "证书密码（可选）", enabled = draft.certificateBase64.isNotBlank(), visualTransformation = PasswordVisualTransformation())
                }
            }
        }
    }
}

@Composable
private fun ProxySettingsPage(stored: ConnectionProfile, onSave: (ConnectionProfile) -> Unit, onBack: () -> Unit) {
    var draft by remember(stored) { mutableStateOf(stored) }
    val localProxyEnabled = draft.mode == ConnectionMode.LOCAL_PROXY
    val credentialsValid = !localProxyEnabled || draft.socksUsername.isBlank() == draft.socksPassword.isBlank()
    val directProxyValid = draft.dialDirectProxy.isBlank() || Regex("^(http|socks)://[^:/\\s]+:[0-9]{1,5}$").matches(draft.dialDirectProxy)
    EditorScaffold("本地代理", credentialsValid && directProxyValid, onBack, { onSave(draft); onBack() }) {
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
                SectionTextField(draft.socksPort.toString(), { it.toIntOrNull()?.let { v -> draft = draft.copy(socksPort = v.coerceIn(1, 65535)) } }, "SOCKS5 端口", enabled = localProxyEnabled, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                SectionTextField(draft.httpPort.toString(), { it.toIntOrNull()?.let { v -> draft = draft.copy(httpPort = v.coerceIn(1, 65535)) } }, "HTTP 端口", enabled = localProxyEnabled, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            }
        }
        item {
            EditorFields {
                Text("直连上游代理", style = MaterialTheme.typography.titleSmall)
                Text("为校园 VPN Resource 之外的 TCP 连接指定出口。留空表示直接连接。", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                SectionTextField(
                    draft.dialDirectProxy,
                    { draft = draft.copy(dialDirectProxy = it.trim()) },
                    "上游代理（可选）",
                    supportingText = "例如 http://127.0.0.1:7890 或 socks://127.0.0.1:7891",
                )
                if (!directProxyValid) {
                    Text("仅支持 http://主机:端口 或 socks://主机:端口", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        if (!credentialsValid) {
            item {
                Text(
                    "SOCKS5 用户名和密码必须同时填写",
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
        item {
            EditorFields {
                Text("SOCKS5 鉴权", style = MaterialTheme.typography.titleSmall, color = if (localProxyEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f))
                Text(if (localProxyEnabled) "用户名和密码同时填写时启用；SOCKS5 以明文方式传输流量与凭据。" else "切换到本地代理模式后可编辑。", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                SectionTextField(draft.socksUsername, { draft = draft.copy(socksUsername = it) }, "用户名（可选）", enabled = localProxyEnabled)
                SectionTextField(draft.socksPassword, { draft = draft.copy(socksPassword = it) }, "密码（可选）", enabled = localProxyEnabled, visualTransformation = PasswordVisualTransformation())
            }
        }
    }
}

@Composable
private fun PolicySettingsPage(stored: ConnectionProfile, onSave: (ConnectionProfile) -> Unit, onBack: () -> Unit) {
    var draft by remember(stored) { mutableStateOf(stored) }
    fun updateDns(index: Int, value: String) {
        val servers = draft.dnsServers.toMutableList()
        while (servers.size <= index) servers += ""
        servers[index] = value.trim()
        draft = draft.copy(dnsServers = servers.dropLastWhile(String::isBlank))
    }
    EditorScaffold("DNS 与分流", true, onBack, { onSave(draft); onBack() }) {
        item {
            SegmentedColumn("策略") {
                item { SettingsSwitchWidget("代理全部流量", "所有请求优先匹配服务端资源", Icons.TwoTone.Shield, draft.proxyAll) { draft = draft.copy(proxyAll = it) } }
                item { SettingsSwitchWidget("本地配置模式", "以本地路由和 DNS 配置为准", Icons.TwoTone.Settings, draft.disableServerConfig) { draft = draft.copy(disableServerConfig = it) } }
                item { SettingsSwitchWidget("使用本地 DNS", "通过 Android 系统与直连备用 DNS 解析", Icons.TwoTone.Settings, draft.disableRemoteDns) { draft = draft.copy(disableRemoteDns = it) } }
                item { SettingsSwitchWidget("仅使用 IP 资源", "根据服务端 IP Resource 决定 VPN 路由", Icons.TwoTone.Shield, draft.skipDomainResource) { draft = draft.copy(skipDomainResource = it) } }
                item { SettingsSwitchWidget("仅使用 TCP Tunnel", "TCP 资源统一通过应用层隧道传输", Icons.TwoTone.Lan, draft.tcpTunnelOnly, enabled = draft.protocol == VpnProtocol.ATRUST) { draft = draft.copy(tcpTunnelOnly = it) } }
            }
        }
        item {
            EditorFields {
                SectionTextField(draft.routes.joinToString("\n"), { draft = draft.copy(routes = it.lines().map(String::trim).filter(String::isNotBlank)) }, "分流网段", singleLine = false, supportingText = "每行一个 CIDR")
                SectionTextField(draft.dnsServers.getOrNull(0).orEmpty(), { updateDns(0, it) }, "主 DNS（自动）", supportingText = "留空时使用服务端下发的主策略 DNS")
                SectionTextField(draft.dnsServers.getOrNull(1).orEmpty(), { updateDns(1, it) }, "备用 DNS（自动）", supportingText = "留空时自动选择第二策略 DNS或系统备用 DNS")
                SectionTextField(draft.customDns.entries.joinToString("\n") { "${it.key}=${it.value}" }, { value ->
                    draft = draft.copy(customDns = value.lineSequence().mapNotNull { line -> line.split('=', limit = 2).takeIf { it.size == 2 }?.let { it[0].trim() to it[1].trim() } }.toMap())
                }, "自定义 DNS", singleLine = false, supportingText = "高级覆盖：域名=IP")
                SectionTextField(draft.dnsTtl.toString(), { it.toIntOrNull()?.let { v -> draft = draft.copy(dnsTtl = v.coerceAtLeast(1)) } }, "DNS 缓存时间（秒）", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                SectionTextField(draft.customProxyDomains.joinToString("\n"), { value -> draft = draft.copy(customProxyDomains = value.lines().map(String::trim).filter(String::isNotBlank).distinct()) }, "强制代理域名", singleLine = false, supportingText = "每行一个域名；服务端域名规则之外的条目也尝试通过 VPN")
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
    val keepAliveValid = draft.keepAliveUrl.isBlank() || runCatching {
        val uri = URI(draft.keepAliveUrl)
        uri.scheme in setOf("http", "https") && !uri.host.isNullOrBlank()
    }.getOrDefault(false)
    EditorScaffold("连接与会话", keepAliveValid, onBack, { onSave(draft); onBack() }) {
        item {
            SegmentedColumn("连接保活") {
                item {
                    SettingsSwitchWidget(
                        "应用层保活",
                        "每分钟执行一次 DNS/HTTP 会话探测",
                        Icons.TwoTone.Settings,
                        !draft.disableKeepAlive,
                    ) { draft = draft.copy(disableKeepAlive = !it) }
                }
            }
        }
        item {
            EditorFields {
                SectionTextField(
                    draft.keepAliveUrl,
                    { draft = draft.copy(keepAliveUrl = it.trim()) },
                    "保活 URL（可选）",
                    enabled = !draft.disableKeepAlive,
                    supportingText = "留空时通过远程 DNS 探测；填写后每分钟发送一次 HTTP GET",
                )
                if (!keepAliveValid) {
                    Text("请输入有效的 http:// 或 https:// URL", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        if (draft.protocol == VpnProtocol.ATRUST) item {
            EditorFields {
                SectionTextField(draft.updateBestNodesInterval.toString(), { it.toIntOrNull()?.let { v -> draft = draft.copy(updateBestNodesInterval = v.coerceAtLeast(0)) } }, "节点优选间隔（秒）", supportingText = "设置为 0 时停止定时优选", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                SectionTextField(draft.sessionRefreshInterval.toString(), { it.toIntOrNull()?.let { v -> draft = draft.copy(sessionRefreshInterval = v.coerceAtLeast(0)) } }, "会话刷新间隔（秒）", supportingText = "设置为 0 时停止定时刷新", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            }
        }
        if (draft.protocol == VpnProtocol.ATRUST) item {
            SegmentedColumn("已保存会话") {
                item {
                    SettingsBaseWidget(
                        title = if (stored.clientData.isBlank()) "等待会话认证" else "清除保存的会话",
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
            title = { Text("清除保存的会话？") },
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

private data class RoutableApp(val packageName: String, val label: String, val icon: Drawable)

@Composable
private fun AppRoutingSettingsPage(
    stored: ConnectionProfile,
    onSave: (ConnectionProfile) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var draft by remember(stored) { mutableStateOf(stored) }
    var apps by remember { mutableStateOf<List<RoutableApp>>(emptyList()) }
    var query by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        apps = withContext(Dispatchers.IO) {
            val pm = context.packageManager
            pm.getInstalledApplications(PackageManager.GET_META_DATA)
                .asSequence()
                .filter { it.packageName != context.packageName && pm.getLaunchIntentForPackage(it.packageName) != null }
                .map { RoutableApp(it.packageName, pm.getApplicationLabel(it).toString(), pm.getApplicationIcon(it)) }
                .sortedBy { it.label.lowercase() }
                .toList()
        }
    }
    val valid = draft.appRoutingMode != AppRoutingMode.ALLOW_ONLY || draft.routedPackages.isNotEmpty()
    val visibleApps = apps.filter { query.isBlank() || it.label.contains(query, true) || it.packageName.contains(query, true) }
    EditorScaffold("按应用路由", valid, onBack, { onSave(draft); onBack() }) {
        item {
            SegmentedColumn("模式") {
                item {
                    SegmentedControlWidget("系统 VPN 应用范围") {
                        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                            AppRoutingMode.entries.forEachIndexed { index, mode ->
                                SegmentedButton(
                                    selected = draft.appRoutingMode == mode,
                                    onClick = { draft = draft.copy(appRoutingMode = mode) },
                                    shape = SegmentedButtonDefaults.itemShape(index, AppRoutingMode.entries.size),
                                ) {
                                    Text(when (mode) { AppRoutingMode.ALL -> "全部"; AppRoutingMode.ALLOW_ONLY -> "仅选中"; AppRoutingMode.EXCLUDE -> "排除" })
                                }
                            }
                        }
                    }
                }
            }
        }
        if (draft.appRoutingMode != AppRoutingMode.ALL) {
            item {
                EditorFields {
                    SectionTextField(query, { query = it }, "搜索应用")
                    if (!valid) Text("仅选中模式至少需要选择一个应用", color = MaterialTheme.colorScheme.error)
                }
            }
            if (apps.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        LoadingIndicator(Modifier.size(48.dp))
                    }
                }
            }
            items(visibleApps, key = { it.packageName }) { app ->
                Box(Modifier.padding(horizontal = 16.dp, vertical = 2.dp)) {
                    SettingsSwitchWidget(
                        title = app.label,
                        description = app.packageName,
                        checked = app.packageName in draft.routedPackages,
                        leadingContent = {
                            Image(
                                bitmap = remember(app.packageName) { app.icon.toBitmap(48, 48).asImageBitmap() },
                                contentDescription = null,
                                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)),
                            )
                        },
                    ) { checked ->
                        draft = draft.copy(
                            routedPackages = if (checked) draft.routedPackages + app.packageName else draft.routedPackages - app.packageName,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DnsCacheSettingsPage(
    profile: ConnectionProfile,
    loadFakeDns: () -> Result<Map<String, String>>,
    clearFakeDns: () -> Result<Unit>,
    onBack: () -> Unit,
) {
    val namespace = "${profile.server}:${profile.port}"
    var history by remember(namespace) { mutableStateOf(DnsHistoryStore.entries(namespace)) }
    var fakeDns by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var fakeDnsError by remember { mutableStateOf<String?>(null) }
    fun reload() {
        history = DnsHistoryStore.entries(namespace)
        loadFakeDns().fold(
            onSuccess = { fakeDns = it; fakeDnsError = null },
            onFailure = { fakeDns = emptyMap(); fakeDnsError = it.message },
        )
    }
    LaunchedEffect(namespace) { reload() }
    SubPage("DNS 缓存", onBack) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
            item {
                Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FilledTonalButton(onClick = { DnsHistoryStore.clear(namespace); reload() }, modifier = Modifier.weight(1f)) { Text("清除历史") }
                    FilledTonalButton(onClick = { clearFakeDns(); reload() }, modifier = Modifier.weight(1f)) { Text("清除 FakeDNS") }
                }
            }
            item {
                SegmentedColumn("历史成功地址") {
                    if (history.isEmpty()) item { SettingsBaseWidget("等待产生历史记录", namespace, Icons.TwoTone.Info) }
                    history.forEach { entry -> item(key = entry.host) { SettingsBaseWidget(entry.host, entry.addresses.joinToString(), Icons.TwoTone.Settings) } }
                }
            }
            item {
                SegmentedColumn("当前 FakeDNS") {
                    if (fakeDns.isEmpty()) item { SettingsBaseWidget("等待产生 FakeDNS 映射", fakeDnsError ?: "连接并访问域名资源后显示", Icons.TwoTone.Info) }
                    fakeDns.toSortedMap().forEach { (host, address) -> item(key = host) { SettingsBaseWidget(host, address, Icons.TwoTone.Shield) } }
                }
            }
        }
    }
}

private enum class LogFilter { ALL, INFO, WARNING, ERROR }

@Composable
private fun LogPage(logs: List<LogEntry>, onClear: () -> Unit, onExport: (List<LogEntry>) -> Unit, bottomPadding: Dp) {
    val formatter = remember { DateTimeFormatter.ofPattern("HH:mm:ss") }
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(LogFilter.ALL) }
    val filtered = logs.filter { entry ->
        val levelMatches = when (filter) {
            LogFilter.ALL -> true
            LogFilter.INFO -> entry.level in setOf(LogLevel.VERBOSE, LogLevel.DEBUG, LogLevel.INFO)
            LogFilter.WARNING -> entry.level == LogLevel.WARNING
            LogFilter.ERROR -> entry.level in setOf(LogLevel.ERROR, LogLevel.ASSERT)
        }
        levelMatches && (query.isBlank() || entry.tag.contains(query, true) || entry.message.contains(query, true))
    }
    PageScaffold("运行日志", bottomPadding, actions = {
        IconButton(onClick = { onExport(filtered) }) { Icon(Icons.Rounded.Save, "导出日志") }
        IconButton(onClick = onClear) { Icon(Icons.Rounded.DeleteSweep, "清空日志") }
    }) {
        item {
            EditorFields {
                SectionTextField(query, { query = it }, "搜索日志")
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    LogFilter.entries.forEachIndexed { index, value ->
                        SegmentedButton(filter == value, { filter = value }, SegmentedButtonDefaults.itemShape(index, LogFilter.entries.size)) {
                            Text(when (value) { LogFilter.ALL -> "全部"; LogFilter.INFO -> "信息"; LogFilter.WARNING -> "警告"; LogFilter.ERROR -> "错误" })
                        }
                    }
                }
            }
        }
        if (filtered.isEmpty()) item { Text("等待符合筛选条件的日志", Modifier.padding(24.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) }
        items(filtered.asReversed(), key = { "${it.timestamp}-${it.tag}-${it.message.hashCode()}" }) { entry ->
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
    val context = LocalContext.current
    val packageInfo = remember(context) {
        context.packageManager.getPackageInfo(context.packageName, 0)
    }
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
                item { SettingsBaseWidget("版本", "${packageInfo.versionName} (${PackageInfoCompat.getLongVersionCode(packageInfo)})", Icons.TwoTone.Info) }
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
    var clickPoints by remember(challengeJson) { mutableStateOf<List<Pair<Int, Int>>>(emptyList()) }
    var imageBoxSize by remember { mutableStateOf(IntSize.Zero) }
    val captchaImage = remember(challengeJson) {
        payload?.optString("imageBase64")?.takeIf(String::isNotBlank)?.let { encoded ->
            runCatching {
                Base64.decode(encoded, Base64.DEFAULT).let { BitmapFactory.decodeByteArray(it, 0, it.size).asImageBitmap() }
            }.getOrNull()
        }
    }
    val markerColor = MaterialTheme.colorScheme.primary
    val markerCenterColor = MaterialTheme.colorScheme.onPrimary
    val title = when (payload?.optString("kind")) {
        "sms" -> "短信验证码"
        "totp" -> "TOTP 验证码"
        "radius" -> "RADIUS 动态口令"
        else -> when (type) { "textCaptcha" -> "图形验证码"; "clickCaptcha" -> "点选验证码"; else -> "继续认证" }
    }
    AlertDialog(
        onDismissRequest = onCancel,
        shape = RoundedCornerShape(32.dp),
        icon = { Icon(Icons.TwoTone.Shield, null) },
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                payload?.optString("message")?.takeIf(String::isNotBlank)?.let { Text(it) }
                captchaImage?.let { image ->
                    Box(
                        Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(20.dp))
                            .onSizeChanged { imageBoxSize = it }
                            .pointerInput(type, image, imageBoxSize) {
                                if (type == "clickCaptcha") detectTapGestures { tap ->
                                    val scale = minOf(imageBoxSize.width / image.width.toFloat(), imageBoxSize.height / image.height.toFloat())
                                    val shownWidth = image.width * scale
                                    val shownHeight = image.height * scale
                                    val left = (imageBoxSize.width - shownWidth) / 2f
                                    val top = (imageBoxSize.height - shownHeight) / 2f
                                    if (tap.x in left..(left + shownWidth) && tap.y in top..(top + shownHeight)) {
                                        clickPoints = clickPoints + (((tap.x - left) / scale).toInt().coerceIn(0, image.width - 1) to ((tap.y - top) / scale).toInt().coerceIn(0, image.height - 1))
                                    }
                                }
                            },
                    ) {
                        Image(image, "验证码", Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                        Canvas(Modifier.fillMaxSize()) {
                            val scale = minOf(size.width / image.width, size.height / image.height)
                            val left = (size.width - image.width * scale) / 2f
                            val top = (size.height - image.height * scale) / 2f
                            clickPoints.forEach { point ->
                                val center = androidx.compose.ui.geometry.Offset(left + point.first * scale, top + point.second * scale)
                                drawCircle(markerColor, radius = 12.dp.toPx(), center = center)
                                drawCircle(markerCenterColor, radius = 5.dp.toPx(), center = center)
                            }
                        }
                    }
                }
                if (type == "clickCaptcha") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("已选择 ${clickPoints.size} 个位置", modifier = Modifier.weight(1f))
                        TextButton(onClick = { clickPoints = emptyList() }) { Text("重新选择") }
                    }
                } else SectionTextField(value, { value = it }, "认证响应")
                if (payload?.optBoolean("canSkipSecondaryAuth") == true) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(skipSecondary, { skipSecondary = it })
                        Text("仅完成当前认证步骤")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = if (type == "clickCaptcha") clickPoints.isNotEmpty() && captchaImage != null else value.isNotBlank(), onClick = {
                if (type == "clickCaptcha" && captchaImage != null) {
                    val points = org.json.JSONArray().apply {
                        clickPoints.forEach { (x, y) -> put(JSONObject().put("x", x).put("y", y)) }
                    }
                    onSubmit(JSONObject().put("Points", points).put("Width", captchaImage.width).put("Height", captchaImage.height).toString())
                } else onSubmit(JSONObject().put("Code", value).put("SkipSecondaryAuth", skipSecondary).toString())
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
    ProfilePage(
        stored = previewProfile,
        profiles = listOf(previewProfile),
        onSave = {}, onSwitch = {}, onDuplicate = {}, onCreate = {}, onRename = { _, _ -> },
        onDelete = {}, onNavigate = {}, onOpenWizard = {}, bottomPadding = 0.dp,
    )
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
    LogPage(previewLogs, {}, {}, 0.dp)
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
        profile = previewProfile,
        profiles = listOf(previewProfile),
        state = ConnectionState.Disconnected,
        logs = previewLogs,
        snackbar = remember { SnackbarHostState() },
        onConnect = {}, onDisconnect = {}, onClearLogs = {}, onExportLogs = {}, onSaveProfile = {},
        onSwitchProfile = {}, onDuplicateProfile = {}, onCreateProfile = {}, onRenameProfile = { _, _ -> },
        onDeleteProfile = {}, onNavigate = {}, onOpenWizard = {},
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

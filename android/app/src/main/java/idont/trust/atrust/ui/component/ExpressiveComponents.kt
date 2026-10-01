package idont.trust.atrust.ui.component

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.material3.LocalContentColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.max
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt
import androidx.compose.ui.tooling.preview.Preview
import idont.trust.atrust.ui.theme.ThemeConfig
import idont.trust.atrust.ui.theme.DistrustTheme

val LocalSegmentedItemShape = compositionLocalOf<Shape> { RoundedCornerShape(16.dp) }

@Immutable
internal data class Segment(
    val key: Any,
    val visible: Boolean,
    val content: @Composable () -> Unit,
)

class SegmentedColumnScope internal constructor() {
    internal val segments = mutableListOf<Segment>()

    fun item(
        key: Any = segments.size,
        visible: Boolean = true,
        content: @Composable () -> Unit,
    ) {
        segments += Segment(key, visible, content)
    }
}

@Composable
fun SegmentedColumn(
    title: String = "",
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    content: SegmentedColumnScope.() -> Unit,
) {
    val segments = SegmentedColumnScope().apply(content).segments
    val visible = segments.filter { it.visible }
    if (visible.isEmpty()) return
    Column(modifier.padding(contentPadding)) {
        if (title.isNotBlank()) {
            Text(
                title,
                modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
		val floatSpring = spring<Float>(dampingRatio = 0.5f, stiffness = 800f)
		val dpSpring = spring<Dp>(dampingRatio = 0.5f, stiffness = 800f)
		val progresses = segments.map { segment -> key(segment.key) { animateFloatAsState(if (segment.visible) 1f else 0f, floatSpring, label = "progress") } }
		val firstVisibleIndex = segments.indexOfFirst { it.visible }
		val lastVisibleIndex = segments.indexOfLast { it.visible }
		Layout(content = {
			segments.forEachIndexed { index, segment -> key(segment.key) {
				val top by animateDpAsState(if (index == firstVisibleIndex) 16.dp else 5.dp, dpSpring, label = "topRadius")
				val bottom by animateDpAsState(if (index == lastVisibleIndex) 16.dp else 5.dp, dpSpring, label = "bottomRadius")
				val gap by animateDpAsState(if (index == firstVisibleIndex) 0.dp else 2.dp, dpSpring, label = "gap")
				val shape = RoundedCornerShape(max(0.dp, top), max(0.dp, top), max(0.dp, bottom), max(0.dp, bottom))
				Box(Modifier.zIndex(if (segment.visible) (segments.size - index).toFloat() else -index.toFloat()).graphicsLayer {
					val progress = progresses[index].value.coerceAtLeast(0f)
					clip = true
					this.shape = object : Shape {
						override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density) = Outline.Rectangle(Rect(0f, 0f, size.width, size.height * progress))
					}
					alpha = (progress * 1.5f).coerceIn(0f, 1f)
				}.padding(top = max(0.dp, gap))) {
					CompositionLocalProvider(LocalSegmentedItemShape provides shape) { segment.content() }
				}
			} }
		}) { measurables, constraints ->
			val placeables = measurables.map { it.measure(constraints) }
			var currentY = 0f
			val positions = placeables.mapIndexed { index, placeable -> currentY.roundToInt().also { currentY += placeable.height * progresses[index].value } }
			layout(constraints.maxWidth, currentY.roundToInt().coerceAtLeast(0)) {
				placeables.forEachIndexed { index, placeable -> placeable.placeRelative(0, positions[index]) }
			}
		}
    }
}

@Composable
fun SettingsBaseWidget(
    title: String,
    description: String? = null,
    icon: ImageVector? = null,
    leadingContent: (@Composable () -> Unit)? = null,
    iconColor: Color? = null,
    iconSize: Dp = 24.dp,
    enabled: Boolean = true,
    selected: Boolean = false,
    isError: Boolean = false,
    containerColor: Color? = null,
    onClick: (() -> Unit)? = null,
    foreContent: @Composable RowScope.() -> Unit = {},
    trailingContent: (@Composable () -> Unit)? = null,
) {
    val interaction = androidx.compose.runtime.remember { MutableInteractionSource() }
	val haptic = LocalHapticFeedback.current
    val pressed by interaction.collectIsPressedAsState()
    val baseShape = LocalSegmentedItemShape.current
    val pressedRadius by animateDpAsState(
		if (pressed) 16.dp else 0.dp,
		spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "pressedRadius",
    )
	val shape = if (pressedRadius > 0.dp) RoundedCornerShape(pressedRadius) else baseShape
	LaunchedEffect(pressed) {
		if (pressed && enabled && onClick != null) haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
	}
    val container = containerColor ?: when {
        selected -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = ThemeConfig.cardAlpha)
    }
    val contentColor = when {
        containerColor != null -> contentColorFor(containerColor).let {
            if (it == Color.Unspecified) MaterialTheme.colorScheme.onSurface else it
        }
        isError -> MaterialTheme.colorScheme.error
        selected -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }
    val itemContent: @Composable () -> Unit = {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)
                .alpha(if (enabled) 1f else 0.38f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (leadingContent != null) {
                leadingContent()
            } else if (icon != null) {
                Icon(
                    icon,
                    null,
                    Modifier.size(iconSize),
                    tint = iconColor ?: when {
                        isError -> contentColor
                        onClick != null -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    foreContent()
                }
                description?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isError) contentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            trailingContent?.invoke()
        }
    }
    val surfaceModifier = Modifier.fillMaxWidth().heightIn(min = if (description == null) 64.dp else 76.dp)
    if (onClick != null) {
		Box(
			modifier = surfaceModifier
				.clip(shape)
				.background(container)
				.clickable(
					enabled = enabled,
					interactionSource = interaction,
					indication = androidx.compose.material3.ripple(color = contentColor),
					onClick = onClick,
				),
		) {
			CompositionLocalProvider(LocalContentColor provides contentColor) {
				itemContent()
			}
		}
    } else {
        Surface(
            modifier = surfaceModifier,
			shape = shape,
            color = container,
            contentColor = contentColor,
            content = itemContent,
        )
    }
}

@Composable
fun SettingsSwitchWidget(
    title: String,
    description: String? = null,
    icon: ImageVector? = null,
    checked: Boolean,
    enabled: Boolean = true,
    leadingContent: (@Composable () -> Unit)? = null,
    onCheckedChange: (Boolean) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val update: (Boolean) -> Unit = {
        haptic.performHapticFeedback(if (it) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff)
        onCheckedChange(it)
    }
	SettingsBaseWidget(
            title = title,
            description = description,
            icon = icon,
            leadingContent = leadingContent,
            enabled = enabled,
            onClick = { update(!checked) },
	) {
            Switch(
                checked = checked,
                onCheckedChange = null,
                thumbContent = {
                    Icon(
                        if (checked) Icons.Rounded.Check else Icons.Rounded.Close,
                        null,
                        Modifier.size(SwitchDefaults.IconSize),
                    )
                },
            )
	}
}

@Composable
fun SettingsJumpPageWidget(
    title: String,
    description: String? = null,
    icon: ImageVector? = null,
    onClick: () -> Unit,
) {
    SettingsBaseWidget(title, description, icon, onClick = onClick) {
        Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun SegmentedControlWidget(
    title: String,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = LocalSegmentedItemShape.current,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.size(8.dp))
            content()
        }
    }
}

@Composable
fun SectionTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    supportingText: String? = null,
    enabled: Boolean = true,
    visualTransformation: androidx.compose.ui.text.input.VisualTransformation = androidx.compose.ui.text.input.VisualTransformation.None,
    keyboardOptions: androidx.compose.foundation.text.KeyboardOptions = androidx.compose.foundation.text.KeyboardOptions.Default,
) {
    androidx.compose.material3.OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        supportingText = supportingText?.let { { Text(it) } },
        enabled = enabled,
        singleLine = singleLine,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        shape = RoundedCornerShape(20.dp),
        modifier = modifier.fillMaxWidth(),
    )
}

@Preview(name = "Segmented column", showBackground = true)
@Composable
private fun SegmentedColumnPreview() = DistrustTheme {
    SegmentedColumn("连接信息") {
        item { SettingsBaseWidget("协议与服务器", "aTrust · vpn.example.edu.cn:443", Icons.Rounded.Check) }
        item { SettingsBaseWidget("会话地址", "10.85.4.117", Icons.Rounded.Check) }
    }
}

@Preview(name = "Base widget", showBackground = true)
@Composable
private fun SettingsBaseWidgetPreview() = DistrustTheme {
    Box(Modifier.padding(16.dp)) {
        SettingsBaseWidget("系统 VPN 模式", "接管系统选定流量", Icons.Rounded.Check)
    }
}

@Preview(name = "Selected widget", showBackground = true)
@Composable
private fun SelectedSettingsBaseWidgetPreview() = DistrustTheme {
    Box(Modifier.padding(16.dp)) {
        SettingsBaseWidget("aTrust", "服务器发现的认证方式", Icons.Rounded.Check, selected = true)
    }
}

@Preview(name = "Switch widget", showBackground = true)
@Composable
private fun SettingsSwitchWidgetPreview() = DistrustTheme {
    Box(Modifier.padding(16.dp)) {
        SettingsSwitchWidget("代理全部流量", "所有请求优先匹配服务端资源", Icons.Rounded.Check, true) {}
    }
}

@Preview(name = "Jump widget", showBackground = true)
@Composable
private fun SettingsJumpPageWidgetPreview() = DistrustTheme {
    Box(Modifier.padding(16.dp)) {
        SettingsJumpPageWidget("配置向导", "发现认证方式并配置连接", Icons.Rounded.Check) {}
    }
}

@Preview(name = "Segmented control", showBackground = true)
@Composable
private fun SegmentedControlWidgetPreview() = DistrustTheme {
    Box(Modifier.padding(16.dp)) {
        SegmentedControlWidget("运行模式") {
            Text("Choice controls use explicit onSurface content color")
        }
    }
}

@Preview(name = "Expressive text field", showBackground = true)
@Composable
private fun SectionTextFieldPreview() = DistrustTheme {
    Box(Modifier.padding(16.dp)) {
        SectionTextField("vpn.example.edu.cn", {}, "服务器地址", supportingText = "aTrust 服务地址")
    }
}

package idont.trust.atrust.ui.theme

import android.app.WallpaperManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.kieronquinn.monetcompat.core.MonetCompat
import com.kieronquinn.monetcompat.interfaces.MonetColorsChangedListener
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamicColorScheme
import com.materialkolor.dynamiccolor.ColorSpec
import dev.kdrag0n.monet.theme.ColorScheme
import kotlinx.coroutines.launch

@Stable
object ThemeConfig {
    var forceDarkMode by mutableStateOf<Boolean?>(null)
    var monetSeedColor by mutableStateOf<Int?>(null)
    var dynamicColorSpec by mutableStateOf(ColorSpec.SpecVersion.SPEC_2021)
    var dynamicPaletteStyle by mutableStateOf(PaletteStyle.TonalSpot)

    // Matches ReSukiSU CardConfig: cards remain opaque unless a custom wallpaper is enabled.
    const val cardAlpha: Float = 1f
}

@Composable
fun DistrustTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val dark = ThemeConfig.forceDarkMode ?: isSystemInDarkTheme()
    val inspectionMode = LocalInspectionMode.current
    val wallpaperSeed = remember(context, inspectionMode) {
        if (inspectionMode) context.getColor(android.R.color.system_accent1_500) else resolveWallpaperSeed(context)
    }
    if (!inspectionMode) MonetCompatInitializer(wallpaperSeed)
    val seed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        colorResource(android.R.color.system_accent1_500).toArgb()
    } else {
        ThemeConfig.monetSeedColor ?: wallpaperSeed
    }
    val colorScheme = dynamicColorScheme(
        seedColor = Color(seed),
        isDark = dark,
        style = ThemeConfig.dynamicPaletteStyle,
        specVersion = ThemeConfig.dynamicColorSpec,
    )
    val activity = context as? ComponentActivity
    if (activity != null && !inspectionMode) SideEffect {
        activity.enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.Transparent.toArgb(), Color.Transparent.toArgb()) { dark },
            navigationBarStyle = if (dark) SystemBarStyle.dark(Color.Transparent.toArgb())
            else SystemBarStyle.light(Color.Transparent.toArgb(), Color.Transparent.toArgb()),
        )
    }
    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        motionScheme = MotionScheme.expressive(),
        content = content,
    )
}

@Composable
private fun MonetCompatInitializer(wallpaperSeed: Int) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) return
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    DisposableEffect(context) {
        MonetCompat.useSystemColorsOnAndroid12 = false
        runCatching { MonetCompat.enablePaletteCompat() }
        val monet = MonetCompat.setup(context).apply {
            defaultPrimaryColor = wallpaperSeed
            defaultSecondaryColor = wallpaperSeed
            defaultAccentColor = wallpaperSeed
        }
        val listener = object : MonetColorsChangedListener {
            override fun onMonetColorsChanged(monet: MonetCompat, monetColors: ColorScheme, isInitialChange: Boolean) {
                scope.launch {
                    ThemeConfig.monetSeedColor = monet.getSelectedWallpaperColor() ?: wallpaperSeed
                }
            }
        }
        monet.addMonetColorsChangedListener(listener, true)
        onDispose { monet.removeMonetColorsChangedListener(listener) }
    }
}

private fun resolveWallpaperSeed(context: android.content.Context): Int {
    val wallpaper = WallpaperManager.getInstance(context)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
        wallpaper.getWallpaperColors(WallpaperManager.FLAG_SYSTEM)?.primaryColor?.toArgb()?.let { return it }
    }
    val drawable = requireNotNull(wallpaper.drawable) { "System wallpaper is unavailable for Monet extraction" }
    val bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, 1, 1)
    drawable.draw(canvas)
    return bitmap.getPixel(0, 0)
}

@Preview(name = "Monet palette", showBackground = true)
@Composable
private fun DistrustThemePreview() = DistrustTheme {
    Column(Modifier.padding(16.dp)) {
        Text("Distrust", style = MaterialTheme.typography.headlineLarge)
        Row(Modifier.fillMaxWidth().padding(top = 16.dp)) {
            Surface(Modifier.weight(1f), color = MaterialTheme.colorScheme.primaryContainer) {
                Text("Primary", Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Surface(Modifier.weight(1f), color = MaterialTheme.colorScheme.secondaryContainer) {
                Text("Secondary", Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSecondaryContainer)
            }
        }
    }
}

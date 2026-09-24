package idont.trust.atrust.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = Color(0xFF335F91),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD1E4FF),
    onPrimaryContainer = Color(0xFF001D36),
    secondary = Color(0xFF535F70),
    secondaryContainer = Color(0xFFD7E3F8),
    tertiary = Color(0xFF6B5778),
    tertiaryContainer = Color(0xFFF3DAFF),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9ECAFF),
    primaryContainer = Color(0xFF164777),
    secondary = Color(0xFFBBC7DB),
    secondaryContainer = Color(0xFF3B4858),
    tertiary = Color(0xFFD7BDE4),
    tertiaryContainer = Color(0xFF523F5F),
)

@Composable
fun DistrustTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val dark = isSystemInDarkTheme()
    val colors = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && dark -> dynamicDarkColorScheme(context)
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> dynamicLightColorScheme(context)
        dark -> DarkColors
        else -> LightColors
    }
    MaterialTheme(colorScheme = colors, content = content)
}

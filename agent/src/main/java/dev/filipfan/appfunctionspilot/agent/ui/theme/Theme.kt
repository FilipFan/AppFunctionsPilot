package dev.filipfan.appfunctionspilot.agent.ui.theme

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

private val DarkColorScheme = darkColorScheme(
    primary = Blue_Dark,
    secondary = Teal_Dark,
    background = Black_Dark,
    surface = Gray_Dark,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = Charcoal_Dark,
    onSurface = Charcoal_Dark,
)

private val LightColorScheme = lightColorScheme(
    primary = Blue_Light,
    secondary = Teal_Light,
    background = Gray_Light,
    surface = White_Light,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = Charcoal_Light,
    onSurface = Charcoal_Light,
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true, // Dynamic color is available on Android 12+
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content,
    )
}

package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.data.local.AppLanguage
import com.example.data.local.AppThemeMode

private val SatyenDarkColorScheme = darkColorScheme(
    primary = RoyalBlueBright,
    onPrimary = Color.White,
    primaryContainer = RoyalBlue,
    onPrimaryContainer = RoyalBlueSoft,
    secondary = VioletBright,
    onSecondary = NavyDeep,
    secondaryContainer = VioletGlow,
    onSecondaryContainer = VioletSoft,
    tertiary = CyanSpark,
    onTertiary = NavyDeep,
    background = NavyDeep,
    onBackground = DarkOnBg,
    surface = NavySurface,
    onSurface = DarkOnBg,
    surfaceVariant = NavyCard,
    onSurfaceVariant = DarkOnMuted,
    outline = NavyBorder,
    error = CrimsonExpense,
    onError = Color.White
)

private val SatyenLightColorScheme = lightColorScheme(
    primary = RoyalBlue,
    onPrimary = Color.White,
    primaryContainer = RoyalBlueSoft,
    onPrimaryContainer = NavyDeep,
    secondary = VioletGlow,
    onSecondary = Color.White,
    secondaryContainer = VioletSoft,
    onSecondaryContainer = NavyDeep,
    tertiary = CyanSpark,
    onTertiary = Color.White,
    background = LightBg,
    onBackground = LightOnBg,
    surface = LightSurface,
    onSurface = LightOnBg,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnMuted,
    outline = LightOutline,
    error = CrimsonExpense,
    onError = Color.White
)

private val SatyenShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun SatyenMetaAiTheme(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    language: AppLanguage = AppLanguage.EN,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    }
    val colorScheme = if (darkTheme) SatyenDarkColorScheme else SatyenLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = buildAppTypography(language),
        shapes = SatyenShapes,
        content = content
    )
}

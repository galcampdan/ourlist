package com.ourlist.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface as MaterialSurface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val OurListColors = darkColorScheme(
    primary = Primary,
    onPrimary = Ink,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = PrimarySoft,
    secondary = Review,
    onSecondary = Ink,
    secondaryContainer = SurfaceRaised,
    onSecondaryContainer = TextPrimary,
    tertiary = HeartRed,
    onTertiary = Ink,
    background = Ink,
    onBackground = TextPrimary,
    surface = Surface,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceRaised,
    onSurfaceVariant = TextSecondary,
    surfaceContainer = SurfaceRaised,
    surfaceContainerHigh = SurfaceSelected,
    surfaceContainerLow = Surface,
    inverseSurface = TextPrimary,
    inverseOnSurface = Ink,
    error = Danger,
    onError = Ink,
    outline = Divider,
    outlineVariant = Divider,
)

@Composable
fun OurListTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = OurListColors,
        typography = OurListTypography,
    ) {
        MaterialSurface(
            color = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onBackground,
            content = content,
        )
    }
}

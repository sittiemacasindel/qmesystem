package com.qme.mobile.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val QmeLightColors = lightColorScheme(
    primary          = QmeBlue,
    onPrimary        = QmeWhite,
    primaryContainer = QmeSky,
    onPrimaryContainer = QmeDarkBlue,
    secondary        = QmeCyan,
    onSecondary      = QmeWhite,
    background       = QmeSurface,
    onBackground     = QmeOnSurface,
    surface          = QmeWhite,
    onSurface        = QmeOnSurface,
    outline          = QmeDivider
)

@Composable
fun QMeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = QmeLightColors,
        typography  = QmeTypography,
        content     = content
    )
}

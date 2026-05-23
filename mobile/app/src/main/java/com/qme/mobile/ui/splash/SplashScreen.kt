package com.qme.mobile.ui.splash

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import com.qme.mobile.ui.components.QmeLogo
import com.qme.mobile.ui.theme.QmeSky
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onTimeout: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(
        targetValue   = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 700),
        label         = "splash_alpha"
    )

    LaunchedEffect(Unit) {
        visible = true
        delay(2200)
        onTimeout()
    }

    Box(
        modifier         = Modifier.fillMaxSize().background(QmeSky),
        contentAlignment = Alignment.Center
    ) {
        QmeLogo(
            large    = true,
            modifier = Modifier.alpha(alpha)
        )
    }
}
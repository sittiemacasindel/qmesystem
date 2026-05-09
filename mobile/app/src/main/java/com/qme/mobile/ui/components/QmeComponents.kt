package com.qme.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qme.mobile.ui.theme.QmeBlue
import com.qme.mobile.ui.theme.QmeDarkBlue
import com.qme.mobile.ui.theme.QmeErrorBg
import com.qme.mobile.ui.theme.QmeError
import com.qme.mobile.ui.theme.QmeSky
import com.qme.mobile.ui.theme.QmeSubtext
import com.qme.mobile.ui.theme.QmeWhite

/** Centered "QME" logo text shown on auth screens */
@Composable
fun QmeLogo(modifier: Modifier = Modifier) {
    Column(
        modifier       = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text       = "QME",
            fontSize   = 52.sp,
            fontWeight = FontWeight.ExtraBold,
            color      = QmeBlue,
            letterSpacing = 2.sp
        )
        Text(
            text       = "Queue Me",
            fontSize   = 14.sp,
            fontWeight = FontWeight.Medium,
            color      = QmeSubtext
        )
    }
}

/** Top app bar with "QME" title centered */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QmeTopBar(
    title: String = "QME",
    navigationIcon: @Composable () -> Unit = {}
) {
    TopAppBar(
        title = {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    text       = title,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 20.sp,
                    color      = QmeDarkBlue
                )
            }
        },
        navigationIcon = navigationIcon,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor    = QmeWhite,
            titleContentColor = QmeDarkBlue
        )
    )
}

/** Standard labeled text field */
@Composable
fun QmeTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    enabled: Boolean = true,
    singleLine: Boolean = true
) {
    OutlinedTextField(
        value               = value,
        onValueChange       = onValueChange,
        label               = { Text(label) },
        modifier            = modifier.fillMaxWidth(),
        leadingIcon         = leadingIcon,
        trailingIcon        = trailingIcon,
        visualTransformation = visualTransformation,
        keyboardOptions     = keyboardOptions,
        enabled             = enabled,
        singleLine          = singleLine,
        shape               = RoundedCornerShape(10.dp)
    )
}

/** Full-width primary action button with optional loading state */
@Composable
fun QmePrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    loading: Boolean = false,
    enabled: Boolean = true
) {
    Button(
        onClick  = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp),
        enabled  = enabled && !loading,
        shape    = RoundedCornerShape(10.dp),
        colors   = ButtonDefaults.buttonColors(containerColor = QmeBlue)
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier  = Modifier.size(20.dp),
                color     = QmeWhite,
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text       = text,
                fontWeight = FontWeight.SemiBold,
                fontSize   = 15.sp,
                color      = QmeWhite
            )
        }
    }
}

/** Inline error message banner */
@Composable
fun QmeErrorBanner(message: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(QmeErrorBg, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment    = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text       = "⚠ $message",
            color      = QmeError,
            fontSize   = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

/** Centered loading spinner */
@Composable
fun QmeLoader(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = QmeBlue)
    }
}

/** Info card with light-blue background */
@Composable
fun QmeInfoCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(QmeSky, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        content()
    }
}

/** Status badge chip (ACTIVE / PAUSED) */
@Composable
fun StatusBadge(status: String) {
    val (bg, fg) = when (status.uppercase()) {
        "ACTIVE" -> Color(0xFFE8F5E9) to Color(0xFF2E7D32)
        "PAUSED" -> Color(0xFFFFF3E0) to Color(0xFFF57C00)
        else     -> Color(0xFFECEFF1) to Color(0xFF607D8B)
    }
    Box(
        modifier = Modifier
            .background(bg, RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text       = status.uppercase(),
            color      = fg,
            fontSize   = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/** Section spacer */
@Composable
fun QmeSpacer(height: Int = 16) = Spacer(Modifier.height(height.dp))

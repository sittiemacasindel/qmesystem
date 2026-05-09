package com.qme.mobile.ui.auth

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qme.mobile.ui.components.QmeErrorBanner
import com.qme.mobile.ui.components.QmeLogo
import com.qme.mobile.ui.components.QmePrimaryButton
import com.qme.mobile.ui.components.QmeSpacer
import com.qme.mobile.ui.components.QmeTextField
import com.qme.mobile.ui.theme.QmeBlue
import com.qme.mobile.ui.theme.QmeSubtext

@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit
) {
    var email       by remember { mutableStateOf("") }
    var password    by remember { mutableStateOf("") }
    var showPass    by remember { mutableStateOf(false) }
    var loading     by remember { mutableStateOf(false) }
    var errorMsg    by remember { mutableStateOf<String?>(null) }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 28.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment  = Alignment.CenterHorizontally,
            verticalArrangement  = Arrangement.Center
        ) {
            QmeSpacer(40)

            QmeLogo()

            QmeSpacer(32)

            Text(
                text       = "Log in",
                fontSize   = 24.sp,
                fontWeight = FontWeight.Bold,
                color      = QmeBlue
            )

            QmeSpacer(20)

            errorMsg?.let { QmeErrorBanner(it); QmeSpacer(12) }

            QmeTextField(
                value         = email,
                onValueChange = { email = it; errorMsg = null },
                label         = "Email address",
                leadingIcon   = { Icon(Icons.Default.Email, null, tint = QmeBlue) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                enabled       = !loading
            )

            QmeSpacer(14)

            QmeTextField(
                value               = password,
                onValueChange       = { password = it; errorMsg = null },
                label               = "Password",
                leadingIcon         = { Icon(Icons.Default.Lock, null, tint = QmeBlue) },
                trailingIcon        = {
                    IconButton(onClick = { showPass = !showPass }) {
                        Icon(
                            if (showPass) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = null,
                            tint = QmeSubtext
                        )
                    }
                },
                visualTransformation = if (showPass) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions      = KeyboardOptions(keyboardType = KeyboardType.Password),
                enabled              = !loading
            )

            QmeSpacer(24)

            QmePrimaryButton(
                text    = "Log in",
                onClick = {
                    loading = true
                    errorMsg = null
                    viewModel.login(email, password) { success, msg ->
                        loading = false
                        if (success) onLoginSuccess() else errorMsg = msg
                    }
                },
                loading = loading
            )

            QmeSpacer(16)

            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text      = buildAnnotatedString {
                        append("Don't have an account? ")
                        withStyle(SpanStyle(color = QmeBlue, fontWeight = FontWeight.SemiBold)) {
                            append("Sign up")
                        }
                    },
                    fontSize  = 13.sp,
                    modifier  = Modifier.clickable(enabled = !loading) { onNavigateToRegister() }
                )
            }

            QmeSpacer(40)
        }
    }
}

package com.qme.mobile.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qme.mobile.data.model.response.UserProfileResponse
import com.qme.mobile.ui.components.QmeErrorBanner
import com.qme.mobile.ui.components.QmeLoader
import com.qme.mobile.ui.components.QmePrimaryButton
import com.qme.mobile.ui.components.QmeSpacer
import com.qme.mobile.ui.components.QmeTextField
import com.qme.mobile.ui.components.QmeTopBar
import com.qme.mobile.ui.theme.QmeBlue
import com.qme.mobile.ui.theme.QmeDarkBlue
import com.qme.mobile.ui.theme.QmeOnSurface
import com.qme.mobile.ui.theme.QmeSky
import com.qme.mobile.ui.theme.QmeSubtext
import com.qme.mobile.ui.theme.QmeSuccess
import com.qme.mobile.ui.theme.QmeSuccessBg
import com.qme.mobile.ui.theme.QmeWhite

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onLogout: () -> Unit
) {
    var profile     by remember { mutableStateOf<UserProfileResponse?>(null) }
    var loading     by remember { mutableStateOf(true) }

    // Edit profile state
    var name        by remember { mutableStateOf("") }
    var org         by remember { mutableStateOf("") }
    var saving      by remember { mutableStateOf(false) }
    var profileMsg  by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

    // Change password state
    var currentPwd  by remember { mutableStateOf("") }
    var newPwd      by remember { mutableStateOf("") }
    var confirmPwd  by remember { mutableStateOf("") }
    var showCurr    by remember { mutableStateOf(false) }
    var showNew     by remember { mutableStateOf(false) }
    var showConf    by remember { mutableStateOf(false) }
    var pwdSaving   by remember { mutableStateOf(false) }
    var pwdMsg      by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadProfile { p, _ ->
            profile = p
            name    = p?.name ?: ""
            org     = p?.organization ?: ""
            loading = false
        }
    }

    Scaffold(topBar = { QmeTopBar() }) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            QmeSpacer(20)

            if (loading) {
                QmeLoader()
            } else {
                // ── Avatar initials ──
                val initials = name.split(" ").mapNotNull { it.firstOrNull()?.toString() }
                    .take(2).joinToString("").uppercase().ifEmpty { "U" }

                Column(
                    modifier            = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(QmeBlue, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(initials, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = QmeWhite)
                    }
                    QmeSpacer(10)
                    Text(profile?.name ?: "", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = QmeDarkBlue)
                    Text(profile?.email ?: "", fontSize = 13.sp, color = QmeSubtext)
                    Text(profile?.role ?: "CUSTOMER", fontSize = 11.sp, color = QmeBlue, fontWeight = FontWeight.Medium)
                }

                QmeSpacer(24)
                HorizontalDivider(color = QmeSky)
                QmeSpacer(20)

                // ── Edit Profile ──
                Text("Edit Profile", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = QmeDarkBlue)
                QmeSpacer(12)

                profileMsg?.let { (success, msg) ->
                    if (success) SuccessBanner(msg) else QmeErrorBanner(msg)
                    QmeSpacer(8)
                }

                QmeTextField(
                    value         = name,
                    onValueChange = { name = it; profileMsg = null },
                    label         = "Full Name",
                    leadingIcon   = { Icon(Icons.Default.Person, null, tint = QmeBlue) },
                    enabled       = !saving
                )
                QmeSpacer(10)
                QmeTextField(
                    value         = org,
                    onValueChange = { org = it; profileMsg = null },
                    label         = "Organization (optional)",
                    enabled       = !saving
                )
                QmeSpacer(14)
                QmePrimaryButton(
                    text    = "Save Changes",
                    onClick = {
                        saving = true
                        profileMsg = null
                        viewModel.updateProfile(name, org.ifBlank { null }) { ok, msg ->
                            saving = false
                            profileMsg = ok to (msg ?: if (ok) "Profile updated!" else "Unknown error")
                        }
                    },
                    loading = saving
                )

                QmeSpacer(28)
                HorizontalDivider(color = QmeSky)
                QmeSpacer(20)

                // ── Change Password ──
                Text("Change Password", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = QmeDarkBlue)
                QmeSpacer(12)

                pwdMsg?.let { (success, msg) ->
                    if (success) SuccessBanner(msg) else QmeErrorBanner(msg)
                    QmeSpacer(8)
                }

                QmeTextField(
                    value               = currentPwd,
                    onValueChange       = { currentPwd = it; pwdMsg = null },
                    label               = "Current Password",
                    leadingIcon         = { Icon(Icons.Default.Lock, null, tint = QmeBlue) },
                    trailingIcon        = {
                        IconButton(onClick = { showCurr = !showCurr }) {
                            Icon(if (showCurr) Icons.Default.VisibilityOff else Icons.Default.Visibility, null, tint = QmeSubtext)
                        }
                    },
                    visualTransformation = if (showCurr) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions      = KeyboardOptions(keyboardType = KeyboardType.Password),
                    enabled              = !pwdSaving
                )
                QmeSpacer(10)
                QmeTextField(
                    value               = newPwd,
                    onValueChange       = { newPwd = it; pwdMsg = null },
                    label               = "New Password",
                    leadingIcon         = { Icon(Icons.Default.Lock, null, tint = QmeBlue) },
                    trailingIcon        = {
                        IconButton(onClick = { showNew = !showNew }) {
                            Icon(if (showNew) Icons.Default.VisibilityOff else Icons.Default.Visibility, null, tint = QmeSubtext)
                        }
                    },
                    visualTransformation = if (showNew) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions      = KeyboardOptions(keyboardType = KeyboardType.Password),
                    enabled              = !pwdSaving
                )
                QmeSpacer(10)
                QmeTextField(
                    value               = confirmPwd,
                    onValueChange       = { confirmPwd = it; pwdMsg = null },
                    label               = "Confirm New Password",
                    leadingIcon         = { Icon(Icons.Default.Lock, null, tint = QmeBlue) },
                    trailingIcon        = {
                        IconButton(onClick = { showConf = !showConf }) {
                            Icon(if (showConf) Icons.Default.VisibilityOff else Icons.Default.Visibility, null, tint = QmeSubtext)
                        }
                    },
                    visualTransformation = if (showConf) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions      = KeyboardOptions(keyboardType = KeyboardType.Password),
                    enabled              = !pwdSaving
                )
                QmeSpacer(14)
                QmePrimaryButton(
                    text    = "Change Password",
                    onClick = {
                        pwdSaving = true
                        pwdMsg = null
                        viewModel.changePassword(currentPwd, newPwd, confirmPwd) { ok, msg ->
                            pwdSaving = false
                            pwdMsg    = ok to (msg ?: if (ok) "Password changed!" else "Unknown error")
                            if (ok) { currentPwd = ""; newPwd = ""; confirmPwd = "" }
                        }
                    },
                    loading = pwdSaving
                )

                QmeSpacer(28)
                HorizontalDivider(color = QmeSky)
                QmeSpacer(16)

                // ── Logout ──
                QmePrimaryButton(
                    text    = "Logout",
                    onClick = onLogout
                )
            }

            QmeSpacer(32)
        }
    }
}

@Composable
private fun SuccessBanner(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(QmeSuccessBg, androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text("✓ $message", color = QmeSuccess, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

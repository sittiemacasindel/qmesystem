package com.qme.mobile.ui.queue

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qme.mobile.data.model.response.OrganizationResponse
import com.qme.mobile.ui.components.QmeErrorBanner
import com.qme.mobile.ui.components.QmeInfoCard
import com.qme.mobile.ui.components.QmeLoader
import com.qme.mobile.ui.components.QmePrimaryButton
import com.qme.mobile.ui.components.QmeSpacer
import com.qme.mobile.ui.components.QmeTextField
import com.qme.mobile.ui.components.QmeTopBar
import com.qme.mobile.ui.components.StatusBadge
import com.qme.mobile.ui.theme.QmeBlue
import com.qme.mobile.ui.theme.QmeDarkBlue
import com.qme.mobile.ui.theme.QmeOnSurface
import com.qme.mobile.ui.theme.QmeSky
import com.qme.mobile.ui.theme.QmeSubtext

@Composable
fun JoinQueueScreen(
    viewModel: JoinQueueViewModel,
    onJoinSuccess: (entryId: String) -> Unit
) {
    var code         by remember { mutableStateOf("") }
    var lookingUp    by remember { mutableStateOf(false) }
    var joining      by remember { mutableStateOf(false) }
    var errorMsg     by remember { mutableStateOf<String?>(null) }
    var foundOrg     by remember { mutableStateOf<OrganizationResponse?>(null) }
    var showDialog   by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { QmeTopBar() }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            QmeSpacer(20)

            Text(
                text       = "Join a Queue",
                fontSize   = 20.sp,
                fontWeight = FontWeight.Bold,
                color      = QmeDarkBlue
            )

            QmeSpacer(16)

            // ── Code entry card ──
            QmeInfoCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text       = "ENTER QUEUE CODE",
                        fontSize   = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color      = QmeSubtext,
                        letterSpacing = 1.sp
                    )

                    QmeTextField(
                        value           = code,
                        onValueChange   = { if (it.length <= 6) { code = it; errorMsg = null } },
                        label           = "6-digit code",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        enabled         = !lookingUp
                    )

                    errorMsg?.let { QmeErrorBanner(it) }

                    QmePrimaryButton(
                        text    = "Join",
                        onClick = {
                            lookingUp = true
                            errorMsg = null
                            viewModel.lookupCode(code.trim()) { org, err ->
                                lookingUp = false
                                if (org != null) {
                                    foundOrg   = org
                                    showDialog = true
                                } else {
                                    errorMsg = err
                                }
                            }
                        },
                        loading = lookingUp
                    )
                }
            }

            QmeSpacer(24)

            // ── How to get a queue code ──
            Text(
                text       = "How to get a queue code:",
                fontSize   = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color      = QmeOnSurface
            )
            QmeSpacer(6)
            Text("1. Ask the service provider", fontSize = 13.sp, color = QmeSubtext)
            Text("2. Check their website or display", fontSize = 13.sp, color = QmeSubtext)

            QmeSpacer(24)
        }
    }

    // ── Confirm Join dialog ──
    if (showDialog && foundOrg != null) {
        ConfirmJoinDialog(
            org      = foundOrg!!,
            joining  = joining,
            onJoin   = {
                joining = true
                viewModel.confirmJoin(foundOrg!!.queueCode) { response, err ->
                    joining = false
                    if (response != null) {
                        showDialog = false
                        onJoinSuccess(response.entryId)
                    } else {
                        errorMsg   = err
                        showDialog = false
                    }
                }
            },
            onCancel = { showDialog = false }
        )
    }
}

@Composable
private fun ConfirmJoinDialog(
    org: OrganizationResponse,
    joining: Boolean,
    onJoin: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        containerColor   = QmeSky,
        shape            = RoundedCornerShape(16.dp),
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                StatusBadge(org.status)
                QmeSpacer(6)
                Text(
                    org.name,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 16.sp,
                    color      = QmeDarkBlue
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                HorizontalDivider(color = QmeBlue.copy(alpha = 0.2f))
                if (!org.openingHours.isNullOrBlank() && !org.closingHours.isNullOrBlank()) {
                    DetailRow("🕐", "${org.openingHours} – ${org.closingHours}")
                }
                if (!org.location.isNullOrBlank()) {
                    DetailRow("📍", "Location: ${org.location}")
                }
                if (!org.contactNumber.isNullOrBlank()) {
                    DetailRow("📞", org.contactNumber)
                }
                org.totalWaitingCustomers?.let {
                    DetailRow("👥", "$it people currently waiting")
                }
            }
        },
        confirmButton = {
            QmePrimaryButton(
                text    = "Join Queue",
                onClick = onJoin,
                loading = joining
            )
        },
        dismissButton = {
            OutlinedButton(onClick = onCancel) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun DetailRow(icon: String, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(icon, fontSize = 14.sp)
        Text(text, fontSize = 13.sp, color = QmeOnSurface)
    }
}

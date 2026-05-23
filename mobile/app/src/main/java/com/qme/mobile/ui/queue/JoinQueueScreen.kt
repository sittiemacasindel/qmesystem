package com.qme.mobile.ui.queue

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.qme.mobile.data.model.response.HistoryEntryResponse
import com.qme.mobile.data.model.response.OrganizationResponse
import com.qme.mobile.ui.components.*
import com.qme.mobile.ui.theme.*

@Composable
fun JoinQueueScreen(
    viewModel: JoinQueueViewModel,
    onJoinSuccess: (entryId: String) -> Unit,
    onViewHistory: () -> Unit = {}
) {
    var code         by remember { mutableStateOf("") }
    var lookingUp    by remember { mutableStateOf(false) }
    var joining      by remember { mutableStateOf(false) }
    var errorMsg     by remember { mutableStateOf<String?>(null) }
    var foundOrg     by remember { mutableStateOf<OrganizationResponse?>(null) }
    var showNotFound by remember { mutableStateOf(false) }
    var recentQueues by remember { mutableStateOf<List<HistoryEntryResponse>>(emptyList()) }
    var hasMore      by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadRecentQueues { list, more ->
            recentQueues = list
            hasMore      = more
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
            Text("Join a Queue", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = QmeDarkBlue)
            QmeSpacer(16)

            // ── Code entry card ──
            QmeInfoCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "ENTER QUEUE CODE",
                        fontSize = 11.sp, fontWeight = FontWeight.Bold,
                        color = QmeSubtext, letterSpacing = 1.sp
                    )
                    QmeTextField(
                        value           = code.uppercase(),
                        onValueChange   = {
                            if (it.length <= 6) {
                                code     = it.uppercase()
                                errorMsg = null
                            }
                        },
                        label           = "6-digit code",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        enabled         = !lookingUp
                    )
                    errorMsg?.let { QmeErrorBanner(it) }
                    QmePrimaryButton(
                        text    = "Search",
                        loading = lookingUp,
                        onClick = {
                            lookingUp = true
                            errorMsg  = null
                            foundOrg  = null
                            viewModel.lookupCode(code.trim()) { org, err ->
                                lookingUp = false
                                when {
                                    org != null -> foundOrg = org
                                    err?.contains("No queue found") == true ||
                                            err?.contains("not found", ignoreCase = true) == true ->
                                        showNotFound = true
                                    else -> errorMsg = err
                                }
                            }
                        }
                    )
                }
            }

            QmeSpacer(24)
            Text("How to get a queue code:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = QmeOnSurface)
            QmeSpacer(6)
            Text("1. Ask the service provider", fontSize = 13.sp, color = QmeSubtext)
            Text("2. Check their website or display board", fontSize = 13.sp, color = QmeSubtext)

            // ── Recent Queues ──
            if (recentQueues.isNotEmpty()) {
                QmeSpacer(24)
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text("Recent Queues", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = QmeDarkBlue)
                    if (hasMore) {
                        TextButton(onClick = onViewHistory) {
                            Text("View More", color = QmeBlue, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                    }
                }
                QmeSpacer(8)
                recentQueues.forEach { entry ->
                    RecentQueueRow(entry = entry, onClick = {
                        code     = entry.queueCode
                        errorMsg = null
                        foundOrg = null
                    })
                    QmeSpacer(8)
                }
            }

            QmeSpacer(24)
        }
    }

    // ── Queue Preview Modal (Dialog) ──
    if (foundOrg != null) {
        Dialog(
            onDismissRequest = { foundOrg = null },
            properties       = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier      = Modifier
                    .fillMaxWidth(0.92f)
                    .wrapContentHeight(),
                shape         = RoundedCornerShape(20.dp),
                color         = QmeWhite,
                tonalElevation = 8.dp
            ) {
                QueuePreviewContent(
                    org     = foundOrg!!,
                    joining = joining,
                    onJoin  = {
                        joining = true
                        viewModel.confirmJoin(foundOrg!!.queueCode) { response, err ->
                            joining = false
                            if (response != null) {
                                foundOrg = null
                                onJoinSuccess(response.entryId)
                            } else {
                                foundOrg = null
                                errorMsg = err
                            }
                        }
                    },
                    onCancel = { foundOrg = null }
                )
            }
        }
    }

    // ── Queue Not Found dialog ──
    if (showNotFound) {
        AlertDialog(
            onDismissRequest = { showNotFound = false },
            title = { Text("Queue Not Found", fontWeight = FontWeight.Bold, color = QmeDarkBlue) },
            text  = { Text("This queue does not exist. Please check the code and try again.", color = QmeSubtext) },
            confirmButton = {
                TextButton(onClick = { showNotFound = false; code = "" }) {
                    Text("OK", color = QmeBlue, fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }
}

@Composable
private fun QueuePreviewContent(
    org: OrganizationResponse,
    joining: Boolean,
    onJoin: () -> Unit,
    onCancel: () -> Unit
) {
    val isPaused    = org.status.uppercase() == "PAUSED"
    val statusColor = if (isPaused) androidx.compose.ui.graphics.Color(0xFFE65100)
    else          androidx.compose.ui.graphics.Color(0xFF2E7D32)

    Column(
        modifier            = Modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Avatar — real photo if available
        OrgAvatar(orgName = org.name, photoBase64 = org.photo, size = 80.dp)

        // Status
        Text(
            text          = if (isPaused) "PAUSED" else "OPEN",
            color         = statusColor,
            fontSize      = 13.sp,
            fontWeight    = FontWeight.ExtraBold,
            letterSpacing = 1.sp
        )

        // Name
        Text(
            text       = org.name,
            fontSize   = 18.sp,
            fontWeight = FontWeight.Bold,
            color      = QmeDarkBlue,
            textAlign  = TextAlign.Center
        )

        HorizontalDivider(color = QmeBlue.copy(alpha = 0.15f))

        if (!org.openingHours.isNullOrBlank()) {
            PreviewRow(Icons.Default.AccessTime, "${org.openingHours} – ${org.closingHours}")
        }
        if (!org.location.isNullOrBlank()) {
            PreviewRow(Icons.Default.LocationOn, org.location)
        }
        if (!org.contactNumber.isNullOrBlank()) {
            PreviewRow(Icons.Default.Phone, org.contactNumber)
        }

        QmeSpacer(4)

        if (isPaused) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(androidx.compose.ui.graphics.Color(0xFFFFF3E0), RoundedCornerShape(8.dp))
                    .padding(10.dp)
            ) {
                Text(
                    "This queue is currently paused.",
                    fontSize  = 12.sp,
                    color     = androidx.compose.ui.graphics.Color(0xFFE65100),
                    textAlign = TextAlign.Center,
                    modifier  = Modifier.fillMaxWidth()
                )
            }
        }

        QmePrimaryButton(
            text    = "Join Queue",
            onClick = onJoin,
            loading = joining,
            enabled = !isPaused
        )
        TextButton(onClick = onCancel) {
            Text("Cancel", color = QmeSubtext, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun PreviewRow(icon: ImageVector, text: String) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(icon, contentDescription = null, tint = QmeBlue)
        Text(text, fontSize = 13.sp, color = QmeOnSurface)
    }
}

@Composable
private fun RecentQueueRow(entry: HistoryEntryResponse, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(QmeSky, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OrgAvatar(orgName = entry.organizationName, size = 40.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(entry.organizationName, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = QmeDarkBlue)
            Text("Code: ${entry.queueCode}", fontSize = 12.sp, color = QmeSubtext)
        }
        Text(entry.queueCode, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = QmeBlue)
    }
}
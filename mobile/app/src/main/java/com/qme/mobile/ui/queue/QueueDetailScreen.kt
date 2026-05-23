package com.qme.mobile.ui.queue

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qme.mobile.data.model.response.QueueDetailsResponse
import com.qme.mobile.ui.components.OrgAvatar
import com.qme.mobile.ui.components.QmeErrorBanner
import com.qme.mobile.ui.components.QmeLoader
import com.qme.mobile.ui.components.QmePrimaryButton
import com.qme.mobile.ui.components.QmeSpacer
import com.qme.mobile.ui.components.QmeTopBar
import com.qme.mobile.ui.theme.QmeBlue
import com.qme.mobile.ui.theme.QmeDarkBlue
import com.qme.mobile.ui.theme.QmeError
import com.qme.mobile.ui.theme.QmeOnSurface
import com.qme.mobile.ui.theme.QmeSky
import com.qme.mobile.ui.theme.QmeSubtext
import com.qme.mobile.ui.theme.QmeSuccess
import com.qme.mobile.ui.theme.QmeSuccessBg
import com.qme.mobile.ui.theme.QmeWhite
import kotlinx.coroutines.delay

private data class ServiceSummary(
    val orgName: String,
    val queueCode: String,
    val queueNumber: Int,
    val joinedAt: String?
)

@Composable
fun QueueDetailScreen(
    viewModel: QueueDetailViewModel,
    onBack: () -> Unit,
    onCancelled: () -> Unit,
    onJoinAnother: () -> Unit,
    onDone: () -> Unit
) {
    var loading         by remember { mutableStateOf(true) }
    var details         by remember { mutableStateOf<QueueDetailsResponse?>(null) }
    var errorMsg        by remember { mutableStateOf<String?>(null) }
    var cancelling      by remember { mutableStateOf(false) }
    var cancelConfirm   by remember { mutableStateOf(false) }
    var userCancelled   by remember { mutableStateOf(false) }

    // Served state — captures last known info before queue disappears
    var servedSummary   by remember { mutableStateOf<ServiceSummary?>(null) }
    var showYourTurn    by remember { mutableStateOf(false) }
    var prevPosition    by remember { mutableIntStateOf(-1) }

    // ── Polling loop ──
    LaunchedEffect(Unit) {
        while (true) {
            viewModel.loadDetails { d, err ->
                if (!userCancelled) {
                    if (d != null) {
                        // Detect position → 1 transition for "Your Turn" popup
                        if (prevPosition > 1 && d.positionInLine == 1) {
                            showYourTurn = true
                        }
                        prevPosition = d.positionInLine
                        details      = d
                        errorMsg     = null
                    } else {
                        // null with no error = served externally
                        if (err == null && details != null) {
                            // Capture service summary before clearing
                            val prev = details!!
                            servedSummary = ServiceSummary(
                                orgName     = prev.organization.name,
                                queueCode   = prev.organization.queueCode,
                                queueNumber = prev.queueNumber,
                                joinedAt    = prev.joinedAt
                            )
                            details = null
                        } else if (err != null) {
                            errorMsg = err
                        }
                    }
                    loading = false
                }
            }
            delay(10_000)
        }
    }

    // ── "Your Turn" popup ──
    if (showYourTurn) {
        AlertDialog(
            onDismissRequest = { showYourTurn = false },
            containerColor   = QmeSuccessBg,
            shape            = RoundedCornerShape(16.dp),
            title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("🎉", fontSize = 36.sp)
                    QmeSpacer(4)
                    Text("Your Turn!", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = QmeSuccess)
                }
            },
            text = {
                Text(
                    "Your Turn! Please proceed to the counter.",
                    textAlign = TextAlign.Center,
                    color     = QmeOnSurface
                )
            },
            confirmButton = {
                QmePrimaryButton(
                    text            = "OK",
                    onClick         = { showYourTurn = false },
                    containerColor  = QmeSuccess
                )
            }
        )
    }

    // ── Cancel confirmation dialog ──
    if (cancelConfirm && details != null) {
        AlertDialog(
            onDismissRequest = { cancelConfirm = false },
            title = { Text("Leave Queue?", fontWeight = FontWeight.Bold) },
            text  = { Text("Are you sure you want to leave? Your spot will be lost.") },
            confirmButton = {
                Button(
                    onClick = {
                        cancelConfirm = false
                        cancelling    = true
                        userCancelled = true
                        viewModel.cancelQueue(details!!.entryId) { success, err ->
                            cancelling = false
                            if (success) onCancelled() else { userCancelled = false; errorMsg = err }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = QmeError)
                ) { Text("Yes, Leave", color = QmeWhite) }
            },
            dismissButton = {
                TextButton(onClick = { cancelConfirm = false }) { Text("Keep My Spot") }
            }
        )
    }

    Scaffold(
        topBar = {
            QmeTopBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = QmeDarkBlue)
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            QmeSpacer(16)

            when {
                loading       -> QmeLoader()

                // ── Served / Thank You screen ──
                servedSummary != null -> ServedScreen(
                    summary       = servedSummary!!,
                    onJoinAnother = onJoinAnother,
                    onDone        = onDone
                )

                // ── Error ──
                errorMsg != null && details == null -> {
                    QmeErrorBanner(errorMsg!!)
                }

                // ── Active queue ──
                details != null -> {
                    val d   = details!!
                    val org = d.organization

                    // Org avatar
                    Column(
                        modifier            = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        OrgAvatar(orgName = org.name, photoBase64 = org.photo, size = 80.dp)
                    }

                    QmeSpacer(16)

                    // Org info border card
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, QmeBlue.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
                            .background(QmeWhite, RoundedCornerShape(14.dp))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            val statusColor = if (org.status == "ACTIVE") QmeSuccess
                            else Color(0xFFE65100)
                            Text(
                                text       = org.status,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize   = 12.sp,
                                color      = statusColor,
                                letterSpacing = 0.5.sp
                            )
                            if (!org.openingHours.isNullOrBlank()) {
                                Text(
                                    "⏰ ${org.openingHours} – ${org.closingHours}",
                                    fontSize = 12.sp, color = QmeSubtext
                                )
                            }
                        }
                        HorizontalDivider(color = QmeBlue.copy(alpha = 0.15f))
                        Text(org.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = QmeDarkBlue)
                        if (!org.location.isNullOrBlank()) {
                            Text("📍 ${org.location}", fontSize = 13.sp, color = QmeSubtext)
                        }
                        Text("🔑 ${org.queueCode}", fontSize = 13.sp, color = QmeSubtext)
                    }

                    QmeSpacer(16)

                    // Position card
                    val isBeingServed = d.computedStatus == "BEING_SERVED"
                    val isNext        = d.positionInLine == 1
                    val statusLabel   = when {
                        isBeingServed -> "Serving"
                        isNext        -> "Your Turn"
                        else          -> "Waiting"
                    }
                    val statusBg = when {
                        isBeingServed || isNext -> QmeSuccessBg
                        else                   -> QmeSky
                    }
                    val statusFg = when {
                        isBeingServed || isNext -> QmeSuccess
                        else                   -> QmeBlue
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(statusBg, RoundedCornerShape(16.dp))
                            .padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            statusLabel,
                            fontSize      = 13.sp,
                            fontWeight    = FontWeight.Bold,
                            color         = statusFg,
                            letterSpacing = 1.sp
                        )

                        Text(
                            text       = "${d.positionInLine}",
                            fontSize   = 64.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color      = statusFg
                        )

                        Text("YOUR POSITION", fontSize = 11.sp, color = QmeSubtext, letterSpacing = 1.sp)

                        val ahead = (d.positionInLine - 1).coerceAtLeast(0)
                        if (ahead > 0) {
                            Text("$ahead ${if (ahead == 1) "person" else "people"} ahead", fontSize = 13.sp, color = QmeSubtext)
                        }

                        HorizontalDivider(color = statusFg.copy(alpha = 0.2f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("#${d.queueNumber}", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = statusFg)
                                Text("Your Number", fontSize = 11.sp, color = QmeSubtext)
                            }
                            d.currentlyServingNumber?.let {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("#$it", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = QmeBlue)
                                    Text("Now Serving", fontSize = 11.sp, color = QmeSubtext)
                                }
                            }
                            d.estimatedWaitTime?.let {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("~$it min", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = QmeBlue)
                                    Text("Est. Wait", fontSize = 11.sp, color = QmeSubtext)
                                }
                            }
                        }

                        d.joinedAt?.let {
                            Text(
                                "Joined: ${it.take(16).replace("T", " ")}",
                                fontSize = 12.sp, color = QmeSubtext
                            )
                        }
                    }

                    QmeSpacer(20)

                    Button(
                        onClick  = { cancelConfirm = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape    = RoundedCornerShape(12.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = QmeError),
                        enabled  = !cancelling
                    ) {
                        Text(
                            if (cancelling) "Cancelling…" else "Cancel Queue",
                            color = QmeWhite, fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            QmeSpacer(24)
        }
    }
}

@Composable
private fun ServedScreen(
    summary: ServiceSummary,
    onJoinAnother: () -> Unit,
    onDone: () -> Unit
) {
    Column(
        modifier            = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        QmeSpacer(16)

        Text("✅", fontSize = 56.sp)

        Text(
            "Thank you!",
            fontSize   = 26.sp,
            fontWeight = FontWeight.ExtraBold,
            color      = QmeSuccess,
            textAlign  = TextAlign.Center
        )

        Text(
            "Your service is now complete.",
            fontSize  = 15.sp,
            color     = QmeSubtext,
            textAlign = TextAlign.Center
        )

        QmeSpacer(8)

        // Summary card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, QmeBlue.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
                .background(QmeWhite, RoundedCornerShape(14.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Service Summary", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = QmeSubtext)
            HorizontalDivider(color = QmeBlue.copy(alpha = 0.15f))
            SummaryRow("Queue Name", summary.orgName)
            SummaryRow("Queue No.", "#${summary.queueNumber}")
            SummaryRow("Code", summary.queueCode)
            summary.joinedAt?.let {
                SummaryRow("Joined", it.take(16).replace("T", " "))
            }
        }

        QmeSpacer(8)

        QmePrimaryButton(
            text           = "Join Another Queue",
            onClick        = onJoinAnother,
            containerColor = QmeBlue
        )

        TextButton(onClick = onDone) {
            Text(
                "Done → View History",
                color      = QmeSubtext,
                fontWeight = FontWeight.Medium,
                fontSize   = 14.sp
            )
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 13.sp, color = QmeSubtext)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = QmeOnSurface)
    }
}
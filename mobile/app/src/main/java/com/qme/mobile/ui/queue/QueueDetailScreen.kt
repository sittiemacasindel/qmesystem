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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qme.mobile.data.model.response.QueueDetailsResponse
import com.qme.mobile.ui.components.QmeErrorBanner
import com.qme.mobile.ui.components.QmeInfoCard
import com.qme.mobile.ui.components.QmeLoader
import com.qme.mobile.ui.components.QmeSpacer
import com.qme.mobile.ui.components.QmeTopBar
import com.qme.mobile.ui.components.StatusBadge
import com.qme.mobile.ui.theme.QmeBlue
import com.qme.mobile.ui.theme.QmeDarkBlue
import com.qme.mobile.ui.theme.QmeError
import com.qme.mobile.ui.theme.QmeErrorBg
import com.qme.mobile.ui.theme.QmeOnSurface
import com.qme.mobile.ui.theme.QmeSky
import com.qme.mobile.ui.theme.QmeSubtext
import com.qme.mobile.ui.theme.QmeWhite
import kotlinx.coroutines.delay

@Composable
fun QueueDetailScreen(
    viewModel: QueueDetailViewModel,
    onBack: () -> Unit,
    onCancelled: () -> Unit
) {
    var loading    by remember { mutableStateOf(true) }
    var details    by remember { mutableStateOf<QueueDetailsResponse?>(null) }
    var errorMsg   by remember { mutableStateOf<String?>(null) }
    var cancelling by remember { mutableStateOf(false) }
    var showConfirmCancel by remember { mutableStateOf(false) }

    // Auto-poll every 10 seconds for live position updates
    LaunchedEffect(Unit) {
        while (true) {
            viewModel.loadDetails { d, err ->
                details  = d
                errorMsg = if (d == null) err else null
                loading  = false
            }
            delay(10_000)
        }
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

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text       = "⬅ My Queue Status",
                    fontSize   = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color      = QmeDarkBlue
                )
            }

            QmeSpacer(16)

            when {
                loading  -> QmeLoader()
                errorMsg != null && details == null -> {
                    QmeErrorBanner(errorMsg!!)
                    QmeSpacer(12)
                    Text(
                        "No active queue found.",
                        fontSize = 14.sp,
                        color    = QmeSubtext
                    )
                }
                details != null -> {
                    val d   = details!!
                    val org = d.organization

                    // ── Org info card ──
                    QmeInfoCard {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier              = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment     = Alignment.CenterVertically
                            ) {
                                StatusBadge(org.status)
                                if (!org.openingHours.isNullOrBlank()) {
                                    Text(
                                        "${org.openingHours} – ${org.closingHours}",
                                        fontSize = 12.sp,
                                        color    = QmeSubtext
                                    )
                                }
                            }
                            HorizontalDivider(color = QmeBlue.copy(alpha = 0.2f))
                            Text(
                                "Queue Name: ${org.name}",
                                fontWeight = FontWeight.SemiBold,
                                color      = QmeOnSurface
                            )
                            if (!org.location.isNullOrBlank()) {
                                Text("Location: ${org.location}", fontSize = 13.sp, color = QmeSubtext)
                            }
                            Text("Code: ${org.queueCode}", fontSize = 13.sp, color = QmeSubtext)
                        }
                    }

                    QmeSpacer(16)

                    // ── Position card ──
                    Column(
                        modifier              = Modifier
                            .fillMaxWidth()
                            .background(QmeSky, RoundedCornerShape(14.dp))
                            .padding(20.dp),
                        horizontalAlignment   = Alignment.CenterHorizontally,
                        verticalArrangement   = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text       = "YOUR POSITION",
                            fontSize   = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color      = QmeSubtext,
                            letterSpacing = 1.sp
                        )

                        Text(
                            text       = "${d.positionInLine}",
                            fontSize   = 56.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color      = QmeBlue
                        )

                        val ahead = (d.positionInLine - 1).coerceAtLeast(0)
                        Text(
                            "$ahead ${if (ahead == 1) "person" else "people"} ahead",
                            fontSize  = 13.sp,
                            color     = QmeSubtext
                        )

                        Text(
                            "STATUS WAITING ⬇",
                            fontSize   = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color      = QmeOnSurface
                        )

                        // Status chips
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            d.currentlyServingNumber?.let {
                                StatusChip("Currently Serving: #$it", QmeSky)
                            }
                            StatusChip("Your Number: #${d.queueNumber}", QmeBlue, QmeWhite)
                        }

                        HorizontalDivider(color = QmeBlue.copy(alpha = 0.2f))

                        // Wait time and join time
                        d.estimatedWaitTime?.let {
                            Text(
                                "Estimated Wait: $it min",
                                fontSize   = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color      = QmeOnSurface
                            )
                        }
                        d.joinedAt?.let {
                            val formatted = it.take(16).replace("T", " at ")
                            Text("Joined $formatted", fontSize = 12.sp, color = QmeSubtext)
                        }
                    }

                    QmeSpacer(20)

                    // ── Cancel button ──
                    Button(
                        onClick  = { showConfirmCancel = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape    = RoundedCornerShape(10.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = QmeError),
                        enabled  = !cancelling
                    ) {
                        Text(
                            text       = if (cancelling) "Cancelling…" else "Cancel Queue",
                            color      = QmeWhite,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            QmeSpacer(24)
        }
    }

    // ── Confirm Cancel dialog ──
    if (showConfirmCancel && details != null) {
        AlertDialog(
            onDismissRequest = { showConfirmCancel = false },
            title = { Text("Cancel Queue?", fontWeight = FontWeight.Bold) },
            text  = { Text("Are you sure you want to leave the queue? Your spot will be lost.") },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmCancel = false
                        cancelling = true
                        viewModel.cancelQueue(details!!.entryId) { success, err ->
                            cancelling = false
                            if (success) onCancelled() else errorMsg = err
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = QmeError)
                ) {
                    Text("Yes, Cancel", color = QmeWhite)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmCancel = false }) {
                    Text("Keep My Spot")
                }
            }
        )
    }
}

@Composable
private fun StatusChip(label: String, background: androidx.compose.ui.graphics.Color, textColor: androidx.compose.ui.graphics.Color = QmeDarkBlue) {
    Box(
        modifier = Modifier
            .background(background.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = textColor)
    }
}

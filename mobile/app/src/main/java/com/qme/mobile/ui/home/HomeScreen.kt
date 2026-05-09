package com.qme.mobile.ui.home

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
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qme.mobile.data.model.response.QueueDetailsResponse
import com.qme.mobile.ui.components.QmeInfoCard
import com.qme.mobile.ui.components.QmeLoader
import com.qme.mobile.ui.components.QmePrimaryButton
import com.qme.mobile.ui.components.QmeSpacer
import com.qme.mobile.ui.components.QmeTopBar
import com.qme.mobile.ui.components.StatusBadge
import com.qme.mobile.ui.theme.QmeBlue
import com.qme.mobile.ui.theme.QmeDarkBlue
import com.qme.mobile.ui.theme.QmeOnSurface
import com.qme.mobile.ui.theme.QmeSky
import com.qme.mobile.ui.theme.QmeSubtext

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    userName: String,
    onJoinQueue: () -> Unit,
    onViewQueueDetail: (String) -> Unit   // entryId
) {
    var loading by remember { mutableStateOf(true) }
    var queueDetails by remember { mutableStateOf<QueueDetailsResponse?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadMyQueue { details, _ ->
            queueDetails = details
            loading = false
        }
    }

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
                text       = "Welcome, ${userName.split(" ").firstOrNull() ?: userName}!",
                fontSize   = 20.sp,
                fontWeight = FontWeight.Bold,
                color      = QmeDarkBlue
            )

            QmeSpacer(20)

            if (loading) {
                QmeLoader()
            } else if (queueDetails != null) {
                // ── Active queue card ──
                val org = queueDetails!!.organization
                ActiveQueueCard(
                    details       = queueDetails!!,
                    orgName       = org.name,
                    orgStatus     = org.status,
                    onClick       = { onViewQueueDetail(queueDetails!!.entryId) }
                )
            } else {
                // ── Empty state ──
                QmeInfoCard {
                    Column(
                        modifier              = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                        horizontalAlignment   = Alignment.CenterHorizontally,
                        verticalArrangement   = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text       = "No active queues",
                            fontSize   = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color      = QmeSubtext
                        )
                        QmePrimaryButton(
                            text    = "Join a Queue",
                            onClick = onJoinQueue,
                            modifier = Modifier.fillMaxWidth(0.6f)
                        )
                    }
                }
            }

            QmeSpacer(24)
        }
    }
}

@Composable
private fun ActiveQueueCard(
    details: QueueDetailsResponse,
    orgName: String,
    orgStatus: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(QmeSky, RoundedCornerShape(14.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text(orgName, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = QmeOnSurface)
            StatusBadge(orgStatus)
        }
        HorizontalDivider(color = QmeBlue.copy(alpha = 0.15f))
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            QueueStat("Your #", "#${details.queueNumber}")
            QueueStat("Position", "${details.positionInLine}")
            QueueStat("Status", details.computedStatus.replace("_", " "))
        }
        QmePrimaryButton(text = "View My Queue", onClick = onClick)
    }
}

@Composable
private fun QueueStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = QmeBlue)
        Text(label, fontSize = 11.sp, color = QmeSubtext)
    }
}

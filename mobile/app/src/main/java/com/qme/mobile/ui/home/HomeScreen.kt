package com.qme.mobile.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import com.qme.mobile.ui.components.OrgAvatar
import com.qme.mobile.ui.components.QmeInfoCard
import com.qme.mobile.ui.components.QmeLoader
import com.qme.mobile.ui.components.QmePrimaryButton
import com.qme.mobile.ui.components.QmeSpacer
import com.qme.mobile.ui.components.QmeTopBar
import com.qme.mobile.ui.theme.QmeBlue
import com.qme.mobile.ui.theme.QmeDarkBlue
import com.qme.mobile.ui.theme.QmeOnSurface
import com.qme.mobile.ui.theme.QmeSky
import com.qme.mobile.ui.theme.QmeSubtext
import com.qme.mobile.ui.theme.QmeSuccess
import com.qme.mobile.ui.theme.QmeSuccessBg

@Composable

fun HomeScreen(

    viewModel: HomeViewModel,
    userName: String,
    onJoinQueue: () -> Unit,
    onViewQueueDetail: (String) -> Unit

) {

    var loading      by remember { mutableStateOf(true) }
    var queueDetails by remember { mutableStateOf<QueueDetailsResponse?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadMyQueue { details, _ ->
            queueDetails = details
            loading      = false
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
            val firstName = userName.split(" ").firstOrNull() ?: userName
            Text(
                text       = "Hi, $firstName! 👋",
                fontSize   = 22.sp,
                fontWeight = FontWeight.Bold,
                color      = QmeDarkBlue
            )
            Text(
                text     = "Here's your queue status",
                fontSize = 13.sp,
                color    = QmeSubtext
            )
            QmeSpacer(20)
            if (loading) {
                QmeLoader()
            } else if (queueDetails != null) {
                ActiveQueueCard(
                    details = queueDetails!!,
                    onClick = { onViewQueueDetail(queueDetails!!.entryId) }
                )
            } else {
                // Empty state
                QmeInfoCard {
                    Column(
                        modifier              = Modifier.fillMaxWidth().padding(vertical = 28.dp),
                        horizontalAlignment   = Alignment.CenterHorizontally,
                        verticalArrangement   = Arrangement.spacedBy(14.dp)
                    ) {
                        Text("🎫", fontSize = 40.sp)
                        Text(
                            "No active queue",
                            fontSize   = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color      = QmeDarkBlue
                        )
                        Text(
                            "Join a queue using a 6-digit code",
                            fontSize = 13.sp,
                            color    = QmeSubtext

                        )
                        QmePrimaryButton(
                            text     = "+ Join a Queue",
                            onClick  = onJoinQueue,
                            modifier = Modifier.fillMaxWidth(0.7f)
                        )
                    }
                }
            }
            QmeSpacer(24)
        }
    }
}

@Composable

private fun ActiveQueueCard(details: QueueDetailsResponse, onClick: () -> Unit) {

    val org           = details.organization
    val isBeingServed = details.computedStatus == "BEING_SERVED"
    val isNext        = details.positionInLine == 1

    val statusLabel   = when {
        isBeingServed -> "Serving"
        isNext        -> "Your Turn"
        else          -> "Waiting"
    }
    val cardBg = if (isBeingServed || isNext) QmeSuccessBg else QmeSky
    val accent = if (isBeingServed || isNext) QmeSuccess else QmeBlue

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(cardBg, RoundedCornerShape(16.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Org avatar + name row
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OrgAvatar(orgName = org.name, photoBase64 = org.photo, size = 52.dp)
            Column {
                Text(org.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = QmeOnSurface)
                Text(statusLabel, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = accent)
            }
        }
        HorizontalDivider(color = accent.copy(alpha = 0.2f))
        // Stats row
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            QueueStat("Your #", "#${details.queueNumber}", accent)
            QueueStat("Position", "${details.positionInLine}", accent)
            details.estimatedWaitTime?.let {
                QueueStat("Est. Wait", "~$it min", accent)
            }
        }
        QmePrimaryButton(
            text           = "View My Queue",
            onClick        = onClick,
            containerColor = accent
        )
    }
}

@Composable

private fun QueueStat(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = color)
        Text(label, fontSize = 11.sp, color = QmeSubtext)
    }
}

 
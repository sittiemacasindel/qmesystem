package com.qme.mobile.ui.history
 
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qme.mobile.data.model.response.HistoryEntryResponse
import com.qme.mobile.ui.components.OrgAvatar
import com.qme.mobile.ui.components.QmeErrorBanner
import com.qme.mobile.ui.components.QmeLoader
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
fun HistoryScreen(viewModel: HistoryViewModel) {
    var loading  by remember { mutableStateOf(true) }
    var history  by remember { mutableStateOf<List<HistoryEntryResponse>>(emptyList()) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
 
    LaunchedEffect(Unit) {
        viewModel.loadHistory { list, err ->
            history  = list ?: emptyList()
            errorMsg = err
            loading  = false
        }
    }
 
    Scaffold(topBar = { QmeTopBar() }) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
        ) {
            QmeSpacer(16)
            Text("Queue History", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = QmeDarkBlue)
            QmeSpacer(4)
            Text("${history.size} entries", fontSize = 13.sp, color = QmeSubtext)
            QmeSpacer(14)
 
            when {
                loading      -> QmeLoader()
                errorMsg != null -> QmeErrorBanner(errorMsg!!)
                history.isEmpty() -> {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("📋", fontSize = 40.sp)
                        Text("No history yet", fontSize = 15.sp, color = QmeSubtext)
                    }
                }
                else -> {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(history) { entry -> HistoryCard(entry) }
                    }
                }
            }
        }
    }
}
 
@Composable
private fun HistoryCard(entry: HistoryEntryResponse) {
    val (statusColor, statusBg) = when (entry.status) {
        "SERVED"    -> QmeSuccess to QmeSuccessBg
        "CANCELLED" -> Color(0xFFC62828) to Color(0xFFFFEBEE)
        "SKIPPED"   -> Color(0xFFF57C00) to Color(0xFFFFF3E0)
        else        -> QmeSubtext to QmeSky
    }
 
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(QmeSky, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OrgAvatar(orgName = entry.organizationName, size = 48.dp)
 
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    entry.organizationName,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 14.sp,
                    color      = QmeOnSurface,
                    modifier   = Modifier.weight(1f)
                )
                Text(
                    text       = entry.status,
                    fontSize   = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color      = statusColor,
                    modifier   = Modifier
                        .background(statusBg, RoundedCornerShape(20.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
            HorizontalDivider(color = QmeBlue.copy(alpha = 0.12f))
            Text("Queue #${entry.queueNumber}  ·  ${entry.queueCode}", fontSize = 12.sp, color = QmeSubtext)
            entry.joinedAt?.let {
                Text("Joined: ${it.take(16).replace("T", " ")}", fontSize = 11.sp, color = QmeSubtext)
            }
        }
    }
}
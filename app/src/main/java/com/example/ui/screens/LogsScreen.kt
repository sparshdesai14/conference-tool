package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.BookingEntity
import com.example.data.LogEntity
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun LogsScreen(
    allLogs: List<LogEntity>,
    viewModel: com.example.viewmodel.BookingViewModel,
    onLogActionRequested: (BookingEntity) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilterDay by remember { mutableStateOf("all") } // "all", "today"

    val todayDateString = remember {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        sdf.format(Date())
    }

    val filteredLogs = remember(allLogs, searchQuery, selectedFilterDay) {
        var resultList = allLogs

        // Day Filtering
        if (selectedFilterDay == "today") {
            resultList = resultList.filter { it.dateString == todayDateString }
        }

        // Search Query Filtering
        if (searchQuery.trim().isNotEmpty()) {
            val q = searchQuery.lowercase().trim()
            resultList = resultList.filter { log ->
                log.department.lowercase().contains(q) ||
                log.bookedBy.lowercase().contains(q) ||
                log.action.lowercase().contains(q) ||
                log.meetingSubject.lowercase().contains(q) ||
                log.senderEmail.lowercase().contains(q) ||
                log.attendees.lowercase().contains(q)
            }
        }
        resultList
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Logs Header Title
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = "📜", fontSize = 22.sp)
                Text(
                    text = "Activity Log",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            Text(
                text = "Trace historical booking operations and calendar releases",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Search Input text field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("🔍 Search meeting, dept, name...", fontSize = 13.sp) },
            shape = RoundedCornerShape(22.dp),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("log_search_input"),
            trailingIcon = {
                Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            )
        )

        // Filter Bar chips (Today vs All)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, SleekOutline.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White)
        ) {
            listOf("all" to "All History", "today" to "Today Checked").forEach { (key, label) ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                        .background(if (selectedFilterDay == key) SleekPrimary else Color.Transparent)
                        .clickable { selectedFilterDay = key }
                        .testTag("log_filter_$key"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedFilterDay == key) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Historical listing stack
        if (filteredLogs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("📜", fontSize = 42.sp)
                    Text(
                        text = "No history records match",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Bookings or releases will log records here",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("logs_feed"),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredLogs) { log ->
                    // Styles matching action type
                    val isGoodAction = log.action.startsWith("Booked") || 
                                       log.action.startsWith("Sched") || 
                                       log.action.startsWith("Ext") || 
                                       log.action.startsWith("Auto") || 
                                       log.action.startsWith("Inv")
                    
                    val actionColor = if (isGoodAction) StatusFreeGreen else StatusBusyRed

                    val isGyaan = log.roomName.lowercase() == "gyaan"
                    val roomLabel = if (isGyaan) "Gyaan" else "Dharma"

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, SleekOutline.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // First row: Room badge + Dept title + Action type
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (isGyaan) SleekPrimary.copy(alpha = 0.08f) else Color(0xFF6264A7).copy(alpha = 0.08f)
                                    ) {
                                        Text(
                                            text = roomLabel,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (isGyaan) SleekPrimary else Color(0xFF6264A7),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }

                                    Text(
                                        text = log.department,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Text(
                                    text = log.action,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = actionColor
                                )
                            }

                            // Meeting subject description
                            if (log.meetingSubject.isNotEmpty()) {
                                Text(
                                    text = log.meetingSubject,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Sub-chips with booker information and quick action invite buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = "👤 ${log.bookedBy.ifEmpty { "System Action" }}",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                    )
                                    if (log.senderEmail.isNotEmpty()) {
                                        Text(
                                            text = "📥 ${log.senderEmail}",
                                            fontSize = 9.sp,
                                            color = StatusFreeGreen,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }

                                // Quick Actions from log logic: reopen invite/calendar blocks
                                val isReopenable = log.action.contains("Booked") || log.action.contains("Scheduled") || log.action.contains("Auto-started")
                                if (isReopenable) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        // Mail Quick action
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(SleekPrimaryContainer)
                                                .clickable {
                                                    // Parse back context stub to reuse sharing details dialog
                                                    val bookingStub = BookingEntity(
                                                        id = "log_${log.id}",
                                                        roomName = log.roomName,
                                                        department = log.department,
                                                        meetingSubject = log.meetingSubject,
                                                        bookedBy = log.bookedBy,
                                                        startTime = log.timestamp,
                                                        endTime = 0L,
                                                        duration = log.duration ?: "30 min",
                                                        isScheduled = false,
                                                        createdAt = log.timestamp,
                                                        senderEmail = log.senderEmail,
                                                        attendees = log.attendees
                                                    )
                                                    onLogActionRequested(bookingStub)
                                                }
                                                .padding(horizontal = 6.dp, vertical = 3.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Icon(imageVector = Icons.Default.Email, contentDescription = null, modifier = Modifier.size(10.dp), tint = SleekPrimary)
                                                Text("Mail", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = SleekPrimary)
                                            }
                                        }

                                        // Calendar/Microsoft Teams quick launcher
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color(0xFFEDE8F5))
                                                .clickable {
                                                    val bookingStub = BookingEntity(
                                                        id = "log_${log.id}",
                                                        roomName = log.roomName,
                                                        department = log.department,
                                                        meetingSubject = log.meetingSubject,
                                                        bookedBy = log.bookedBy,
                                                        startTime = log.timestamp,
                                                        endTime = 0L,
                                                        duration = log.duration ?: "30 min",
                                                        isScheduled = false,
                                                        createdAt = log.timestamp,
                                                        senderEmail = log.senderEmail,
                                                        attendees = log.attendees
                                                    )
                                                    onLogActionRequested(bookingStub)
                                                }
                                                .padding(horizontal = 6.dp, vertical = 3.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Icon(imageVector = Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(10.dp), tint = Color(0xFF6264A7))
                                                Text("Teams", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6264A7))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

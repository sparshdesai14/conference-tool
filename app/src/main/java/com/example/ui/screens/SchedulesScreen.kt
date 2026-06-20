package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.BookingEntity
import com.example.ui.theme.*

@Composable
fun SchedulesScreen(
    scheduledBookings: List<BookingEntity>,
    viewModel: com.example.viewmodel.BookingViewModel,
    onInviteRequested: (BookingEntity) -> Unit
) {
    val context = LocalContext.current
    val listSchedules = remember(scheduledBookings) {
        scheduledBookings.sortedBy { it.startTime }
    }

    var pendingCancelBooking by remember { mutableStateOf<BookingEntity?>(null) }
    var cancellationReasonText by remember { mutableStateOf("") }

    var selectedRoomFilter by remember { mutableStateOf("all") } // "all", "gyaan", "dharma"

    val displayedSchedules = remember(listSchedules, selectedRoomFilter) {
        if (selectedRoomFilter == "all") listSchedules
        else listSchedules.filter { it.roomName.lowercase() == selectedRoomFilter }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Timeline Header
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = "📅", fontSize = 22.sp)
                Text(
                    text = "Upcoming Schedules",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            Text(
                text = "Review all future conference slot bookings chronologically",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Room Filter selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, SleekOutline.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White)
        ) {
            listOf("all" to "Both Rooms", "gyaan" to "Gyaan", "dharma" to "Dharma").forEach { (key, label) ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                        .background(if (selectedRoomFilter == key) SleekPrimary else Color.Transparent)
                        .clickable { selectedRoomFilter = key }
                        .testTag("sched_filter_$key"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedRoomFilter == key) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Scrollable timeline of schedules
        if (displayedSchedules.isEmpty()) {
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
                    Text("📅", fontSize = 42.sp)
                    Text(
                        text = "No upcoming schedules found",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Switch back to Home tab to schedule a reservation",
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
                    .testTag("schedules_list"),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(displayedSchedules) { schedule ->
                    val isGyaan = schedule.roomName.lowercase() == "gyaan"
                    val roomLabel = if (isGyaan) "Gyaan (2nd Fl)" else "Dharma (1st Fl)"

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, StatusSchedAmberBorder.copy(alpha = 0.2f), RoundedCornerShape(14.dp)),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = StatusSchedAmberBg)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (isGyaan) SleekPrimary else Color(0xFF6264A7)
                                    ) {
                                        Text(
                                            text = roomLabel,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Text(
                                        text = schedule.department,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                if (schedule.meetingSubject.isNotEmpty()) {
                                    Text(
                                        text = schedule.meetingSubject,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = "Booked by: ${schedule.bookedBy}",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                                Text(
                                    text = "${viewModel.fmtDate(schedule.startTime)} · ${viewModel.fmtTime(schedule.startTime)} – ${viewModel.fmtTime(schedule.endTime)} (${schedule.duration})",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = StatusSchedAmber,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                // Invite Trigger
                                IconButton(
                                    onClick = { onInviteRequested(schedule) },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.White)
                                        .border(1.dp, SleekOutline.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Email,
                                        contentDescription = "Invite Attendees",
                                        tint = SleekPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                // Cancel Schedule
                                IconButton(
                                    onClick = { pendingCancelBooking = schedule },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.White)
                                        .border(1.dp, StatusBusyRed.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info, // Modern placeholder for cross delete
                                        contentDescription = "Cancel",
                                        tint = StatusBusyRed,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Cancellation prompt popup dialog box
    if (pendingCancelBooking != null) {
        AlertDialog(
            onDismissRequest = { pendingCancelBooking = null; cancellationReasonText = "" },
            title = { Text("Cancel scheduled booking?", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Reason for cancellation is required:", fontSize = 13.sp)
                    OutlinedTextField(
                        value = cancellationReasonText,
                        onValueChange = { cancellationReasonText = it },
                        placeholder = { Text("e.g. reschedule requested...") },
                        modifier = Modifier.fillMaxWidth().testTag("sched_cancel_reason"),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (cancellationReasonText.trim().isEmpty()) {
                            Toast.makeText(context, "Cancellation reason is required!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        viewModel.cancelScheduledBooking(
                            booking = pendingCancelBooking!!,
                            reason = cancellationReasonText,
                            onSuccess = {
                                Toast.makeText(context, "Schedule cancelled successfully", Toast.LENGTH_SHORT).show()
                                pendingCancelBooking = null
                                cancellationReasonText = ""
                            },
                            onFailure = { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusBusyRed)
                ) {
                    Text("Confirm Cancel", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingCancelBooking = null; cancellationReasonText = "" }) {
                    Text("Keep Booking")
                }
            }
        )
    }
}

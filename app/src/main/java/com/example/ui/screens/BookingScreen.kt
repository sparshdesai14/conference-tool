package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.BookingEntity
import com.example.ui.components.RoomCard
import com.example.ui.components.TickerBar
import com.example.ui.theme.*

@Composable
fun BookingScreen(
    activeBookings: List<BookingEntity>,
    scheduledBookings: List<BookingEntity>,
    viewModel: com.example.viewmodel.BookingViewModel,
    onInviteRequested: (BookingEntity) -> Unit
) {
    var showAboutPanel by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Ticker Bar
        TickerBar(
            activeBookings = activeBookings,
            scheduledBookings = scheduledBookings,
            viewModel = viewModel
        )

        // Title and Subtitle Row with About Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = "🏢", fontSize = 22.sp)
                    Text(
                        text = "Conference Booking",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                Text(
                    text = "Room availability board · Advait Group (AETL & AGPL)",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = { showAboutPanel = !showAboutPanel },
                colors = ButtonDefaults.buttonColors(
                    containerColor = SleekPrimaryContainer,
                    contentColor = SleekPrimary
                ),
                shape = RoundedCornerShape(20.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier
                    .height(32.dp)
                    .testTag("about_toggle_btn")
            ) {
                Text("ⓘ About", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        // About expand panel
        AnimatedVisibility(visible = showAboutPanel) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("about_panel"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "About Conference Room Booking",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = SleekPrimary
                        )

                        Text(
                            text = "✕ Close",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .clickable { showAboutPanel = false }
                                .testTag("about_close_btn")
                        )
                    }

                    Divider(color = SleekPrimary.copy(alpha = 0.2f))

                    Text(
                        text = "Real-time scheduling for Advait Group's conference rooms — synchronized on local storage.",
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("•", fontWeight = FontWeight.Bold, color = SleekPrimary)
                            Text("🧠 Gyaan — 2nd Floor · Capacity 12", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("•", fontWeight = FontWeight.Bold, color = SleekPrimary)
                            Text("⚖️ Dharma — 1st Floor · Capacity 14", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }

                    Text(
                        text = "Book Now instantly occupies the room. Schedule Ahead puts a reservation in the queue. Future schedules autostart at their designated times.",
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Corporate HR Sneda Christian
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(SleekPrimaryContainer)
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(SleekPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "HR",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Column {
                            Text(
                                "Sneha Christian",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = SleekOnPrimaryContainer
                            )
                            Text(
                                "Corporate — HR Coordinator",
                                fontSize = 10.sp,
                                color = SleekPrimary
                            )
                        }
                    }
                }
            }
        }

        // Room Grid Column / Stack (Phone responsive layout)
        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // GYAAN CARD
            RoomCard(
                roomKey = "gyaan",
                roomName = "Gyaan Conference Room",
                roomIcon = "🧠",
                location = "📍 2nd Floor",
                capacity = "👥 Capacity: 12",
                activeBookings = activeBookings,
                scheduledBookings = scheduledBookings,
                viewModel = viewModel,
                onInviteRequested = onInviteRequested
            )

            // DHARMA CARD
            RoomCard(
                roomKey = "dharma",
                roomName = "Dharma Conference Room",
                roomIcon = "⚖️",
                location = "📍 1st Floor",
                capacity = "👥 Capacity: 14",
                activeBookings = activeBookings,
                scheduledBookings = scheduledBookings,
                viewModel = viewModel,
                onInviteRequested = onInviteRequested
            )
        }
    }
}

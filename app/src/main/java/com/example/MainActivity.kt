package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.BookingEntity
import com.example.ui.components.InviteDialog
import com.example.ui.screens.BookingScreen
import com.example.ui.screens.LogsScreen
import com.example.ui.screens.SchedulesScreen
import com.example.ui.theme.*
import com.example.viewmodel.BookingViewModel

class MainActivity : ComponentActivity() {

    private val bookingViewModel: BookingViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Supports notch rendering & safe areas natively
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                var selectedTab by remember { mutableIntStateOf(0) }
                var activeSharingInviteBooking by remember { mutableStateOf<BookingEntity?>(null) }

                // Safe UI lifecycle flow collection
                val activeBookings by bookingViewModel.activeBookings.collectAsState()
                val scheduledBookings by bookingViewModel.scheduledBookings.collectAsState()
                val allLogs by bookingViewModel.allLogs.collectAsState()

                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.safeDrawing),
                    topBar = {
                        Column {
                            // Top Banner App Bar
                            Surface(
                                color = Color.White,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(60.dp),
                                shadowElevation = 1.dp
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(SleekPrimaryContainer),
                                            contentAlignment = androidx.compose.ui.Alignment.Center
                                        ) {
                                            Text(text = "🏢", fontSize = 18.sp)
                                        }
                                        Text(
                                            text = "Advait Booking Center",
                                            fontSize = 17.sp,
                                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                            color = SleekOnSecondaryContainer
                                        )
                                    }

                                    // Material 3 stylized Profiler Circle Avatar representing Sleek design
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(androidx.compose.foundation.shape.CircleShape)
                                            .border(1.dp, SleekOutline, androidx.compose.foundation.shape.CircleShape)
                                            .background(
                                                androidx.compose.ui.graphics.Brush.linearGradient(
                                                    colors = listOf(SleekPrimary, SleekPrimaryContainer)
                                                )
                                            )
                                    )
                                }
                            }
                        }
                    },
                    bottomBar = {
                        // Custom Navigation Bar matching the exact Tailwind structure of 'Sleek Interface' with h-24 height of gesture safety
                        NavigationBar(
                            containerColor = SleekSecondaryContainer,
                            contentColor = SleekOnSecondaryContainer,
                            tonalElevation = 2.dp,
                            modifier = Modifier
                                .height(68.dp)
                                .border(1.dp, SleekOutline.copy(alpha = 0.2f), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                                .testTag("main_navigation_bar")
                        ) {
                            // Tab 0: Home / Room Reservation Grid Board
                            NavigationBarItem(
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                icon = { Icon(imageVector = Icons.Default.Home, contentDescription = "Booking") },
                                label = { Text("Booking", fontSize = 11.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = SleekOnPrimaryContainer,
                                    selectedTextColor = SleekOnPrimaryContainer,
                                    indicatorColor = SleekPrimaryContainer,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                ),
                                modifier = Modifier.testTag("nav_tab_booking")
                            )

                            // Tab 1: Detailed chronological timelines of future schedules
                            NavigationBarItem(
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                icon = { Icon(imageVector = Icons.Default.DateRange, contentDescription = "Schedules") },
                                label = { Text("Schedules", fontSize = 11.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = SleekOnPrimaryContainer,
                                    selectedTextColor = SleekOnPrimaryContainer,
                                    indicatorColor = SleekPrimaryContainer,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                ),
                                modifier = Modifier.testTag("nav_tab_schedules")
                            )

                            // Tab 2: Activity Log trace feeds
                            NavigationBarItem(
                                selected = selectedTab == 2,
                                onClick = { selectedTab = 2 },
                                icon = { Icon(imageVector = Icons.Default.List, contentDescription = "History Logs") },
                                label = { Text("Activity Log", fontSize = 11.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = SleekOnPrimaryContainer,
                                    selectedTextColor = SleekOnPrimaryContainer,
                                    indicatorColor = SleekPrimaryContainer,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                ),
                                modifier = Modifier.testTag("nav_tab_history")
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .background(SleekBackground)
                    ) {
                        when (selectedTab) {
                            0 -> BookingScreen(
                                activeBookings = activeBookings,
                                scheduledBookings = scheduledBookings,
                                viewModel = bookingViewModel,
                                onInviteRequested = { activeSharingInviteBooking = it }
                            )
                            1 -> SchedulesScreen(
                                scheduledBookings = scheduledBookings,
                                viewModel = bookingViewModel,
                                onInviteRequested = { activeSharingInviteBooking = it }
                            )
                            2 -> LogsScreen(
                                allLogs = allLogs,
                                viewModel = bookingViewModel,
                                onLogActionRequested = { activeSharingInviteBooking = it }
                            )
                        }
                        
                        // Sharing calendar overlays
                        if (activeSharingInviteBooking != null) {
                            InviteDialog(
                                booking = activeSharingInviteBooking!!,
                                viewModelHelper = bookingViewModel,
                                onDismiss = { activeSharingInviteBooking = null }
                            )
                        }
                    }
                }
            }
        }
    }
}

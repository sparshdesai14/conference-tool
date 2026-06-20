package com.example.ui.components

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.BookingEntity
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun RoomCard(
    roomKey: String, // "gyaan" or "dharma"
    roomName: String,
    roomIcon: String,
    location: String,
    capacity: String,
    activeBookings: List<BookingEntity>,
    scheduledBookings: List<BookingEntity>,
    viewModel: com.example.viewmodel.BookingViewModel,
    onInviteRequested: (BookingEntity) -> Unit
) {
    val context = LocalContext.current
    val now = viewModel.currentTime.collectAsState().value

    // Current room active/sched items
    val activeBooking = activeBookings.firstOrNull { it.roomName.lowercase() == roomKey }
    val roomSchedules = scheduledBookings.filter { it.roomName.lowercase() == roomKey }
        .sortedBy { it.startTime }

    // Draft User states
    var selectedDept by remember { mutableStateOf("") }
    var meetingSubject by remember { mutableStateOf("") }
    var bookedByName by remember { mutableStateOf("") }
    var activeTabMode by remember { mutableStateOf("now") } // "now" or "sched"
    var showDeptDropdown by remember { mutableStateOf(false) }

    // Validation trigger
    var isSubmittedMissingName by remember { mutableStateOf(false) }

    // Booking Options states
    var showBookNowDurationBox by remember { mutableStateOf(false) }
    var selectedDurationOption by remember { mutableStateOf("30 min") }
    var customDurationInput by remember { mutableStateOf("") }

    // Scheduling Options states
    var showSchedPanel by remember { mutableStateOf(false) }
    var schedDate by remember { mutableStateOf("") }
    var schedTime by remember { mutableStateOf("") }
    var schedDurationOption by remember { mutableStateOf("1 hr") }
    var schedCustomDurationInput by remember { mutableStateOf("") }

    // Extending states
    var showExtendPanel by remember { mutableStateOf(false) }
    var extendDurationOption by remember { mutableStateOf("30 min") }
    var extendCustomDurationInput by remember { mutableStateOf("") }
    var extendReasonText by remember { mutableStateOf("") }

    // Cancellation states
    var pendingCancelBooking by remember { mutableStateOf<BookingEntity?>(null) }
    var cancellationReasonText by remember { mutableStateOf("") }

    // Next scheduled conflict calculator
    val nextSchedule = roomSchedules.firstOrNull { it.startTime > now }

    // Pulse animation for Busy indicator
    val transition = rememberInfiniteTransition(label = "breathing")
    val alphaPulse by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    // Calendar & dialog pre-fills
    val calendarInstance = Calendar.getInstance()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SleekOutline.copy(alpha = 0.4f), RoundedCornerShape(24.dp))
            .testTag("room_card_$roomKey"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Room Header Layout
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(SleekPrimaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = roomIcon, fontSize = 22.sp, modifier = Modifier.testTag("room_icon_$roomKey"))
                    }
                    Column {
                        Text(
                            text = roomName,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.testTag("room_name_$roomKey")
                        )
                        Text(
                            text = location,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = SleekSecondary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(SleekPrimaryContainer.copy(alpha = 0.5f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = capacity,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = SleekOnPrimaryContainer
                            )
                        }
                    }
                }

                // Status Badge
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = when {
                        activeBooking != null -> StatusBusyRedBg
                        nextSchedule != null -> StatusSchedAmberBg
                        else -> StatusFreeGreenBg
                    },
                    modifier = Modifier.testTag("status_badge_$roomKey")
                ) {
                    Text(
                        text = when {
                            activeBooking != null -> "Occupied"
                            nextSchedule != null -> "Scheduled"
                            else -> "Available"
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            activeBooking != null -> StatusBusyRed
                            nextSchedule != null -> StatusSchedAmber
                            else -> StatusFreeGreen
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }

            // Info indicator sentence row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(vertical = 8.dp, horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .alpha(if (activeBooking != null) alphaPulse else 1.0f)
                        .clip(CircleShape)
                        .background(
                            when {
                                activeBooking != null -> StatusBusyRed
                                nextSchedule != null -> StatusSchedAmber
                                else -> StatusFreeGreen
                            }
                        )
                )

                Text(
                    text = when {
                        activeBooking != null -> {
                            val subjectText = if (activeBooking.meetingSubject.isNotEmpty()) " · ${activeBooking.meetingSubject}" else ""
                            val durationText = if (activeBooking.duration.isNotEmpty()) " · ${activeBooking.duration}" else ""
                            val timeRemaining = if (activeBooking.endTime > 0L) {
                                " · ${viewModel.fmtRem(activeBooking.endTime - now)} left"
                            } else " · ∞ left"
                            "Booked · ${activeBooking.department}$subjectText$durationText$timeRemaining"
                        }
                        nextSchedule != null -> {
                            val meetingSub = if (nextSchedule.meetingSubject.isNotEmpty()) " · ${nextSchedule.meetingSubject}" else ""
                            val remainingStart = nextSchedule.startTime - now
                            "$roomName free until ${viewModel.fmtTime(nextSchedule.startTime)} · ${nextSchedule.department}$meetingSub in ${viewModel.fmtRem(remainingStart)}"
                        }
                        else -> "$roomName is free — no booking now."
                    },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("info_row_$roomKey")
                )
            }

            // Department Input dropdown spinner
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = selectedDept,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Select department") },
                    placeholder = { Text("Select department...") },
                    shape = RoundedCornerShape(8.dp),
                    trailingIcon = {
                        IconButton(onClick = { showDeptDropdown = !showDeptDropdown }) {
                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDeptDropdown = !showDeptDropdown }
                        .testTag("dept_select_$roomKey"),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    )
                )

                DropdownMenu(
                    expanded = showDeptDropdown,
                    onDismissRequest = { showDeptDropdown = false },
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(280.dp)
                ) {
                    com.example.viewmodel.BookingViewModel.DEPARTMENTS.forEach { dept ->
                        DropdownMenuItem(
                            text = { Text(dept, fontSize = 13.sp) },
                            onClick = {
                                selectedDept = dept
                                showDeptDropdown = false
                                // Reset sheets
                                showBookNowDurationBox = false
                                showSchedPanel = false
                            },
                            modifier = Modifier.testTag("dept_option_$dept")
                        )
                    }
                }
            }

            // Meeting Subject Text Field
            OutlinedTextField(
                value = meetingSubject,
                onValueChange = { if (it.length <= 60) meetingSubject = it },
                label = { Text("Meeting subject / name") },
                placeholder = { Text("Enter meeting subject...") },
                shape = RoundedCornerShape(8.dp),
                maxLines = 1,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("meeting_subject_$roomKey"),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                )
            )

            // Booked By Text field (your name, required)
            OutlinedTextField(
                value = bookedByName,
                onValueChange = {
                    bookedByName = it
                    if (it.isNotEmpty()) isSubmittedMissingName = false
                },
                label = { Text("Your name (booked by) *") },
                placeholder = { Text("Booker's full name...") },
                shape = RoundedCornerShape(8.dp),
                maxLines = 1,
                isError = isSubmittedMissingName,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("bookedby_$roomKey"),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    errorContainerColor = Color.Transparent
                )
            )

            // Tabs / Modes (Book now vs Sched ahead)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, SleekOutline.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White)
            ) {
                // Book Now button tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(32.dp)
                        .background(if (activeTabMode == "now") SleekPrimary else Color.Transparent)
                        .clickable {
                            activeTabMode = "now"
                            showBookNowDurationBox = false
                            showSchedPanel = false
                        }
                        .testTag("mode_now_$roomKey"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "⚡ Book Now",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (activeTabMode == "now") Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Sched Ahead Button tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(32.dp)
                        .background(if (activeTabMode == "sched") SleekPrimary else Color.Transparent)
                        .clickable {
                            activeTabMode = "sched"
                            showBookNowDurationBox = false
                            showSchedPanel = false
                        }
                        .testTag("mode_sched_$roomKey"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "📅 Schedule Ahead",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (activeTabMode == "sched") Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // PRIMARY BIG ACTION BUTTON
            val hasMatchedRelease = activeBooking != null && selectedDept == activeBooking.department
            Button(
                onClick = {
                    if (selectedDept.isEmpty()) {
                        Toast.makeText(context, "Select your department first!", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    if (bookedByName.trim().isEmpty()) {
                        isSubmittedMissingName = true
                        Toast.makeText(context, "Please enter your name in 'Booked by' field", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    if (hasMatchedRelease) {
                        // Release booking logic
                        viewModel.releaseBooking(
                            activeBooking!!,
                            onSuccess = {
                                Toast.makeText(context, "$roomName released successfully!", Toast.LENGTH_SHORT).show()
                                bookedByName = ""
                                meetingSubject = ""
                                showBookNowDurationBox = false
                                showExtendPanel = false
                            },
                            onFailure = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
                        )
                    } else if (activeTabMode == "sched") {
                        showSchedPanel = true
                        showBookNowDurationBox = false
                    } else {
                        // Open "Book Now" Duration sheets
                        showBookNowDurationBox = true
                        showSchedPanel = false
                    }
                },
                enabled = selectedDept.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (hasMatchedRelease) StatusBusyRed else SleekPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f)
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag("action_btn_$roomKey")
            ) {
                Text(
                    text = when {
                        selectedDept.isEmpty() -> "Select dept first"
                        hasMatchedRelease -> "Release — Done"
                        activeTabMode == "sched" -> "📅 Open Schedule Planner"
                        else -> "⚡ Book Room Now"
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            // ACTIVE BOOKING PROGRESS TIMER TRACK BAR
            AnimatedVisibility(visible = activeBooking != null) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "⏱ TIME REMAINING",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (activeBooking != null && activeBooking.endTime > 0L) {
                                viewModel.fmtRem(activeBooking.endTime - now)
                            } else "Until released (∞)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = SleekPrimary
                        )
                    }

                    // Simple percentage calculation
                    val progressValue = if (activeBooking != null && activeBooking.endTime > 0L) {
                        val total = activeBooking.endTime - activeBooking.startTime
                        val rem = activeBooking.endTime - now
                        if (total > 0L) (rem.toFloat() / total.toFloat()).coerceIn(0f, 1f) else 1f
                    } else 1f

                    val colorBar = when {
                        progressValue <= 0.1f -> StatusBusyRed
                        progressValue <= 0.25f -> StatusSchedAmber
                        else -> SleekPrimary
                    }

                    LinearProgressIndicator(
                        progress = progressValue,
                        color = colorBar,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                    )
                }
            }

            // Invite action button contextually active
            AnimatedVisibility(
                visible = activeBooking != null || roomSchedules.isNotEmpty()
            ) {
                OutlinedButton(
                    onClick = {
                        val targetedBooking = activeBooking ?: roomSchedules.first()
                        onInviteRequested(targetedBooking)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("invite_button_$roomKey"),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SleekPrimary)
                ) {
                    Text("📧 Invite Attendees & Block Calendar", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            // DURATION SELECTION DRAWER/PANEL (⚡ Book Now)
            AnimatedVisibility(visible = showBookNowDurationBox) {
                Surface(
                    color = SleekPrimaryContainer,
                    border = BorderStroke(1.dp, SleekPrimary.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (nextSchedule != null) {
                                "How long? (Free until ${viewModel.fmtTime(nextSchedule.startTime)})"
                            } else "How long do you need $roomName?",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = SleekOnPrimaryContainer
                        )

                        // Duration choices list
                        val presets = remember(nextSchedule) {
                            val allList = listOf(
                                "15 min", "30 min", "45 min", "1 hr", "1.5 hrs", "2 hrs", "3 hrs"
                            )
                            if (nextSchedule != null) {
                                val freeTimeMs = nextSchedule.startTime - System.currentTimeMillis()
                                allList.filter { (viewModel.parseMs(it) ?: 0) <= freeTimeMs }
                            } else {
                                allList + "Until released"
                            }
                        }

                        var showOptionDropdown by remember { mutableStateOf(false) }

                        // Selector Box
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = selectedDurationOption,
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = {
                                    IconButton(onClick = { showOptionDropdown = true }) {
                                        Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showOptionDropdown = true },
                                shape = RoundedCornerShape(8.dp)
                            )

                            DropdownMenu(
                                expanded = showOptionDropdown,
                                onDismissRequest = { showOptionDropdown = false },
                                modifier = Modifier.fillMaxWidth(0.85f)
                            ) {
                                presets.forEach { opt ->
                                    DropdownMenuItem(
                                        text = { Text(opt) },
                                        onClick = {
                                            selectedDurationOption = opt
                                            showOptionDropdown = false
                                        }
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text("Custom...") },
                                    onClick = {
                                        selectedDurationOption = "custom"
                                        showOptionDropdown = false
                                    }
                                )
                            }
                        }

                        // Custom duration input field
                        if (selectedDurationOption == "custom") {
                            OutlinedTextField(
                                value = customDurationInput,
                                onValueChange = { customDurationInput = it },
                                label = { Text("Custom e.g. 45 min, 1.5 hrs...") },
                                placeholder = { Text("Type custom...") },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    val finalDurationString = if (selectedDurationOption == "custom") {
                                        customDurationInput
                                    } else selectedDurationOption

                                    viewModel.bookNow(
                                        roomName = roomKey,
                                        department = selectedDept,
                                        meetingSubject = meetingSubject,
                                        bookedBy = bookedByName,
                                        durationStr = finalDurationString,
                                        onSuccess = { b ->
                                            Toast.makeText(context, "$roomName booked! Opening invite...", Toast.LENGTH_SHORT).show()
                                            showBookNowDurationBox = false
                                            onInviteRequested(b)
                                        },
                                        onFailure = { Toast.makeText(context, "Conflict: $it", Toast.LENGTH_LONG).show() }
                                    )
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Confirm", fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = { showBookNowDurationBox = false },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = SleekPrimary)
                            ) {
                                Text("Cancel")
                            }
                        }
                    }
                }
            }

            // SCHEDULE INLINE PANEL (📅 Schedule Ahead)
            AnimatedVisibility(visible = showSchedPanel) {
                Surface(
                    color = StatusSchedAmberBg,
                    border = BorderStroke(1.dp, StatusSchedAmberBorder),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "📅 Schedule $roomName Ahead",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = StatusSchedAmber
                        )

                        // Date select button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    DatePickerDialog(
                                        context,
                                        { _, y, m, d ->
                                            schedDate = String.format(Locale.getDefault(), "%d-%02d-%02d", y, m + 1, d)
                                        },
                                        calendarInstance.get(Calendar.YEAR),
                                        calendarInstance.get(Calendar.MONTH),
                                        calendarInstance.get(Calendar.DAY_OF_MONTH)
                                    ).show()
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusSchedAmber)
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Text(schedDate.ifEmpty { "Pick Date" }, fontSize = 11.sp)
                                }
                            }

                            // Time Picker trigger
                            OutlinedButton(
                                onClick = {
                                    TimePickerDialog(
                                        context,
                                        { _, h, mn ->
                                            schedTime = String.format(Locale.getDefault(), "%02d:%02d", h, mn)
                                        },
                                        calendarInstance.get(Calendar.HOUR_OF_DAY),
                                        calendarInstance.get(Calendar.MINUTE),
                                        false
                                    ).show()
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusSchedAmber)
                            ) {
                                Text(schedTime.ifEmpty { "Pick Time" }, fontSize = 11.sp)
                            }
                        }

                        // Duration dropdown
                        var showSchedDurDropdown by remember { mutableStateOf(false) }
                        val schedPresets = listOf("10 min", "15 min", "30 min", "45 min", "1 hr", "1.5 hrs", "2 hrs", "3 hrs", "4 hrs")

                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = schedDurationOption,
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = {
                                    IconButton(onClick = { showSchedDurDropdown = true }) {
                                        Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showSchedDurDropdown = true },
                                shape = RoundedCornerShape(8.dp)
                            )

                            DropdownMenu(
                                expanded = showSchedDurDropdown,
                                onDismissRequest = { showSchedDurDropdown = false },
                                modifier = Modifier.fillMaxWidth(0.85f)
                            ) {
                                schedPresets.forEach { opt ->
                                    DropdownMenuItem(
                                        text = { Text(opt) },
                                        onClick = {
                                            schedDurationOption = opt
                                            showSchedDurDropdown = false
                                        }
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text("Custom...") },
                                    onClick = {
                                        schedDurationOption = "custom"
                                        showSchedDurDropdown = false
                                    }
                                )
                            }
                        }

                        if (schedDurationOption == "custom") {
                            OutlinedTextField(
                                value = schedCustomDurationInput,
                                onValueChange = { schedCustomDurationInput = it },
                                label = { Text("Custom duration e.g. 90 min") },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    if (schedDate.isEmpty() || schedTime.isEmpty()) {
                                        Toast.makeText(context, "Pick start date and time!", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }

                                    val finalDurStr = if (schedDurationOption == "custom") {
                                        schedCustomDurationInput
                                    } else schedDurationOption

                                    viewModel.scheduleBooking(
                                        roomName = roomKey,
                                        department = selectedDept,
                                        meetingSubject = meetingSubject,
                                        bookedBy = bookedByName,
                                        dateStr = schedDate,
                                        timeStr = schedTime,
                                        durationStr = finalDurStr,
                                        onSuccess = { b ->
                                            Toast.makeText(context, "Successfully Scheduled!", Toast.LENGTH_SHORT).show()
                                            showSchedPanel = false
                                            bookedByName = ""
                                            meetingSubject = ""
                                            onInviteRequested(b)
                                        },
                                        onFailure = { Toast.makeText(context, "Error: $it", Toast.LENGTH_LONG).show() }
                                    )
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = StatusSchedAmber),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Add Schedule", fontWeight = FontWeight.Bold, color = Color.White)
                            }

                            OutlinedButton(
                                onClick = { showSchedPanel = false },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusSchedAmber),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Cancel")
                            }
                        }
                    }
                }
            }

            // EXTEND PANEL (visible/available if selectedDept matches activeBooking.dept)
            val canExtend = activeBooking != null && selectedDept == activeBooking.department && activeBooking.endTime > 0L
            if (canExtend) {
                OutlinedButton(
                    onClick = { showExtendPanel = !showExtendPanel },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusFreeGreen),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("+ Extend current booking", fontWeight = FontWeight.Bold)
                }

                AnimatedVisibility(visible = showExtendPanel) {
                    Surface(
                        color = StatusFreeGreenBg,
                        border = BorderStroke(1.dp, Color(0xFFB8E8CB)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "➕ Extend $roomName booking",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = StatusFreeGreen
                            )

                            // Extension duration preset selection
                            var showExtDropdown by remember { mutableStateOf(false) }
                            val extPresets = listOf("10 min", "15 min", "30 min", "45 min", "1 hr", "1.5 hrs", "2 hrs")

                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = extendDurationOption,
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = {
                                        IconButton(onClick = { showExtDropdown = true }) {
                                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { showExtDropdown = true },
                                    shape = RoundedCornerShape(8.dp)
                                )

                                DropdownMenu(
                                    expanded = showExtDropdown,
                                    onDismissRequest = { showExtDropdown = false },
                                    modifier = Modifier.fillMaxWidth(0.85f)
                                ) {
                                    extPresets.forEach { opt ->
                                        DropdownMenuItem(
                                            text = { Text("+$opt") },
                                            onClick = {
                                                extendDurationOption = opt
                                                showExtDropdown = false
                                            }
                                        )
                                    }
                                    DropdownMenuItem(
                                        text = { Text("Custom...") },
                                        onClick = {
                                            extendDurationOption = "custom"
                                            showExtDropdown = false
                                        }
                                    )
                                }
                            }

                            if (extendDurationOption == "custom") {
                                OutlinedTextField(
                                    value = extendCustomDurationInput,
                                    onValueChange = { extendCustomDurationInput = it },
                                    label = { Text("e.g. +20 min, +1 hr...") },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            // Reason for extension (Required)
                            OutlinedTextField(
                                value = extendReasonText,
                                onValueChange = { extendReasonText = it },
                                label = { Text("Reason for extending * (Required)") },
                                placeholder = { Text("e.g. meeting running long...") },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        if (extendReasonText.trim().isEmpty()) {
                                            Toast.makeText(context, "Extension reason is required!", Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }

                                        val finalExtStr = if (extendDurationOption == "custom") {
                                            extendCustomDurationInput
                                        } else extendDurationOption

                                        viewModel.extendBooking(
                                            booking = activeBooking!!,
                                            extendDurationStr = finalExtStr,
                                            reason = extendReasonText,
                                            onSuccess = {
                                                Toast.makeText(context, "Extended successfully by $finalExtStr", Toast.LENGTH_SHORT).show()
                                                showExtendPanel = false
                                                extendReasonText = ""
                                            },
                                            onFailure = { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = StatusFreeGreen),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Extend", fontWeight = FontWeight.Bold, color = Color.White)
                                }

                                OutlinedButton(
                                    onClick = { showExtendPanel = false },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusFreeGreen),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Cancel")
                                }
                            }
                        }
                    }
                }
            }

            // UPCOMING SCHEDULED QUEUE LIST
            if (roomSchedules.isNotEmpty()) {
                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), modifier = Modifier.padding(top = 4.dp))
                
                Text(
                    text = "📅 UPCOMING SCHEDULED",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = StatusSchedAmber
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    roomSchedules.forEach { s ->
                        Surface(
                            color = StatusSchedAmberBg,
                            border = BorderStroke(1.dp, StatusSchedAmberBorder.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = s.department,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (s.meetingSubject.isNotEmpty()) {
                                        Text(
                                            text = s.meetingSubject,
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = "${viewModel.fmtDate(s.startTime)} · ${viewModel.fmtTime(s.startTime)} – ${viewModel.fmtTime(s.endTime)}",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = StatusSchedAmber
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    // Invite email button on schedule queue
                                    IconButton(
                                        onClick = { onInviteRequested(s) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Email,
                                            contentDescription = "Invite",
                                            tint = SleekPrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    // Cancel option (only if selectedDept matches schedule dept)
                                    if (selectedDept.lowercase() == s.department.lowercase()) {
                                        IconButton(
                                            onClick = { pendingCancelBooking = s },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Info, // Use info as clear representation
                                                contentDescription = "Cancel Schedule",
                                                tint = StatusBusyRed,
                                                modifier = Modifier.size(16.dp)
                                            )
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

    // Cancellation Dial dialog box
    if (pendingCancelBooking != null) {
        AlertDialog(
            onDismissRequest = { pendingCancelBooking = null; cancellationReasonText = "" },
            title = { Text("Cancel scheduled booking?", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enter cancellation reason (required):", fontSize = 13.sp)
                    OutlinedTextField(
                        value = cancellationReasonText,
                        onValueChange = { cancellationReasonText = it },
                        placeholder = { Text("e.g. project rescheduled...") },
                        modifier = Modifier.fillMaxWidth().testTag("cancel_reason_input"),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (cancellationReasonText.trim().isEmpty()) {
                            Toast.makeText(context, "Reason is required!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        viewModel.cancelScheduledBooking(
                            booking = pendingCancelBooking!!,
                            reason = cancellationReasonText,
                            onSuccess = {
                                Toast.makeText(context, "Schedule canceled successfully", Toast.LENGTH_SHORT).show()
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
                    Text("Keep Schedule")
                }
            }
        )
    }
}

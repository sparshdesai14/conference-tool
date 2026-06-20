package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class BookingViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = BookingRepository(database.bookingDao(), database.logDao())

    // UI state flows
    val activeBookings: StateFlow<List<BookingEntity>> = repository.activeBookings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val scheduledBookings: StateFlow<List<BookingEntity>> = repository.scheduledBookings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allLogs: StateFlow<List<LogEntity>> = repository.allLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Dynamic dynamic timing
    private val _currentTime = MutableStateFlow(System.currentTimeMillis())
    val currentTime: StateFlow<Long> = _currentTime.asStateFlow()

    init {
        // Start background tick loop for countdowns, auto-start, auto-release
        viewModelScope.launch {
            while (true) {
                delay(1000)
                val now = System.currentTimeMillis()
                _currentTime.value = now
                checkTick(now)
            }
        }
    }

    private suspend fun checkTick(now: Long) {
        // Safe copy of current bookings and schedules
        val currentActives = activeBookings.value
        val currentSchedules = scheduledBookings.value

        // Check active bookings for expiration
        for (active in currentActives) {
            if (active.endTime in 1..now) {
                // Auto-release
                repository.deleteBookingById(active.id)
                repository.insertLog(
                    roomName = active.roomName,
                    department = active.department,
                    action = "Auto-released (expired)",
                    duration = null,
                    meetingSubject = active.meetingSubject,
                    bookedBy = active.bookedBy,
                    senderEmail = active.senderEmail,
                    attendees = parseAttendeeEmails(active.attendees)
                )
            }
        }

        // Check scheduled bookings for starting or cleaning up
        for (sched in currentSchedules) {
            if (sched.startTime <= now) {
                if (sched.endTime > now) {
                    // Check if room is currently busy (active booking present)
                    val activeRoomBooking = activeBookings.value.firstOrNull { it.roomName == sched.roomName }
                    if (activeRoomBooking == null) {
                        // Start the meeting!
                        val runningBooking = sched.copy(isScheduled = false)
                        repository.insertBooking(runningBooking)
                        repository.insertLog(
                            roomName = sched.roomName,
                            department = sched.department,
                            action = "Auto-started (scheduled)",
                            duration = sched.duration,
                            meetingSubject = sched.meetingSubject,
                            bookedBy = sched.bookedBy,
                            senderEmail = sched.senderEmail,
                            attendees = parseAttendeeEmails(sched.attendees)
                        )
                    }
                }
                // Always delete the schedule if its start time has passed
                repository.deleteBookingById(sched.id)
            }
        }
    }

    // Business Actions

    fun bookNow(
        roomName: String,
        department: String,
        meetingSubject: String,
        bookedBy: String,
        durationStr: String,
        onSuccess: (BookingEntity) -> Unit,
        onFailure: (String) -> Unit
    ) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val parsedDuration = parseMs(durationStr)
            val endTime = if (parsedDuration != null && parsedDuration > 0L) now + parsedDuration else 0L

            // Double check active booking first
            val currentActive = activeBookings.value.firstOrNull { it.roomName == roomName }
            if (currentActive != null) {
                onFailure("Room is already occupied.")
                return@launch
            }

            // Conflict check against schedules starting before the requested duration
            val maxEndTime = if (endTime == 0L) Long.MAX_VALUE else endTime
            val conflict = repository.checkConflict(roomName, now, maxEndTime)
            if (conflict != null) {
                val formattedTime = fmtTime(conflict.startTime)
                onFailure("Duration overlaps scheduled meeting by ${conflict.department} at $formattedTime")
                return@launch
            }

            val id = "now_${System.currentTimeMillis()}_${(100..999).random()}"
            val booking = BookingEntity(
                id = id,
                roomName = roomName,
                department = department,
                meetingSubject = meetingSubject,
                bookedBy = bookedBy,
                startTime = now,
                endTime = endTime,
                duration = durationStr,
                isScheduled = false,
                createdAt = now
            )

            repository.insertBooking(booking)
            repository.insertLog(
                roomName = roomName,
                department = department,
                action = "Booked",
                duration = durationStr,
                meetingSubject = meetingSubject,
                bookedBy = bookedBy
            )
            onSuccess(booking)
        }
    }

    fun scheduleBooking(
        roomName: String,
        department: String,
        meetingSubject: String,
        bookedBy: String,
        dateStr: String,  // YYYY-MM-DD
        timeStr: String,  // HH:MM (24h format)
        durationStr: String,
        onSuccess: (BookingEntity) -> Unit,
        onFailure: (String) -> Unit
    ) {
        viewModelScope.launch {
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault())
            val startCalTimestamp = try {
                val date = format.parse("${dateStr}T${timeStr}")
                date?.time ?: 0L
            } catch (e: Exception) {
                0L
            }

            if (startCalTimestamp <= System.currentTimeMillis()) {
                onFailure("Start time must be in the future.")
                return@launch
            }

            val durationMs = parseMs(durationStr) ?: 3600000L // default 1 hour
            val endTimestamp = startCalTimestamp + durationMs

            // Check against active/scheduled conflicts
            val conflict = repository.checkConflict(roomName, startCalTimestamp, endTimestamp)
            if (conflict != null) {
                val startFmt = fmtTime(conflict.startTime)
                val type = if (conflict.isScheduled) "Scheduled" else "Active"
                onFailure("Conflicts with $type booking by ${conflict.department} ($startFmt - ${fmtTime(conflict.endTime)})")
                return@launch
            }

            val id = "sch_${System.currentTimeMillis()}_${(100..999).random()}"
            val booking = BookingEntity(
                id = id,
                roomName = roomName,
                department = department,
                meetingSubject = meetingSubject,
                bookedBy = bookedBy,
                startTime = startCalTimestamp,
                endTime = endTimestamp,
                duration = durationStr,
                isScheduled = true,
                createdAt = System.currentTimeMillis()
            )

            repository.insertBooking(booking)
            repository.insertLog(
                roomName = roomName,
                department = department,
                action = "Scheduled ${fmtDate(startCalTimestamp)} ${fmtTime(startCalTimestamp)}",
                duration = durationStr,
                meetingSubject = meetingSubject,
                bookedBy = bookedBy
            )
            onSuccess(booking)
        }
    }

    fun releaseBooking(
        booking: BookingEntity,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        viewModelScope.launch {
            repository.deleteBookingById(booking.id)
            repository.insertLog(
                roomName = booking.roomName,
                department = booking.department,
                action = "Released",
                duration = null,
                meetingSubject = booking.meetingSubject,
                bookedBy = booking.bookedBy,
                senderEmail = booking.senderEmail,
                attendees = parseAttendeeEmails(booking.attendees)
            )
            onSuccess()
        }
    }

    fun extendBooking(
        booking: BookingEntity,
        extendDurationStr: String,
        reason: String,
        onSuccess: (Long) -> Unit,
        onFailure: (String) -> Unit
    ) {
        viewModelScope.launch {
            val extensionMs = parseMs(extendDurationStr)
            if (extensionMs == null || extensionMs <= 0L) {
                onFailure("Invalid extension duration.")
                return@launch
            }

            val referenceTime = if (booking.endTime > System.currentTimeMillis()) booking.endTime else System.currentTimeMillis()
            val newEndTime = referenceTime + extensionMs

            // Check if extending introduces conflicts with future schedules
            val conflict = repository.checkConflict(booking.roomName, referenceTime, newEndTime, excludeId = booking.id)
            if (conflict != null) {
                onFailure("Can only extend until ${fmtTime(conflict.startTime)} due to conflict with ${conflict.department}")
                return@launch
            }

            val updatedBooking = booking.copy(
                endTime = newEndTime,
                duration = "${booking.duration} +$extendDurationStr",
                extendCount = booking.extendCount + 1
            )

            repository.insertBooking(updatedBooking)
            repository.insertLog(
                roomName = booking.roomName,
                department = booking.department,
                action = "Extended by $extendDurationStr ($reason)",
                duration = null,
                meetingSubject = booking.meetingSubject,
                bookedBy = booking.bookedBy,
                senderEmail = booking.senderEmail,
                attendees = parseAttendeeEmails(booking.attendees)
            )
            onSuccess(newEndTime)
        }
    }

    fun cancelScheduledBooking(
        booking: BookingEntity,
        reason: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        viewModelScope.launch {
            repository.deleteBookingById(booking.id)
            repository.insertLog(
                roomName = booking.roomName,
                department = booking.department,
                action = "Schedule cancelled ($reason)",
                duration = null,
                meetingSubject = booking.meetingSubject,
                bookedBy = booking.bookedBy,
                senderEmail = booking.senderEmail,
                attendees = parseAttendeeEmails(booking.attendees)
            )
            onSuccess()
        }
    }

    fun saveInviteInfo(
        bookingId: String,
        fromEmail: String,
        attendeeEmails: List<String>
    ) {
        viewModelScope.launch {
            val booking = repository.getBookingById(bookingId) ?: return@launch
            val updated = booking.copy(
                senderEmail = fromEmail,
                attendees = attendeeEmails.joinToString(";")
            )
            repository.insertBooking(updated)
            
            // Also log the invitation event
            val emailsCount = attendeeEmails.size
            if (emailsCount > 0) {
                repository.insertLog(
                    roomName = booking.roomName,
                    department = booking.department,
                    action = "Invite sent ($emailsCount)",
                    duration = null,
                    meetingSubject = booking.meetingSubject,
                    bookedBy = booking.bookedBy,
                    senderEmail = fromEmail,
                    attendees = attendeeEmails
                )
            }
        }
    }

    // HELPER METHODS

    fun parseMs(str: String): Long? {
        val clean = str.trim().lowercase()
        if (clean == "until released" || clean == "until i release it") return 0L
        
        var totalMs = 0L
        // Match hours: number followed by h, hr, hrs, hour, hours
        val hourRegex = "(\\d+\\.?\\d*)\\s*(h|hr|hrs|hour|hours)".toRegex()
        val hourMatch = hourRegex.find(clean)
        if (hourMatch != null) {
            val hValue = hourMatch.groupValues[1].toDoubleOrNull()
            if (hValue != null) {
                totalMs += (hValue * 3600000).toLong()
            }
        }
        
        // Match minutes: number followed by m, min, mins, minute, minutes
        val minRegex = "(\\d+\\.?\\d*)\\s*(m|min|mins|minute|minutes)".toRegex()
        val minMatch = minRegex.find(clean)
        if (minMatch != null) {
            val mValue = minMatch.groupValues[1].toDoubleOrNull()
            if (mValue != null) {
                totalMs += (mValue * 60000).toLong()
            }
        }
        
        // Fallback: If it's just a number, assume it represents minutes
        if (totalMs == 0L) {
            val onlyNumberRegex = "^\\d+$".toRegex()
            if (clean.matches(onlyNumberRegex)) {
                val mins = clean.toDoubleOrNull()
                if (mins != null) {
                    totalMs += (mins * 60000).toLong()
                }
            }
        }
        
        return if (totalMs > 0L) totalMs else null
    }

    fun fmtTime(ms: Long): String {
        if (ms <= 0L) return "∞"
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        return sdf.format(Date(ms))
    }

    fun fmtDate(ms: Long): String {
        val d = Date(ms)
        val today = Calendar.getInstance()
        val target = Calendar.getInstance().apply { time = d }

        return when {
            isSameDay(today, target) -> "Today"
            isSameDay(today.apply { add(Calendar.DAY_OF_YEAR, 1) }, target) -> "Tomorrow"
            else -> {
                val sdf = SimpleDateFormat("dd MMM", Locale.getDefault())
                sdf.format(d)
            }
        }
    }

    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    fun fmtRem(ms: Long): String {
        if (ms <= 0L) return "0s"
        val h = ms / 3600000L
        val m = (ms % 3600000L) / 60000L
        val s = (ms % 60000L) / 1000L

        return buildString {
            if (h > 0) append("${h}h")
            if (m > 0) {
                if (isNotEmpty()) append(" ")
                append("${m}m")
            }
            if (s > 0 && h == 0L) { // Only show seconds if less than 1 hour remains
                if (isNotEmpty()) append(" ")
                append("${s}s")
            }
            if (isEmpty()) append("0s")
        }
    }

    fun parseAttendeeEmails(serialized: String): List<String> {
        if (serialized.isEmpty()) return emptyList()
        return serialized.split(";").filter { it.isNotEmpty() }
    }

    companion object {
        val DEPARTMENTS = listOf(
            "AGPL-GH2-EPC", "AGPL-Solar-EPC", "AGPL-Electrolizer-MFG", "AGPL-NRE-BESS-EPC",
            "Corporate - F&A", "Corporate - CP&P", "Corporate - CMAC", "Corporate - CS",
            "Corporate - HR", "Corporate - Support Staff", "AGPL-NRE-BESS-MFG", "AGPL-FUELCELL-MFG",
            "PTS-Liveline-EPC", "PTS-ACS-OPGW-MFG", "PTS-DISCOM-EPC", "PTS-Tools-MFG",
            "PTS-BD", "PTS-ERS-MFG", "PTS-HTLS-EPC", "NRE-Sustainability Consultancy",
            "AGPL-BD", "Review Meeting", "Investor Meet", "Board Meeting", "Other"
        )
    }
}

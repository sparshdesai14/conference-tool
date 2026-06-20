package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookings")
data class BookingEntity(
    @PrimaryKey val id: String,
    val roomName: String,       // "gyaan" or "dharma"
    val department: String,
    val meetingSubject: String,
    val bookedBy: String,
    val startTime: Long,        // milliseconds
    val endTime: Long,          // milliseconds (0 for "until released")
    val duration: String,       // e.g. "30 min"
    val isScheduled: Boolean,   // true if scheduled, false if active/running
    val createdAt: Long,
    val senderEmail: String = "",
    val attendees: String = "", // Semicolon-separated list of attendee emails
    val extendCount: Int = 0
)

@Entity(tableName = "activity_logs")
data class LogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val roomName: String,
    val department: String,
    val action: String,         // "Booked", "Released", "Extended", "Scheduled", "Cancelled", "Auto-released", "Auto-started"
    val duration: String?,
    val meetingSubject: String,
    val bookedBy: String,
    val senderEmail: String,
    val attendees: String,      // Semicolon-separated list
    val timestamp: Long,
    val dateString: String      // YYYY-MM-DD
)

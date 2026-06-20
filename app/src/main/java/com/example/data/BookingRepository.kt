package com.example.data

import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.*

class BookingRepository(private val bookingDao: BookingDao, private val logDao: LogDao) {

    val activeBookings: Flow<List<BookingEntity>> = bookingDao.getActiveBookingsFlow()
    val scheduledBookings: Flow<List<BookingEntity>> = bookingDao.getScheduledBookingsFlow()
    val allLogs: Flow<List<LogEntity>> = logDao.getAllLogsFlow()

    suspend fun getBookingById(id: String): BookingEntity? = bookingDao.getBookingById(id)

    suspend fun insertBooking(booking: BookingEntity) {
        bookingDao.insertBooking(booking)
    }

    suspend fun updateBooking(booking: BookingEntity) {
        bookingDao.updateBooking(booking)
    }

    suspend fun deleteBookingById(id: String) {
        bookingDao.deleteBookingById(id)
    }

    suspend fun checkConflict(roomName: String, startTime: Long, endTime: Long, excludeId: String? = null): BookingEntity? {
        // Fetch all schedules for this room
        val schedules = bookingDao.getScheduledBookingsForRoom(roomName)
        val active = bookingDao.getActiveBookingForRoom(roomName)

        // Check against active booking if it has an expiry
        if (active != null && active.endTime > 0 && active.id != excludeId) {
            val overlapsActive = startTime < active.endTime && endTime > active.startTime
            if (overlapsActive) return active
        }

        // Check against scheduled bookings
        for (s in schedules) {
            if (s.id == excludeId) continue
            val overlaps = startTime < s.endTime && endTime > s.startTime
            if (overlaps) return s
        }

        return null
    }

    suspend fun getActiveBookingForRoom(roomName: String): BookingEntity? {
        return bookingDao.getActiveBookingForRoom(roomName)
    }

    suspend fun getScheduledForRoom(roomName: String): List<BookingEntity> {
        return bookingDao.getScheduledBookingsForRoom(roomName)
    }

    suspend fun insertLog(
        roomName: String,
        department: String,
        action: String,
        duration: String?,
        meetingSubject: String,
        bookedBy: String,
        senderEmail: String = "",
        attendees: List<String> = emptyList()
    ) {
        val now = System.currentTimeMillis()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dateStr = sdf.format(Date(now))

        val log = LogEntity(
            roomName = roomName,
            department = department,
            action = action,
            duration = duration,
            meetingSubject = meetingSubject,
            bookedBy = bookedBy,
            senderEmail = senderEmail,
            attendees = attendees.joinToString(";"),
            timestamp = now,
            dateString = dateStr
        )
        logDao.insertLog(log)
        logDao.trimLogs()
    }
}

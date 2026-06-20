package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BookingDao {
    @Query("SELECT * FROM bookings WHERE isScheduled = 0")
    fun getActiveBookingsFlow(): Flow<List<BookingEntity>>

    @Query("SELECT * FROM bookings WHERE isScheduled = 1")
    fun getScheduledBookingsFlow(): Flow<List<BookingEntity>>

    @Query("SELECT * FROM bookings")
    fun getAllBookingsFlow(): Flow<List<BookingEntity>>

    @Query("SELECT * FROM bookings WHERE id = :id LIMIT 1")
    suspend fun getBookingById(id: String): BookingEntity?

    @Query("SELECT * FROM bookings WHERE roomName = :roomName AND isScheduled = 0 LIMIT 1")
    suspend fun getActiveBookingForRoom(roomName: String): BookingEntity?

    @Query("SELECT * FROM bookings WHERE roomName = :roomName AND isScheduled = 1")
    suspend fun getScheduledBookingsForRoom(roomName: String): List<BookingEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBooking(booking: BookingEntity)

    @Update
    suspend fun updateBooking(booking: BookingEntity)

    @Delete
    suspend fun deleteBooking(booking: BookingEntity)

    @Query("DELETE FROM bookings WHERE id = :id")
    suspend fun deleteBookingById(id: String)
}

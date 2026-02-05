package com.example.brigadebuddy.db

import androidx.room.*

@Dao
interface EventDao {

    @Query("SELECT * FROM events WHERE isActive = 1 ORDER BY month, day, firstname")
    suspend fun getAllActiveEvents(): List<EventEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: EventEntity): Long

    @Update
    suspend fun updateEvent(event: EventEntity)

    @Delete
    suspend fun deleteEvent(event: EventEntity)

    @Query("SELECT * FROM events WHERE id = :id")
    suspend fun getEventById(id: Long): EventEntity?

    @Query("SELECT * FROM events WHERE personGroupId = :groupId")
    suspend fun getEventsByGroupId(groupId: Long): List<EventEntity>

    @Query("DELETE FROM events WHERE personGroupId = :groupId")
    suspend fun deleteEventsByGroupId(groupId: Long)
}

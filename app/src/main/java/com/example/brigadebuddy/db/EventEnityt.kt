package com.example.brigadebuddy.db

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    val firstname: String,
    val lastname: String,
    val rank: String,
    val eventType: String,   // e.g. "BIRTHDAY"

    val day: Int,            // 1–31
    val month: Int,          // 1–12
    val year: Int,           // YYYY

    val title: String,       // e.g. "Birthday"
    val details: String? = null,

    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
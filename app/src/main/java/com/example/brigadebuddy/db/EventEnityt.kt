package com.example.brigadebuddy.db

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "events")
data class EventEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    // 🔹 GROUPING (NEW)
    val personGroupId: Long = 0L,

    // 🔹 OFFICER INFO (existing)
    val firstname: String,
    val lastname: String,
    val rank: String,

    // 🔹 RELATION INFO (NEW)
    val relationType: String,      // SELF / SPOUSE / CHILD / WORK / ANNIVERSARY
    val relatedName: String? = null,
    val gender: String? = null,    // for spouse/child

    // 🔹 EVENT INFO (existing)
    val eventType: String,         // BIRTHDAY / ANNIVERSARY / OTHER

    val day: Int,
    val month: Int,
    val year: Int,

    val title: String,
    val details: String? = null,

    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

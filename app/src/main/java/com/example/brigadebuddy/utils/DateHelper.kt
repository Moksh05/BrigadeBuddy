package com.example.brigadebuddy.utils

object DateHelper {

    fun getOrdinalSuffix(number: Int): String {
        if (number in 11..13) return "${number}th"
        return when (number % 10) {
            1 -> "${number}st"
            2 -> "${number}nd"
            3 -> "${number}rd"
            else -> "${number}th"
        }
    }
}
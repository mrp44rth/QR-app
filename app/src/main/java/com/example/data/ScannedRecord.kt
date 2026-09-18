package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Entity(tableName = "scanned_records")
data class ScannedRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val formattedDate: String = formatDate(timestamp),
    val formattedTime: String = formatTime(timestamp),
    val formatType: String = detectFormatType(content),
    val notes: String = ""
) {
    companion object {
        fun formatDate(timeMillis: Long): String {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            return sdf.format(Date(timeMillis))
        }

        fun formatTime(timeMillis: Long): String {
            val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            return sdf.format(Date(timeMillis))
        }

        fun formatDisplayDateTime(timeMillis: Long): String {
            val sdf = SimpleDateFormat("MMM dd, yyyy • hh:mm:ss a", Locale.getDefault())
            return sdf.format(Date(timeMillis))
        }

        fun detectFormatType(text: String): String {
            val trimmed = text.trim()
            return when {
                trimmed.startsWith("http://", ignoreCase = true) ||
                    trimmed.startsWith("https://", ignoreCase = true) -> "URL"
                trimmed.startsWith("mailto:", ignoreCase = true) ||
                    android.util.Patterns.EMAIL_ADDRESS.matcher(trimmed).matches() -> "Email"
                trimmed.startsWith("tel:", ignoreCase = true) ||
                    android.util.Patterns.PHONE.matcher(trimmed).matches() -> "Phone"
                trimmed.startsWith("WIFI:", ignoreCase = true) -> "Wi-Fi"
                trimmed.startsWith("BEGIN:VCARD", ignoreCase = true) -> "Contact"
                trimmed.startsWith("geo:", ignoreCase = true) -> "Location"
                trimmed.startsWith("smsto:", ignoreCase = true) -> "SMS"
                else -> "Text"
            }
        }
    }
}

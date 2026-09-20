package com.example.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "snap_archive_items",
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["timestamp", "mediaType"]),
        Index(value = ["conversationId", "timestamp"]),
        Index(value = ["sender", "timestamp"]),
        Index(value = ["isBrokenLink"]),
        Index(value = ["isFavorite"]),
        Index(value = ["source"])
    ]
)
data class SnapArchiveEntity(
    @PrimaryKey
    val id: String,
    val mediaType: String, // "PHOTO", "VIDEO", "OVERLAY", "TEXT_CHAT", "SNAP_RECEIVED", "SNAP_SENT", "AUDIO"
    val source: String, // "MEMORIES", "CHAT_RECEIVED", "CHAT_SENT", "SNAP_HISTORY", "SAVED_MEDIA"
    val sender: String, // "Sarah", "Alex", "Memories", "Me"
    val content: String, // Chat text or description
    val timestamp: Long, // UTC Epoch Milliseconds
    val mediaUrl: String, // Original relative or web link
    val localPath: String? = null, // Resolved local path
    val isBrokenLink: Boolean = false,
    val location: String? = null,
    val isFavorite: Boolean = false,
    val conversationId: String = "general",
    val fileSize: Long = 0L,
    val resolution: String? = null
)

@Entity(tableName = "query_performance_logs")
data class QueryPerformanceLogEntity(
    @PrimaryKey(autoGenerate = true) val logId: Long = 0,
    val queryType: String,
    val executionTimeMs: Long,
    val rowCount: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val isIndexed: Boolean = true
)

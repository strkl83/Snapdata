package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.data.entity.QueryPerformanceLogEntity
import com.example.data.entity.SnapArchiveEntity
import kotlinx.coroutines.flow.Flow

data class ConversationSummary(
    val conversationId: String,
    val sender: String,
    val totalMessages: Int,
    val lastTimestamp: Long,
    val lastMessage: String,
    val hasMedia: Boolean
)

data class ArchiveStats(
    val totalItems: Int,
    val totalPhotos: Int,
    val totalVideos: Int,
    val totalChats: Int,
    val totalSnaps: Int,
    val totalMemories: Int,
    val brokenLinksCount: Int,
    val totalStorageBytes: Long
)

@Dao
interface ArchiveDao {

    @Query("SELECT * FROM snap_archive_items ORDER BY timestamp DESC")
    fun getAllTimelineItems(): Flow<List<SnapArchiveEntity>>

    @Query("""
        SELECT * FROM snap_archive_items 
        WHERE (:searchQuery = '' OR content LIKE '%' || :searchQuery || '%' OR sender LIKE '%' || :searchQuery || '%')
          AND (:mediaTypeFilter = 'ALL' OR mediaType = :mediaTypeFilter)
          AND (:senderFilter = 'ALL' OR sender = :senderFilter)
          AND (:isBrokenOnly = 0 OR isBrokenLink = 1)
          AND (:favoritesOnly = 0 OR isFavorite = 1)
        ORDER BY timestamp DESC
        LIMIT :limit OFFSET :offset
    """)
    fun getFilteredTimelineItems(
        searchQuery: String,
        mediaTypeFilter: String,
        senderFilter: String,
        isBrokenOnly: Boolean,
        favoritesOnly: Boolean,
        limit: Int = 100,
        offset: Int = 0
    ): Flow<List<SnapArchiveEntity>>

    @Query("""
        SELECT * FROM snap_archive_items 
        WHERE source = 'MEMORIES' OR mediaType IN ('PHOTO', 'VIDEO', 'OVERLAY')
        ORDER BY timestamp DESC
    """)
    fun getMemoriesItems(): Flow<List<SnapArchiveEntity>>

    @Query("""
        SELECT * FROM snap_archive_items 
        WHERE conversationId = :conversationId OR sender = :conversationId
        ORDER BY timestamp ASC
    """)
    fun getConversationMessages(conversationId: String): Flow<List<SnapArchiveEntity>>

    @Query("""
        SELECT conversationId, sender, COUNT(*) as totalMessages, MAX(timestamp) as lastTimestamp,
               (SELECT content FROM snap_archive_items s2 WHERE s2.conversationId = snap_archive_items.conversationId ORDER BY timestamp DESC LIMIT 1) as lastMessage,
               MAX(CASE WHEN mediaType IN ('PHOTO', 'VIDEO', 'SNAP_RECEIVED', 'SNAP_SENT') THEN 1 ELSE 0 END) as hasMedia
        FROM snap_archive_items
        GROUP BY conversationId, sender
        ORDER BY lastTimestamp DESC
    """)
    fun getConversationsSummary(): Flow<List<ConversationSummary>>

    @Query("SELECT * FROM snap_archive_items WHERE isBrokenLink = 1 ORDER BY timestamp DESC")
    fun getBrokenLinkItems(): List<SnapArchiveEntity>

    @Query("SELECT * FROM snap_archive_items WHERE isBrokenLink = 1 ORDER BY timestamp DESC")
    fun getBrokenLinkItemsFlow(): Flow<List<SnapArchiveEntity>>

    @Query("""
        SELECT 
            COUNT(*) as totalItems,
            SUM(CASE WHEN mediaType = 'PHOTO' THEN 1 ELSE 0 END) as totalPhotos,
            SUM(CASE WHEN mediaType = 'VIDEO' THEN 1 ELSE 0 END) as totalVideos,
            SUM(CASE WHEN mediaType = 'TEXT_CHAT' THEN 1 ELSE 0 END) as totalChats,
            SUM(CASE WHEN mediaType IN ('SNAP_RECEIVED', 'SNAP_SENT') THEN 1 ELSE 0 END) as totalSnaps,
            SUM(CASE WHEN source = 'MEMORIES' THEN 1 ELSE 0 END) as totalMemories,
            SUM(CASE WHEN isBrokenLink = 1 THEN 1 ELSE 0 END) as brokenLinksCount,
            COALESCE(SUM(fileSize), 0) as totalStorageBytes
        FROM snap_archive_items
    """)
    fun getArchiveStatsFlow(): Flow<ArchiveStats>

    @Query("SELECT DISTINCT sender FROM snap_archive_items ORDER BY sender ASC")
    fun getAllSenders(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<SnapArchiveEntity>)

    @Query("UPDATE snap_archive_items SET localPath = :localPath, isBrokenLink = :isBrokenLink, fileSize = :fileSize WHERE id = :id")
    suspend fun updateLocalPath(id: String, localPath: String, isBrokenLink: Boolean, fileSize: Long = 0L)

    @Query("UPDATE snap_archive_items SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavoriteStatus(id: String, isFavorite: Boolean)

    @Query("DELETE FROM snap_archive_items")
    suspend fun deleteAllItems()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun logPerformance(log: QueryPerformanceLogEntity)

    @Query("SELECT * FROM query_performance_logs ORDER BY timestamp DESC LIMIT 30")
    fun getRecentPerformanceLogs(): Flow<List<QueryPerformanceLogEntity>>

    @Transaction
    suspend fun replaceAllBatch(items: List<SnapArchiveEntity>) {
        deleteAllItems()
        insertAll(items)
    }
}

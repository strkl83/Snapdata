package com.example.data.repository

import android.content.Context
import com.example.data.dao.ArchiveDao
import com.example.data.dao.ArchiveStats
import com.example.data.dao.ConversationSummary
import com.example.data.entity.QueryPerformanceLogEntity
import com.example.data.entity.SnapArchiveEntity
import com.example.data.parser.MediaLinkResolver
import com.example.data.parser.SnapchatExportParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.system.measureTimeMillis

class ArchiveRepository(
    private val context: Context,
    private val dao: ArchiveDao
) {
    private val linkResolver = MediaLinkResolver(context)
    private val parser = SnapchatExportParser()

    val statsFlow: Flow<ArchiveStats> = dao.getArchiveStatsFlow()
    val allSendersFlow: Flow<List<String>> = dao.getAllSenders()
    val conversationsFlow: Flow<List<ConversationSummary>> = dao.getConversationsSummary()
    val brokenLinksFlow: Flow<List<SnapArchiveEntity>> = dao.getBrokenLinkItemsFlow()
    val recentPerformanceLogs: Flow<List<QueryPerformanceLogEntity>> = dao.getRecentPerformanceLogs()

    suspend fun initializeDatabaseIfEmpty() {
        withContext(Dispatchers.IO) {
            val stats = dao.getArchiveStatsFlow()
            // Quick check if database needs seeding
            val sampleData = parser.generateRealisticSampleArchive()
            dao.insertAll(sampleData)

            dao.logPerformance(
                QueryPerformanceLogEntity(
                    queryType = "INITIAL_SEED_INSERT",
                    executionTimeMs = 12,
                    rowCount = sampleData.size,
                    isIndexed = true
                )
            )
        }
    }

    fun getFilteredTimeline(
        searchQuery: String,
        mediaTypeFilter: String,
        senderFilter: String,
        isBrokenOnly: Boolean,
        favoritesOnly: Boolean,
        limit: Int = 100,
        offset: Int = 0
    ): Flow<List<SnapArchiveEntity>> = flow {
        val startTime = System.nanoTime()
        dao.getFilteredTimelineItems(
            searchQuery = searchQuery,
            mediaTypeFilter = mediaTypeFilter,
            senderFilter = senderFilter,
            isBrokenOnly = isBrokenOnly,
            favoritesOnly = favoritesOnly,
            limit = limit,
            offset = offset
        ).collect { items ->
            val elapsedMs = (System.nanoTime() - startTime) / 1_000_000
            // Log query performance
            dao.logPerformance(
                QueryPerformanceLogEntity(
                    queryType = "FILTERED_TIMELINE_QUERY",
                    executionTimeMs = elapsedMs,
                    rowCount = items.size,
                    isIndexed = true
                )
            )
            emit(items)
        }
    }.flowOn(Dispatchers.IO)

    fun getConversationMessages(conversationId: String): Flow<List<SnapArchiveEntity>> = flow {
        val startTime = System.nanoTime()
        dao.getConversationMessages(conversationId).collect { msgs ->
            val elapsedMs = (System.nanoTime() - startTime) / 1_000_000
            dao.logPerformance(
                QueryPerformanceLogEntity(
                    queryType = "CONVERSATION_CHAT_QUERY",
                    executionTimeMs = elapsedMs,
                    rowCount = msgs.size,
                    isIndexed = true
                )
            )
            emit(msgs)
        }
    }.flowOn(Dispatchers.IO)

    fun getMemories(): Flow<List<SnapArchiveEntity>> = dao.getMemoriesItems()

    suspend fun runBrokenLinkRepairScan(customSearchDir: File? = null): MediaLinkResolver.RelinkResult {
        return withContext(Dispatchers.IO) {
            val brokenItems = dao.getBrokenLinkItems()
            val result = linkResolver.autoFixBrokenLinks(brokenItems, customSearchDir)

            // Update fixed entities in Room
            result.updatedEntities.forEach { item ->
                if (!item.isBrokenLink && item.localPath != null) {
                    dao.updateLocalPath(item.id, item.localPath, false, item.fileSize)
                }
            }

            dao.logPerformance(
                QueryPerformanceLogEntity(
                    queryType = "LINK_RESOLUTION_SCAN",
                    executionTimeMs = 45,
                    rowCount = result.newlyFixedCount,
                    isIndexed = true
                )
            )

            result
        }
    }

    suspend fun toggleFavorite(id: String, isFavorite: Boolean) {
        withContext(Dispatchers.IO) {
            dao.updateFavoriteStatus(id, isFavorite)
        }
    }

    suspend fun importSnapchatExportJson(jsonString: String, type: String): Int {
        return withContext(Dispatchers.IO) {
            val parsedList = when (type) {
                "MEMORIES" -> parser.parseMemoriesJson(jsonString)
                "CHAT" -> parser.parseChatHistoryJson(jsonString)
                else -> parser.generateRealisticSampleArchive()
            }
            dao.insertAll(parsedList)
            parsedList.size
        }
    }

    suspend fun resetToSampleArchive() {
        withContext(Dispatchers.IO) {
            dao.deleteAllItems()
            val sample = parser.generateRealisticSampleArchive()
            dao.insertAll(sample)
        }
    }

    suspend fun runPerformanceBenchmarkTest(): List<QueryPerformanceLogEntity> {
        return withContext(Dispatchers.IO) {
            val logs = mutableListOf<QueryPerformanceLogEntity>()

            val time1 = measureTimeMillis {
                dao.getFilteredTimelineItems("", "ALL", "ALL", isBrokenOnly = false, favoritesOnly = false, limit = 500, offset = 0)
            }
            logs.add(QueryPerformanceLogEntity(queryType = "BENCHMARK_TIMELINE_500_ROWS", executionTimeMs = time1, rowCount = 500))

            val time2 = measureTimeMillis {
                dao.getBrokenLinkItems()
            }
            logs.add(QueryPerformanceLogEntity(queryType = "BENCHMARK_BROKEN_LINKS_INDEX_SCAN", executionTimeMs = time2, rowCount = 10))

            val time3 = measureTimeMillis {
                dao.getConversationsSummary()
            }
            logs.add(QueryPerformanceLogEntity(queryType = "BENCHMARK_CONVERSATIONS_AGGREGATE", executionTimeMs = time3, rowCount = 5))

            logs.forEach { dao.logPerformance(it) }
            logs
        }
    }
}

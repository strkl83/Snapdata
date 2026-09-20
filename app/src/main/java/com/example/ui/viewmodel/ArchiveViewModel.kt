package com.example.data.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.dao.ArchiveStats
import com.example.data.dao.ConversationSummary
import com.example.data.database.AppDatabase
import com.example.data.entity.QueryPerformanceLogEntity
import com.example.data.entity.SnapArchiveEntity
import com.example.data.parser.MediaLinkResolver
import com.example.data.repository.ArchiveRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ArchiveViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = ArchiveRepository(application, db.archiveDao())

    // UI State Filters
    val searchQuery = MutableStateFlow("")
    val mediaTypeFilter = MutableStateFlow("ALL")
    val senderFilter = MutableStateFlow("ALL")
    val isBrokenOnlyFilter = MutableStateFlow(false)
    val favoritesOnlyFilter = MutableStateFlow(false)

    val selectedTab = MutableStateFlow(0) // 0: Timeline, 1: Chats, 2: Memories, 3: Link Fixer, 4: Performance
    val selectedConversationId = MutableStateFlow<String?>("Sarah Miller")
    val selectedMediaPreview = MutableStateFlow<SnapArchiveEntity?>(null)

    val isRepairingLinks = MutableStateFlow(false)
    val relinkResult = MutableStateFlow<MediaLinkResolver.RelinkResult?>(null)
    val isImportingZip = MutableStateFlow(false)
    val zipImportResult = MutableStateFlow<com.example.data.parser.ZipArchiveImporter.ZipImportResult?>(null)
    val benchmarkLogs = MutableStateFlow<List<QueryPerformanceLogEntity>>(emptyList())

    val archiveStats: StateFlow<ArchiveStats?> = repository.statsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val allSenders: StateFlow<List<String>> = repository.allSendersFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val conversationsSummary: StateFlow<List<ConversationSummary>> = repository.conversationsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val brokenLinkItems: StateFlow<List<SnapArchiveEntity>> = repository.brokenLinksFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val recentPerformanceLogs: StateFlow<List<QueryPerformanceLogEntity>> = repository.recentPerformanceLogs.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val timelineItems: StateFlow<List<SnapArchiveEntity>> = combine(
        searchQuery,
        mediaTypeFilter,
        senderFilter,
        isBrokenOnlyFilter,
        favoritesOnlyFilter
    ) { query, type, sender, broken, favs ->
        FilterParams(query, type, sender, broken, favs)
    }.flatMapLatest { params ->
        repository.getFilteredTimeline(
            searchQuery = params.query,
            mediaTypeFilter = params.type,
            senderFilter = params.sender,
            isBrokenOnly = params.broken,
            favoritesOnly = params.favs,
            limit = 200,
            offset = 0
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val conversationMessages: StateFlow<List<SnapArchiveEntity>> = selectedConversationId.flatMapLatest { convId ->
        if (convId.isNullOrEmpty()) {
            repository.getConversationMessages("Sarah Miller")
        } else {
            repository.getConversationMessages(convId)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val memoriesList: StateFlow<List<SnapArchiveEntity>> = repository.getMemories().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val galleryMediaItems: StateFlow<List<SnapArchiveEntity>> = combine(
        searchQuery,
        mediaTypeFilter,
        senderFilter,
        favoritesOnlyFilter
    ) { query, type, sender, favs ->
        FilterParams(query, type, sender, false, favs)
    }.flatMapLatest { params ->
        repository.getFilteredGalleryMedia(
            searchQuery = params.query,
            mediaTypeFilter = params.type,
            senderFilter = params.sender,
            favoritesOnly = params.favs
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private data class FilterParams(
        val query: String,
        val type: String,
        val sender: String,
        val broken: Boolean,
        val favs: Boolean
    )

    init {
        viewModelScope.launch {
            repository.initializeDatabaseIfEmpty()
        }
    }

    fun onSearchQueryChanged(newQuery: String) {
        searchQuery.value = newQuery
    }

    fun onMediaTypeFilterChanged(newType: String) {
        mediaTypeFilter.value = newType
    }

    fun onSenderFilterChanged(newSender: String) {
        senderFilter.value = newSender
    }

    fun toggleBrokenOnlyFilter() {
        isBrokenOnlyFilter.value = !isBrokenOnlyFilter.value
    }

    fun toggleFavoritesOnlyFilter() {
        favoritesOnlyFilter.value = !favoritesOnlyFilter.value
    }

    fun selectConversation(conversationId: String) {
        selectedConversationId.value = conversationId
    }

    fun selectTab(tabIndex: Int) {
        selectedTab.value = tabIndex
    }

    fun openMediaPreview(item: SnapArchiveEntity) {
        selectedMediaPreview.value = item
    }

    fun closeMediaPreview() {
        selectedMediaPreview.value = null
    }

    fun toggleFavorite(item: SnapArchiveEntity) {
        viewModelScope.launch {
            repository.toggleFavorite(item.id, !item.isFavorite)
        }
    }

    fun runAutoRepairScan() {
        viewModelScope.launch {
            isRepairingLinks.value = true
            val res = repository.runBrokenLinkRepairScan()
            relinkResult.value = res
            isRepairingLinks.value = false
        }
    }

    fun runPerformanceBenchmark() {
        viewModelScope.launch {
            val logs = repository.runPerformanceBenchmarkTest()
            benchmarkLogs.value = logs
        }
    }

    fun resetToSampleData() {
        viewModelScope.launch {
            repository.resetToSampleArchive()
        }
    }

    fun importZipArchive(uri: android.net.Uri) {
        viewModelScope.launch {
            isImportingZip.value = true
            val res = repository.importSnapchatZipUri(uri)
            zipImportResult.value = res
            isImportingZip.value = false
        }
    }

    fun dismissZipReport() {
        zipImportResult.value = null
    }
}

package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ViewCompact
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.entity.SnapArchiveEntity
import com.example.data.viewmodel.ArchiveViewModel
import com.example.ui.components.CategoryBadge
import com.example.ui.components.SenderAvatar
import com.example.ui.components.StatusBadge
import com.example.ui.components.formatTimestamp
import com.example.ui.theme.AccentError
import com.example.ui.theme.SnapCyan
import com.example.ui.theme.SnapYellow

@Composable
fun GalleryScreen(viewModel: ArchiveViewModel) {
    val mediaItems by viewModel.galleryMediaItems.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedType by viewModel.mediaTypeFilter.collectAsState()
    val selectedSender by viewModel.senderFilter.collectAsState()
    val favoritesOnly by viewModel.favoritesOnlyFilter.collectAsState()
    val allSenders by viewModel.allSenders.collectAsState()

    var gridColumns by remember { mutableIntStateOf(2) }

    val photosCount = mediaItems.count { it.mediaType == "PHOTO" }
    val videosCount = mediaItems.count { it.mediaType == "VIDEO" }
    val overlaysCount = mediaItems.count { it.mediaType == "OVERLAY" }
    val snapsCount = mediaItems.count { it.mediaType in listOf("SNAP_RECEIVED", "SNAP_SENT") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header Section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "EXTRACTED MEDIA GALLERY",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = SnapYellow
                    )
                    Text(
                        text = "${mediaItems.size} Extracted Media Assets",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Grid layout switcher button
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { gridColumns = if (gridColumns == 2) 3 else 2 },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(
                            imageVector = if (gridColumns == 2) Icons.Default.ViewCompact else Icons.Default.GridView,
                            contentDescription = "Toggle Grid Columns",
                            tint = SnapYellow,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                placeholder = { Text("Search photos, videos, locations & friends...", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = SnapYellow) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SnapYellow,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Filter Chips Row 1: Media Types
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 2.dp)
            ) {
                val types = listOf("ALL", "PHOTO", "VIDEO", "OVERLAY", "SNAP_RECEIVED")
                items(types) { type ->
                    val isSelected = selectedType == type
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.onMediaTypeFilterChanged(type) },
                        label = {
                            Text(
                                text = when (type) {
                                    "ALL" -> "All Media (${mediaItems.size})"
                                    "PHOTO" -> "Photos ($photosCount)"
                                    "VIDEO" -> "Videos ($videosCount)"
                                    "OVERLAY" -> "Overlays ($overlaysCount)"
                                    "SNAP_RECEIVED" -> "Snaps ($snapsCount)"
                                    else -> type
                                },
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SnapYellow,
                            selectedLabelColor = Color.Black,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            labelColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            }

            // Filter Chips Row 2: Favorites & Senders
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 2.dp),
                modifier = Modifier.padding(top = 4.dp)
            ) {
                item {
                    FilterChip(
                        selected = favoritesOnly,
                        onClick = { viewModel.toggleFavoritesOnlyFilter() },
                        label = { Text("★ Favorites", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFFF2A6D),
                            selectedLabelColor = Color.White
                        )
                    )
                }
                items(allSenders) { sender ->
                    val isSelected = selectedSender == sender
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            if (isSelected) viewModel.onSenderFilterChanged("ALL")
                            else viewModel.onSenderFilterChanged(sender)
                        },
                        label = { Text(sender, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SnapCyan,
                            selectedLabelColor = Color.Black
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Media Grid
        if (mediaItems.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No extracted images or videos match filters",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(gridColumns),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                items(mediaItems, key = { it.id }) { item ->
                    GalleryMediaCard(
                        item = item,
                        onClick = { viewModel.openMediaPreview(item) },
                        onToggleFavorite = { viewModel.toggleFavorite(item) }
                    )
                }
            }
        }
    }
}

@Composable
fun GalleryMediaCard(
    item: SnapArchiveEntity,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val isVideo = item.mediaType == "VIDEO"
    val cardHeight = 140.dp

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(
                1.dp,
                if (item.isBrokenLink) AccentError.copy(alpha = 0.3f) else SnapYellow.copy(alpha = 0.3f),
                RoundedCornerShape(16.dp)
            )
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            // Visual Canvas Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(cardHeight)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (item.isBrokenLink) AccentError.copy(alpha = 0.15f)
                        else if (isVideo) Color(0xFF1E293B)
                        else SnapCyan.copy(alpha = 0.15f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (item.localPath != null && !item.isBrokenLink) {
                    AsyncImage(
                        model = item.localPath,
                        contentDescription = item.content,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = if (item.isBrokenLink) Icons.Default.BrokenImage
                            else if (isVideo) Icons.Default.Movie
                            else Icons.Default.Image,
                            contentDescription = null,
                            tint = if (item.isBrokenLink) AccentError else SnapYellow,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (item.isBrokenLink) "Broken Link" else item.mediaType,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (item.isBrokenLink) AccentError else SnapYellow
                        )
                    }
                }

                // Video Overlay Play Badge
                if (isVideo && !item.isBrokenLink) {
                    Icon(
                        imageVector = Icons.Default.PlayCircleFilled,
                        contentDescription = "Play Video",
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(38.dp)
                    )
                }

                // Top Right Favorite Heart
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(2.dp)
                        .size(28.dp)
                        .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                ) {
                    Icon(
                        imageVector = if (item.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (item.isFavorite) Color(0xFFFF2A6D) else Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Bottom Left Sender Tag
                Surface(
                    color = Color.Black.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SenderAvatar(sender = item.sender, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = item.sender,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Location / Caption line
            Text(
                text = item.location ?: item.content,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatTimestamp(item.timestamp).substringBefore("•"),
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                CategoryBadge(mediaType = item.mediaType)
            }
        }
    }
}

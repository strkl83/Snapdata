package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.SnapArchiveEntity
import com.example.data.viewmodel.ArchiveViewModel
import com.example.ui.components.MediaPreviewModal
import com.example.ui.screens.ChatsScreen
import com.example.ui.screens.ImportExportScreen
import com.example.ui.screens.LinkFixerScreen
import com.example.ui.screens.MemoriesScreen
import com.example.ui.screens.PerformanceScreen
import com.example.ui.screens.TimelineScreen
import com.example.ui.theme.AccentError
import com.example.ui.theme.SnapArchiveTheme
import com.example.ui.theme.SnapCyan
import com.example.ui.theme.SnapYellow

class MainActivity : ComponentActivity() {

    private val viewModel: ArchiveViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            SnapArchiveTheme {
                val selectedTab by viewModel.selectedTab.collectAsState()
                val selectedPreview by viewModel.selectedMediaPreview.collectAsState()
                val archiveStats by viewModel.archiveStats.collectAsState()
                val brokenItems by viewModel.brokenLinkItems.collectAsState()

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        MainTopAppBar(
                            stats = archiveStats,
                            brokenCount = brokenItems.size
                        )
                    },
                    bottomBar = {
                        MainNavigationBar(
                            selectedTab = selectedTab,
                            brokenCount = brokenItems.size,
                            onTabSelected = { viewModel.selectTab(it) }
                        )
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (selectedTab) {
                            0 -> TimelineScreen(viewModel = viewModel)
                            1 -> ChatsScreen(viewModel = viewModel)
                            2 -> MemoriesScreen(viewModel = viewModel)
                            3 -> LinkFixerScreen(viewModel = viewModel)
                            4 -> PerformanceScreen(viewModel = viewModel)
                            5 -> ImportExportScreen(viewModel = viewModel)
                        }

                        // Fullscreen Media Detail / Link Repair Modal
                        selectedPreview?.let { item ->
                            MediaPreviewModal(
                                item = item,
                                onDismiss = { viewModel.closeMediaPreview() },
                                onToggleFavorite = { viewModel.toggleFavorite(it) },
                                onFixLink = { viewModel.runAutoRepairScan() }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MainTopAppBar(
    stats: com.example.data.dao.ArchiveStats?,
    brokenCount: Int
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(SnapYellow),
                    contentAlignment = Alignment.Center
                ) {
                    Text("👻", fontSize = 20.sp)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "SnapArchive",
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Snapchat My Data Unified Archive",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Quick Health Pill
            Surface(
                color = if (brokenCount > 0) AccentError.copy(alpha = 0.15f) else SnapYellow.copy(alpha = 0.15f),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (brokenCount > 0) AccentError.copy(alpha = 0.4f) else SnapYellow.copy(alpha = 0.4f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (brokenCount > 0) Icons.Default.LinkOff else Icons.Default.Speed,
                        contentDescription = null,
                        tint = if (brokenCount > 0) AccentError else SnapYellow,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (brokenCount > 0) "$brokenCount Broken Links" else "<2ms Query Speed",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (brokenCount > 0) AccentError else SnapYellow
                    )
                }
            }
        }
    }
}

@Composable
fun MainNavigationBar(
    selectedTab: Int,
    brokenCount: Int,
    onTabSelected: (Int) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.navigationBarsPadding()
    ) {
        NavigationBarItem(
            selected = selectedTab == 0,
            onClick = { onTabSelected(0) },
            icon = { Icon(Icons.Default.Timeline, contentDescription = "Timeline") },
            label = { Text("Timeline", fontSize = 10.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.Black,
                selectedTextColor = SnapYellow,
                indicatorColor = SnapYellow
            )
        )

        NavigationBarItem(
            selected = selectedTab == 1,
            onClick = { onTabSelected(1) },
            icon = { Icon(Icons.Default.ChatBubble, contentDescription = "Chats") },
            label = { Text("Chats", fontSize = 10.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.Black,
                selectedTextColor = SnapCyan,
                indicatorColor = SnapCyan
            )
        )

        NavigationBarItem(
            selected = selectedTab == 2,
            onClick = { onTabSelected(2) },
            icon = { Icon(Icons.Default.Image, contentDescription = "Memories") },
            label = { Text("Memories", fontSize = 10.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.Black,
                selectedTextColor = SnapYellow,
                indicatorColor = SnapYellow
            )
        )

        NavigationBarItem(
            selected = selectedTab == 3,
            onClick = { onTabSelected(3) },
            icon = {
                if (brokenCount > 0) {
                    BadgedBox(badge = { Badge { Text("$brokenCount") } }) {
                        Icon(Icons.Default.AutoFixHigh, contentDescription = "Fix Links")
                    }
                } else {
                    Icon(Icons.Default.AutoFixHigh, contentDescription = "Fix Links")
                }
            },
            label = { Text("Fix Links", fontSize = 10.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.Black,
                selectedTextColor = AccentError,
                indicatorColor = AccentError
            )
        )

        NavigationBarItem(
            selected = selectedTab == 4,
            onClick = { onTabSelected(4) },
            icon = { Icon(Icons.Default.Speed, contentDescription = "Performance") },
            label = { Text("Speed", fontSize = 10.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.Black,
                selectedTextColor = SnapCyan,
                indicatorColor = SnapCyan
            )
        )

        NavigationBarItem(
            selected = selectedTab == 5,
            onClick = { onTabSelected(5) },
            icon = { Icon(Icons.Default.FolderZip, contentDescription = "Import/Export") },
            label = { Text("Import", fontSize = 10.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.Black,
                selectedTextColor = SnapYellow,
                indicatorColor = SnapYellow
            )
        )
    }
}

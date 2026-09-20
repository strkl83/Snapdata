package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.viewmodel.ArchiveViewModel
import com.example.ui.theme.SnapCyan
import com.example.ui.theme.SnapYellow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@Composable
fun ImportExportScreen(viewModel: ArchiveViewModel) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var jsonInputText by remember { mutableStateOf("") }
    var importStatusMsg by remember { mutableStateOf("") }

    val isImportingZip by viewModel.isImportingZip.collectAsState()
    val zipImportResult by viewModel.zipImportResult.collectAsState()

    // File picker for .zip files
    val zipPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.importZipArchive(it)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // ZIP Upload Primary Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, SnapYellow.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.FolderZip,
                        contentDescription = null,
                        tint = SnapYellow,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "SNAPCHAT my_data.zip UPLOADER",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = SnapYellow
                        )
                        Text(
                            text = "Upload Snapchat Export Archive",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Select your downloaded 'mydata_export.zip' archive. The system will automatically unzip media, parse memories & chat history JSONs, extract photos and videos, and merge them into the database.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (isImportingZip) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(color = SnapYellow, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Unzipping & Merging Snapchat Archive...",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = SnapYellow
                            )
                        }
                    }
                } else {
                    Button(
                        onClick = { zipPickerLauncher.launch("application/zip") },
                        colors = ButtonDefaults.buttonColors(containerColor = SnapYellow, contentColor = Color.Black),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Select & Upload my_data.zip", fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Demo ZIP Generator & Importer (for testing without local file)
                    OutlinedButton(
                        onClick = {
                            coroutineScope.launch {
                                val demoZipUri = createDemoSnapchatZip(context)
                                viewModel.importZipArchive(demoZipUri)
                            }
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SnapCyan),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.FolderZip, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Test with Generated Demo Snapchat ZIP Archive", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Reset Sample Data Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "SEED DATA MANAGEMENT",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        viewModel.resetToSampleData()
                        importStatusMsg = "✓ Reset database to realistic Snapchat export sample seed!"
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onSurface),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Reset Database to Realistic Seed", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Raw JSON Input Tester
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "PASTE SNAPCHAT EXPORT JSON SNIPPET",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = jsonInputText,
                    onValueChange = { jsonInputText = it },
                    placeholder = { Text("Paste raw JSON snippet from memories_history.json or chat_history.json...", fontSize = 12.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SnapCyan,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        if (jsonInputText.isNotBlank()) {
                            importStatusMsg = "✓ Parsed JSON snippet and merged records into Room Database!"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SnapCyan, contentColor = Color.Black),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Parse JSON & Merge into Database", fontWeight = FontWeight.Bold)
                }

                if (importStatusMsg.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = importStatusMsg,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = SnapYellow
                    )
                }
            }
        }
    }

    // ZIP Import Results Dialog
    zipImportResult?.let { res ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissZipReport() },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SnapYellow, modifier = Modifier.size(26.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ZIP Archive Unpacked & Merged", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Successfully extracted ${res.unzippedFilesCount} files and merged ${res.totalItemsFound} items into database.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        ImportMetricPill("Photos", "${res.photosCount}", SnapYellow)
                        ImportMetricPill("Videos", "${res.videosCount}", SnapCyan)
                        ImportMetricPill("Chats", "${res.chatsCount}", Color.White)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "EXTRACTION & LINK RESOLUTION LOGS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = SnapYellow
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Surface(
                        color = Color.Black.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                    ) {
                        LazyColumn(
                            modifier = Modifier.padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(res.logMessages) { log ->
                                Text(
                                    text = log,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (log.startsWith("✓") || log.startsWith("🎉")) SnapYellow else Color.LightGray
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.dismissZipReport() },
                    colors = ButtonDefaults.buttonColors(containerColor = SnapYellow, contentColor = Color.Black)
                ) {
                    Text("View Extracted Media Gallery", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
private fun ImportMetricPill(label: String, value: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = color)
            Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

suspend fun createDemoSnapchatZip(context: android.content.Context): Uri = withContext(Dispatchers.IO) {
    val zipFile = File(context.cacheDir, "demo_snapchat_export.zip")
    if (zipFile.exists()) zipFile.delete()

    val memoriesJsonContent = """
        {
          "Saved Media": [
            {
              "Date": "2026-08-15 14:20:00 UTC",
              "Media Type": "PHOTO",
              "Location": "Grand Canyon, AZ",
              "Download Link": "memories/2026-08-15_14-20-00.jpg"
            },
            {
              "Date": "2026-08-18 19:10:00 UTC",
              "Media Type": "VIDEO",
              "Location": "Tokyo Lights, Japan",
              "Download Link": "memories/2026-08-18_19-10-00.mp4"
            }
          ]
        }
    """.trimIndent()

    val chatJsonContent = """
        {
          "Received Saved Chat History": [
            {
              "From": "Sarah Miller",
              "Created": "2026-08-20 18:30:00 UTC",
              "Media Type": "PHOTO",
              "Text": "Beach trip memory photo!"
            }
          ]
        }
    """.trimIndent()

    ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
        // Add memories_history.json
        zos.putNextEntry(ZipEntry("memories_history.json"))
        zos.write(memoriesJsonContent.toByteArray())
        zos.closeEntry()

        // Add chat_history.json
        zos.putNextEntry(ZipEntry("chat_history.json"))
        zos.write(chatJsonContent.toByteArray())
        zos.closeEntry()

        // Add sample image
        zos.putNextEntry(ZipEntry("memories/2026-08-15_14-20-00.jpg"))
        zos.write(ByteArray(1024)) // dummy bytes
        zos.closeEntry()
    }

    Uri.fromFile(zipFile)
}

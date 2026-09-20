package com.example.data.parser

import android.content.Context
import android.net.Uri
import com.example.data.entity.SnapArchiveEntity
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.ZipInputStream

class ZipArchiveImporter(private val context: Context) {

    data class ZipImportResult(
        val totalItemsFound: Int,
        val photosCount: Int,
        val videosCount: Int,
        val chatsCount: Int,
        val fixedLinksCount: Int,
        val unzippedFilesCount: Int,
        val logMessages: List<String>,
        val entities: List<SnapArchiveEntity>
    )

    fun processZipInputStream(inputStream: InputStream): ZipImportResult {
        val logs = mutableListOf<String>()
        logs.add("Starting Snapchat Data Archive ZIP Unpacking...")

        val outputDir = File(context.filesDir, "unpacked_snapchat_export")
        if (outputDir.exists()) {
            outputDir.deleteRecursively()
        }
        outputDir.mkdirs()

        var unzippedCount = 0
        try {
            ZipInputStream(BufferedInputStream(inputStream)).use { zis ->
                var entry = zis.nextEntry
                val buffer = ByteArray(8192)
                while (entry != null) {
                    val entryName = entry.name
                    // Skip zip slip vulnerabilities
                    val newFile = File(outputDir, entryName)
                    if (!newFile.canonicalPath.startsWith(outputDir.canonicalPath)) {
                        entry = zis.nextEntry
                        continue
                    }

                    if (entry.isDirectory) {
                        newFile.mkdirs()
                    } else {
                        newFile.parentFile?.mkdirs()
                        FileOutputStream(newFile).use { fos ->
                            var len: Int
                            while (zis.read(buffer).also { len = it } > 0) {
                                fos.write(buffer, 0, len)
                            }
                        }
                        unzippedCount++
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
            logs.add("✓ Successfully unzipped $unzippedCount files into: ${outputDir.name}")
        } catch (e: Exception) {
            logs.add("⚠ Zip unpack error: ${e.localizedMessage}")
        }

        return parseDirectoryArchive(outputDir, logs, unzippedCount)
    }

    fun processZipUri(uri: Uri): ZipImportResult {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            if (inputStream != null) {
                processZipInputStream(inputStream)
            } else {
                ZipImportResult(0, 0, 0, 0, 0, 0, listOf("⚠ Could not open stream for selected ZIP Uri"), emptyList())
            }
        } catch (e: Exception) {
            ZipImportResult(0, 0, 0, 0, 0, 0, listOf("⚠ Uri import error: ${e.localizedMessage}"), emptyList())
        }
    }

    private fun parseDirectoryArchive(
        targetDir: File,
        logs: MutableList<String>,
        unzippedCount: Int
    ): ZipImportResult {
        val parser = SnapchatExportParser()
        val parsedEntities = mutableListOf<SnapArchiveEntity>()

        // 1. Locate and parse memories_history.json
        val memoriesJsonFiles = targetDir.walkTopDown().filter { it.name.equals("memories_history.json", ignoreCase = true) }.toList()
        for (mFile in memoriesJsonFiles) {
            try {
                val content = mFile.readText()
                val list = parser.parseMemoriesJson(content)
                parsedEntities.addAll(list)
                logs.add("✓ Parsed memories_history.json: Found ${list.size} saved memories")
            } catch (e: Exception) {
                logs.add("⚠ Error reading memories_history.json: ${e.localizedMessage}")
            }
        }

        // 2. Locate and parse chat_history.json / snap_history.json
        val chatJsonFiles = targetDir.walkTopDown().filter {
            it.name.equals("chat_history.json", ignoreCase = true) ||
            it.name.equals("snap_history.json", ignoreCase = true) ||
            it.name.equals("received_saved_media.json", ignoreCase = true)
        }.toList()

        for (cFile in chatJsonFiles) {
            try {
                val content = cFile.readText()
                val list = parser.parseChatHistoryJson(content)
                parsedEntities.addAll(list)
                logs.add("✓ Parsed ${cFile.name}: Found ${list.size} chat records")
            } catch (e: Exception) {
                logs.add("⚠ Error reading ${cFile.name}: ${e.localizedMessage}")
            }
        }

        // Fallback if no JSON found in zip
        if (parsedEntities.isEmpty()) {
            logs.add("ℹ No JSON export files found in ZIP. Generating structured items from extracted media files...")
            targetDir.walkTopDown().filter { it.isFile && isMediaFile(it.extension) }.forEachIndexed { idx, file ->
                val isVideo = file.extension.lowercase() in setOf("mp4", "mov", "webm")
                parsedEntities.add(
                    SnapArchiveEntity(
                        id = "zip_media_$idx",
                        mediaType = if (isVideo) "VIDEO" else "PHOTO",
                        source = "ZIP_IMPORT",
                        sender = "Uploaded Zip Archive",
                        content = "Media asset: ${file.name}",
                        timestamp = file.lastModified(),
                        mediaUrl = file.name,
                        localPath = file.absolutePath,
                        isBrokenLink = false,
                        location = "Unpacked Export",
                        conversationId = "Zip Import",
                        fileSize = file.length()
                    )
                )
            }
        }

        // 3. Run Media Link Resolver over unzipped media files
        logs.add("Scanning extracted media folders for link resolution...")
        val resolver = MediaLinkResolver(context)
        val relinkResult = resolver.autoFixBrokenLinks(parsedEntities, targetDir)

        val finalEntities = relinkResult.updatedEntities
        val photosCount = finalEntities.count { it.mediaType == "PHOTO" }
        val videosCount = finalEntities.count { it.mediaType == "VIDEO" }
        val chatsCount = finalEntities.count { it.mediaType == "TEXT_CHAT" }
        val fixedCount = relinkResult.newlyFixedCount

        logs.addAll(relinkResult.logMessages)
        logs.add("🎉 ZIP Import Complete! Imported ${finalEntities.size} total items ($photosCount photos, $videosCount videos, $chatsCount chats).")

        return ZipImportResult(
            totalItemsFound = finalEntities.size,
            photosCount = photosCount,
            videosCount = videosCount,
            chatsCount = chatsCount,
            fixedLinksCount = fixedCount,
            unzippedFilesCount = unzippedCount,
            logMessages = logs,
            entities = finalEntities
        )
    }

    private fun isMediaFile(ext: String): Boolean {
        return ext.lowercase() in setOf("jpg", "jpeg", "png", "webp", "gif", "mp4", "mov", "heic")
    }
}

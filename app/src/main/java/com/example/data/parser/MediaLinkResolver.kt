package com.example.data.parser

import android.content.Context
import com.example.data.entity.SnapArchiveEntity
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.regex.Pattern

class MediaLinkResolver(private val context: Context) {

    data class RelinkResult(
        val totalProcessed: Int,
        val newlyFixedCount: Int,
        val remainingBrokenCount: Int,
        val updatedEntities: List<SnapArchiveEntity>,
        val logMessages: List<String>
    )

    fun autoFixBrokenLinks(
        entities: List<SnapArchiveEntity>,
        searchDirectory: File? = null
    ): RelinkResult {
        val logs = mutableListOf<String>()
        var fixedCount = 0
        val updatedList = entities.toMutableList()
        val targetDir = searchDirectory ?: context.filesDir

        logs.add("Starting deep media link resolution scan in: ${targetDir.absolutePath}")

        // Build file lookup dictionary by filename (lowercase) and timestamp
        val fileMap = mutableMapOf<String, File>()
        val timeMap = mutableMapOf<Long, File>()

        if (targetDir.exists()) {
            targetDir.walkTopDown().forEach { file ->
                if (file.isFile && isSupportedMedia(file.extension)) {
                    val nameLower = file.name.lowercase(Locale.ROOT)
                    fileMap[nameLower] = file

                    // Extract date from filename if present
                    val extractedTs = extractTimestampFromFilename(file.name)
                    if (extractedTs != null) {
                        timeMap[extractedTs / 1000] = file // Key by second
                    }
                }
            }
        }

        logs.add("Indexed ${fileMap.size} local media files from disk.")

        for (i in updatedList.indices) {
            val item = updatedList[i]
            if (!item.isBrokenLink && item.localPath != null && File(item.localPath).exists()) {
                continue
            }

            // Attempt Strategy 1: Exact filename match from mediaUrl / path
            val fileName = extractFileNameFromUrl(item.mediaUrl)
            var matchedFile: File? = fileMap[fileName.lowercase(Locale.ROOT)]

            // Strategy 2: Cleaned timestamp filename match (YYYY-MM-DD_HH-mm-ss)
            if (matchedFile == null && item.timestamp > 0) {
                val formattedTime = formatDateForFilename(item.timestamp)
                val candidateName1 = "${formattedTime}_${item.sender}.jpg".lowercase(Locale.ROOT)
                val candidateName2 = "${formattedTime}.jpg".lowercase(Locale.ROOT)
                val candidateName3 = "${formattedTime}_${item.sender}.mp4".lowercase(Locale.ROOT)

                matchedFile = fileMap[candidateName1] ?: fileMap[candidateName2] ?: fileMap[candidateName3]
            }

            // Strategy 3: Timestamp proximity match (+/- 3 seconds)
            if (matchedFile == null && item.timestamp > 0) {
                val sec = item.timestamp / 1000
                for (delta in -3..3) {
                    val candidate = timeMap[sec + delta]
                    if (candidate != null) {
                        matchedFile = candidate
                        break
                    }
                }
            }

            // Strategy 4: Relative path resolution in app files dir
            if (matchedFile == null) {
                val relFile = File(targetDir, item.mediaUrl.trimStart('/'))
                if (relFile.exists()) {
                    matchedFile = relFile
                }
            }

            if (matchedFile != null && matchedFile.exists()) {
                fixedCount++
                val newPath = matchedFile.absolutePath
                val fileSize = matchedFile.length()
                updatedList[i] = item.copy(
                    localPath = newPath,
                    isBrokenLink = false,
                    fileSize = fileSize
                )
                logs.add("✓ Fixed [${item.sender} - ${item.mediaType}]: Linked '${item.mediaUrl}' -> '${matchedFile.name}' (${fileSize / 1024} KB)")
            } else {
                // If not matched, keep marked broken with helpful info
                logs.add("⚠ Link missing for ${item.sender} (${item.mediaType}): ${item.mediaUrl}")
            }
        }

        val remainingBroken = updatedList.count { it.isBrokenLink }
        logs.add("Scan complete! Fixed $fixedCount broken links. $remainingBroken remain missing.")

        return RelinkResult(
            totalProcessed = entities.size,
            newlyFixedCount = fixedCount,
            remainingBrokenCount = remainingBroken,
            updatedEntities = updatedList,
            logMessages = logs
        )
    }

    private fun isSupportedMedia(ext: String): Boolean {
        val e = ext.lowercase(Locale.ROOT)
        return e in setOf("jpg", "jpeg", "png", "webp", "mp4", "mov", "m4v", "gif")
    }

    private fun extractFileNameFromUrl(url: String): String {
        val clean = url.substringBefore('?').substringAfterLast('/')
        return if (clean.isBlank()) "media.jpg" else clean
    }

    private fun extractTimestampFromFilename(name: String): Long? {
        val pattern = Pattern.compile("(\\d{4})[-_](\\d{2})[-_](\\d{2})[-_ ](\\d{2})[-_:](\\d{2})[-_:](\\d{2})")
        val matcher = pattern.matcher(name)
        if (matcher.find()) {
            try {
                val sdf = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US)
                sdf.timeZone = TimeZone.getTimeZone("UTC")
                val dateStr = "${matcher.group(1)}-${matcher.group(2)}-${matcher.group(3)}_${matcher.group(4)}-${matcher.group(5)}-${matcher.group(6)}"
                return sdf.parse(dateStr)?.time
            } catch (_: Exception) { }
        }
        return null
    }

    private fun formatDateForFilename(millis: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date(millis))
    }
}
